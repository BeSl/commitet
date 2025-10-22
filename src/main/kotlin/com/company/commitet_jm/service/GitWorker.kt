package com.company.commitet_jm.service

import com.company.commitet_jm.component.ShellExecutor
import com.company.commitet_jm.entity.*
import com.company.commitet_jm.entity.TypesFiles.*
import com.company.commitet_jm.service.ones.OneRunner
import io.jmix.core.DataManager
import io.jmix.core.FileStorage
import io.jmix.core.FileStorageLocator
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.*

@Service
class GitWorker(
    private val dataManager: DataManager,
    private val fileStorageLocator: FileStorageLocator,
) {

    companion object {
        private  val log = LoggerFactory.getLogger(GitWorker::class.java)
    }

    @Autowired
    private lateinit var ones: OneRunner

    fun cloneRepo(repoUrl:String, directoryPath: String, branch: String):Pair<Boolean, String> {
        val executor = ShellExecutor(timeout = 7)
        log.info("start clone repo $repoUrl")
        validateGitUrl(repoUrl)


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
        log.info("end clone repo $repoUrl")
        return Pair(true, "")
    }

    fun createCommit() {

        val commitInfo = firstNewDataCommit() ?: return

        log.info("start createCommit ${commitInfo.taskNum}")

        val repoPath = commitInfo.project!!.localPath!!
        val repoDir = commitInfo.project!!.localPath?.let { File(it) }
        val remoteBranch = commitInfo.project!!.defaultBranch
        val newBranch = "feature/${commitInfo.taskNum?.let { sanitizeGitBranchName(it) }}"

        try {

            if (repoDir != null) {
                beforeCmdCommit(repoDir, remoteBranch!!,newBranch, commitInfo)
            }else{
                throw RuntimeException("$repoDir not exist!!!")
            }

            commitInfo.project?.platform?.let { saveFileCommit(repoPath, commitInfo.files, it) }
            afterCmdCommit(commitInfo, repoDir, newBranch)
        } catch (e: Exception) {
            log.error("Error occurred while creating commit ${e.message}")
            commitInfo.errorInfo = e.message
            commitInfo.setStatus(StatusSheduler.ERROR)
            dataManager.save(commitInfo)
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
        try {
            executor.executeCommand(listOf("git", "checkout", remoteBranch))
        }catch (e: Exception){

            if (e.message?.contains("index.lock") == true) {
                log.info("Обнаружена блокировка Git. Удаление index.lock...")

                val lockFile = File("$repoDir/.git/index.lock")
                if (lockFile.exists()) {
                    lockFile.delete()
                    log.info("Файл index.lock удалён. Повторная попытка...")

                    executor.executeCommand(listOf("git", "checkout", remoteBranch))
                } else {
                    throw IllegalStateException("Файл блокировки не найден, но ошибка возникла: ${e.message}")
                }
            } else if (e.message?.contains("would be overwritten by checkout") == true) {

                executor.executeCommand(listOf("git", "reset", "--hard"))
                executor.executeCommand(listOf("git", "clean", "-fd"))

            } else {
                throw e
            }
        }

        executor.executeCommand(listOf("git", "reset", "--hard", "origin/$remoteBranch"))
        executor.executeCommand(listOf("git", "clean", "-fd"))

        commitInfo.id?.let { setStatusCommit(it, StatusSheduler.PROCESSED) }

        val remoteBranches = executor.executeCommand(listOf("git", "branch", "-a"))

        if (!remoteBranches.contains("remotes/origin/$remoteBranch")) {
            throw IllegalStateException("Default branch does not exist")
        }

        try {
            executor.executeCommand(listOf("git", "checkout", remoteBranch))
            executor.executeCommand(listOf("git", "fetch", "origin", remoteBranch))
            executor.executeCommand(listOf("git", "checkout", remoteBranch))

            // Создаем новую ветку
            if (branchExists(repoPath, newBranch)) {
                log.info("create new branch $newBranch")
                executor.executeCommand(listOf("git", "checkout", newBranch))
            } else {
                log.info("checkout branch $newBranch")
                executor.executeCommand(listOf("git", "checkout", "-b", newBranch))
            }
        } catch (e:Exception){
            log.error("beforeCmdCommit ${e.message}")
            throw RuntimeException("Error cmd git ${e.message}")
        }
    }

    private fun afterCmdCommit(commitInfo: Commit, repoDir: File, newBranch: String) {
        val executor = ShellExecutor(workingDir = repoDir, timeout = 7)
        executor.executeCommand(listOf("git", "add", "."))

        // 4. Создаем коммит от указанного пользователя
        // Создаем временный файл с сообщением коммита
        val commitMessage = commitInfo.description ?: "Default commit message"
        val tempFile = File.createTempFile("commit-message", ".txt")
        tempFile.writeText(commitMessage, Charsets.UTF_8)
        
        try {
            executor.executeCommand(
                listOf(
                    "git",
                    "-c", "user.name=${commitInfo.author!!.gitLogin}",
                    "-c", "user.email=${commitInfo.author!!.email}",
                    "commit",
                    "-F", tempFile.absolutePath
                )
            )
        } finally {
            // Удаляем временный файл
            tempFile.delete()
        }

        log.info("git push start")
        executor.executeCommand(listOf("git", "push", "--force", "-u", "origin", newBranch))
        log.info("git push end")
        commitInfo.hashCommit = executor.executeCommand(listOf("git", "rev-parse", "HEAD"))

        log.info("Successfully committed and pushed changes to branch $newBranch")

        commitInfo.urlBranch = "${commitInfo.project!!.urlRepo}/tree/$newBranch"

        commitInfo.setStatus(StatusSheduler.COMPLETE)
        dataManager.save(commitInfo)

    }

    private fun saveFileCommit(baseDir: String, files: MutableList<FileCommit>, platform: Platform) {
        val executor = ShellExecutor(workingDir = File(baseDir), timeout = 7)
        val filesToUnpack = mutableListOf<Pair<String, String>>()
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
            file.getType()?.let { fileType ->
                val unpackPath = when (fileType) {
                    REPORT -> "$baseDir\\DataProcessorsExt\\erf"
                    DATAPROCESSOR -> "$baseDir\\DataProcessorsExt\\epf"
                    else -> null
                }
                unpackPath?.let {
                    filesToUnpack.add(targetPath.toString() to unpackPath)
                }
            }

        }
        if (filesToUnpack.isNotEmpty()) {
            unpackFiles(filesToUnpack, platform, executor, baseDir)
        }
    }

    private fun unpackFiles(files: List<Pair<String, String>>, platform: Platform, executor : ShellExecutor, baseDir: String){

        if (files.isEmpty()){
            return
        }

        for ((sourcePath, unpackPath) in files) {
            ones.uploadExtFiles(File(sourcePath), unpackPath, platform.pathInstalled.toString(), platform.version.toString())
        }

        val bFiles = findBinaryFilesFromGitStatus(baseDir, executor)
        if (bFiles.isEmpty()) {
            return
        }
        bFiles.forEach { binFile ->
            ones.unpackExtFiles(binFile, binFile.parent)
        }
    }

    private fun findBinaryFilesFromGitStatus(repoDir: String, executor: ShellExecutor): List<File> {
        // Получаем список изменённых файлов
        val gitOutput = executor.executeCommand(listOf("git", "-C", repoDir, "status", "--porcelain")).trim()
        if (gitOutput.isBlank()) return emptyList()

        // Выделяем директории из вывода git status
        val changedDirs = gitOutput
            .lines()
            .mapNotNull { line ->
                val filePath = line.substringAfter(" ").takeIf { it.isNotBlank() }
                filePath?.let { File(repoDir, it) }
            }
            .filterNotNull()
            .distinct()

        // Ищем .bin файлы в изменённых директориях
        val tDir = changedDirs.flatMap { dir ->
            dir.walk()
                .filter { file ->
                    file.isFile && file.name.endsWith("Form.bin", ignoreCase = false)
                }
                .toList()
        }
        return tDir
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