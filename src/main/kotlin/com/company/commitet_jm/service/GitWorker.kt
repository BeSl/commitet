package com.company.commitet_jm.service

import com.company.commitet_jm.app.OneRunner
import com.company.commitet_jm.app.ShellExecutor
import com.company.commitet_jm.entity.*
import com.company.commitet_jm.entity.TypesFiles.*
import io.jmix.core.DataManager
import io.jmix.core.FileStorage
import io.jmix.core.FileStorageLocator
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.*
import java.util.concurrent.TimeUnit


@Service
class GitWorker(
    private val dataManager: DataManager,
    private val fileStorageLocator: FileStorageLocator,
) {

    companion object {
        private  val log = LoggerFactory.getLogger(GitWorker::class.java)
    }

    fun cloneRepo(repoUrl:String, directoryPath: String, branch: String):Pair<Boolean, String> {
        val executor = ShellExecutor(timeout = 7)
        validateGitUrl(repoUrl)

        //get info variable
        val dir = File(directoryPath)

        if (dir.exists() && dir.list()?.isNotEmpty() == true) {
            throw IllegalArgumentException("Target directory must be empty")
        }

        executor.executeCommand(listOf(
            "git", "clone",
            "--branch", branch,
            "--single-branch",
            repoUrl,
            directoryPath
        ))

        return Pair(true, "")
    }

    fun createCommit() {

        val commitInfo = firstNewDataCommit() ?: return

        val repoPath = commitInfo.project!!.localPath!!
        val repoDir = commitInfo.project!!.localPath?.let { File(it) }
        val remoteBranch = commitInfo.project!!.defaultBranch
        val newBranch = "feature/${commitInfo.taskNum?.let { sanitizeGitBranchName(it) }}"

        try {
            if (repoDir != null) {
                beforeCmdCommit(repoDir, remoteBranch!!,newBranch, commitInfo)
            }
            commitInfo.project?.platform?.let { saveFileCommit(repoPath, commitInfo.files, it) }

            if (repoDir != null) {
                afterCmdCommit(commitInfo, repoDir, newBranch)
            }


        } catch (e: Exception) {
            log.error("Error occurred while creating commit", e)
            commitInfo.errorInfo = e.toString()
            dataManager.save(commitInfo)
            commitInfo.id?.let { setStatusCommit(it, StatusSheduler.ERROR) }
        }

    }

    private fun validateGitUrl(url: String) {
        if (!url.matches(Regex("^(https?|git|ssh)://.*"))) {
            throw IllegalArgumentException("Invalid Git URL format")
        }
    }

    private fun beforeCmdCommit(repoDir: File, remoteBranch: String, newBranch:String, commitInfo: Commit){
        if (!File(repoDir, ".git").exists()) {
            throw IllegalArgumentException("Not a git repository")
        }
        val repoPath = commitInfo.project!!.localPath!!
        val executor = ShellExecutor(workingDir = repoDir, timeout = 7)

        // Переключаемся на develop и сбрасываем изменения
        executor.executeCommand(listOf("git", "checkout", remoteBranch))
        executor.executeCommand(listOf("git", "reset", "--hard", "origin/$remoteBranch"))

        executor.executeCommand(listOf("git", "clean", "-fd"))

        commitInfo.id?.let { setStatusCommit(it, StatusSheduler.PROCESSED) }

        val branches = executor.executeCommand(listOf("git", "branch", "-a"))
        if (!branches.contains("remotes/origin/$remoteBranch")) {
            throw IllegalStateException("Develop branch does not exist")
        }

        executor.executeCommand(listOf("git", "checkout", remoteBranch))
        executor.executeCommand(listOf("git", "fetch", "origin", remoteBranch))
        executor.executeCommand(listOf("git", "checkout", remoteBranch))

        // Создаем новую ветку
        if (branchExists(repoPath, newBranch)) {
            executor.executeCommand(listOf("git", "checkout", newBranch))
        }else {
            executor.executeCommand(listOf("git", "checkout", "-b", newBranch))
        }
    }

    private fun afterCmdCommit(commitInfo: Commit, repoDir: File, newBranch: String) {
        val executor = ShellExecutor(workingDir = repoDir, timeout = 7)
        executor.executeCommand(listOf("git", "add", "."))

        // 4. Создаем коммит от указанного пользователя
        executor.executeCommand(
            listOf(
                "git",
                "-c", "user.name=${commitInfo.author!!.gitLogin}",
                "-c", "user.email=${commitInfo.author!!.email}",
                "commit",
                "-m", commitInfo.description?.let { escapeShellArgument(it) }
            )
        )

        executor.executeCommand(listOf("git", "push", "-u", "origin", newBranch))

        commitInfo.hashCommit = executor.executeCommand(listOf("git", "rev-parse", "HEAD"))

        log.info("Successfully committed and pushed changes to branch $newBranch")
        commitInfo.urlBranch = "${commitInfo.project!!.urlRepo}/tree/$newBranch"
        dataManager.save(commitInfo)
        commitInfo.id?.let { setStatusCommit(it, StatusSheduler.COMPLETE) }
    }

    private fun saveFileCommit(baseDir: String, files: MutableList<FileCommit>, platform: Platform) {
        for (file in files) {
            val content = file.data ?: continue

            // correctPath возвращает File, приводим к Path
            val path = file.getType()?.let { correctPath(baseDir, it).toPath() } ?: continue
            val targetPath = path.resolve(file.name.toString()).normalize()

            try {
                // Создаем директории, если нужно
                Files.createDirectories(targetPath.parent)
            } catch (e: IOException) {
                throw RuntimeException("Не удалось создать директорию: ${targetPath.parent}", e)
            }

            val fileStorage = fileStorageLocator.getDefault<FileStorage>()
            fileStorage.openStream(content).use { inputStream ->
                Files.copy(
                    inputStream,
                    targetPath,
                    StandardCopyOption.REPLACE_EXISTING
                )
            }
            val unpackPath = when (file.getType()){
                REPORT -> "$baseDir\\DataProcessorsExt\\erf"
                DATAPROCESSOR -> "$baseDir\\DataProcessorsExt\\epf"

                else -> {""}
            }
            unpackFile(
                targetPath.toString(), unpackPath,
                platform = platform
            )
        }
    }

    private fun unpackFile(pathFile:String, unpackPath:String, platform: Platform){

        if (unpackPath.isEmpty()){
            return
        }

        val ones = platform.pathInstalled?.toString()
            ?.let { OneRunner(this.dataManager, it, platform.version.toString()) }

        ones?.UploadExtFiles(File(pathFile),unpackPath)
        val bFiles = findBinaryFiles(unpackPath)
        if (bFiles.isEmpty()){return}
        bFiles.forEach { binFile ->
            ones?.UnpackExtFiles(binFile, binFile.parent)

        }

    }

    private fun findBinaryFiles(rootDir: String):List<File>{
        return File(rootDir)
            .walk()  // Рекурсивный обход директорий
            .filter { file ->
                file.isFile && file.name.endsWith("Form.bin", ignoreCase = false)
            }
            .toList()
    }

    private fun correctPath(baseDir: String, type: TypesFiles):File{
        return when (type) {
            REPORT -> File(baseDir, "DataProcessorsExt\\Отчет\\")
            DATAPROCESSOR -> File(baseDir, "DataProcessorsExt\\Обработка\\")
            SCHEDULEDJOBS -> File(baseDir, "CodeExt")
            EXTERNAL_CODE -> File(baseDir, "CodeExt")
            EXCHANGE_RULES -> File(baseDir, "EXCHANGE_RULES")
        }
    }

    private fun setStatusCommit(commitId: UUID, status: StatusSheduler){
        val commit = dataManager.load(Commit::class.java)
                    .id(commitId)
                    .one()
        commit.setStatus(status)
        dataManager.save(commit)
    }

    private fun branchExists(repoPath: String, branchName: String): Boolean {
       val executor = ShellExecutor(workingDir = File(repoPath))
        val branches = executor.executeCommand(listOf("git", "branch", "--list", branchName))
        return branches.isNotBlank()
    }

    private fun firstNewDataCommit(): Commit? {
        val entity = dataManager.load(Commit::class.java)
            .query("select cmt from Commit_ cmt where cmt.status = :status1 order by cmt.id asc")
            .parameter("status1", StatusSheduler.NEW)
            .optional()

        if (entity.isEmpty) {
            return null
        } else {
            val commit = entity.get()
            commit.author = entity.get().author
            commit.files = entity.get().files
            commit.project = entity.get().project

            return commit
        }

    }

    private fun escapeShellArgument(arg: String): String {
        if (arg.isEmpty()) {
            return "''"
        }

        // Если строка содержит пробелы, кавычки или другие спецсимволы, заключаем её в кавычки
        if (!arg.matches(Regex("^[a-zA-Z0-9_\\-+=%/:.,@]+$"))) {
            return "'" + arg.replace("'", "'\"'\"'") + "'"
        }

        return arg
    }

    private fun sanitizeGitBranchName(input: String): String {
        // Правила для имён веток Git:
        // - Не могут начинаться с '-'
        // - Не могут содержать:
        //   - пробелы
        //   - символы: ~, ^, :, *, ?, [, ], @, \, /, {, }, ...
        // - Не могут заканчиваться на .lock
        // - Не могут содержать последовательность //

        val forbiddenChars = setOf(
            ' ', '~', '^', ':', '*', '?', '[', ']', '@', '\\', '/', '{', '}',
            '<', '>', '|', '"', '\'', '!', '#', '$', '%', '&', '(', ')', ',', ';', '='
        )

        return input.map { char ->
            when {
                char in forbiddenChars -> '_'
                else -> char
            }
        }.joinToString("")
            .removePrefix(".") // Убираем точку в начале (если есть)
            .replace(Regex("[/\\\\]+"), "_") // Заменяем несколько / или \ на один _
            .replace(Regex("[._]{2,}"), "_") // Заменяем несколько точек или _ подряд на один _
            .replace(Regex("_+$"), "") // Убираем _ в конце
    }
}