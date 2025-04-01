package com.company.commitet_jm.service

import com.company.commitet_jm.entity.Commit
import com.company.commitet_jm.entity.FileCommit
import com.company.commitet_jm.entity.StatusSheduler
import com.company.commitet_jm.entity.TypesFiles
import io.jmix.core.DataManager
import io.jmix.core.FileStorage
import io.jmix.core.FileStorageLocator
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.*
import java.util.concurrent.TimeUnit


@Service
class GitWorker(
    private val dataManager: DataManager
    ,
    private val fileStorageLocator: FileStorageLocator,
) {

    companion object {
        private  val log = LoggerFactory.getLogger(GitWorker::class.java)
    }

    fun cloneRepo(repoUrl:String, directoryPath: String, branch: String):Pair<Boolean, String> {

        validateGitUrl(repoUrl)

        val dir = File(directoryPath)

        if (dir.exists() && dir.list()?.isNotEmpty() == true) {
            throw IllegalArgumentException("Target directory must be empty")
        }

        executeCommand(listOf(
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
        val newBranch = "feature/${commitInfo.taskNum}"

        try {
            if (repoDir != null) {
                beforeCmdCommit(repoDir, remoteBranch!!,newBranch, commitInfo)
            }
            saveFileCommit(repoPath, commitInfo.files)

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


        // Переключаемся на develop и сбрасываем изменения
        executeCommand(listOf("git", "checkout", remoteBranch), repoDir)
        executeCommand(listOf("git", "reset", "--hard", "origin/$remoteBranch"), repoDir)

        executeCommand(listOf("git", "clean", "-fd"), repoDir)

        commitInfo.id?.let { setStatusCommit(it, StatusSheduler.PROCESSED) }

        val branches = executeCommand(listOf("git", "branch", "-a"), repoDir)
        if (!branches.contains("remotes/origin/$remoteBranch")) {
            throw IllegalStateException("Develop branch does not exist")
        }

        executeCommand(listOf("git", "checkout", remoteBranch), repoDir)
        executeCommand(listOf("git", "fetch", "origin", remoteBranch), repoDir)
        executeCommand(listOf("git", "checkout", remoteBranch), repoDir)

        // Создаем новую ветку
        if (branchExists(repoPath, newBranch)) {
            executeCommand(listOf("git", "checkout", newBranch), repoDir)
            executeCommand(listOf("git", "fetch", "origin", newBranch), repoDir)
        }else {
            executeCommand(listOf("git", "checkout", "-b", newBranch), repoDir)
        }
    }

    private fun afterCmdCommit(commitInfo: Commit, repoDir: File, newBranch: String) {

        executeCommand(listOf("git", "add", "."), repoDir)

        // 4. Создаем коммит от указанного пользователя
        executeCommand(
            listOf(
                "git",
                "-c", "user.name=${commitInfo.author!!.gitLogin}",
                "-c", "user.email=${commitInfo.author!!.email}",
                "commit",
                "-m", commitInfo.description
            ),
            repoDir
        )

        executeCommand(listOf("git", "push", "origin", newBranch), repoDir)

        log.info("Successfully committed and pushed changes to branch $newBranch")

        commitInfo.id?.let { setStatusCommit(it, StatusSheduler.COMPLETE) }
    }

    private fun saveFileCommit(baseDir: String, files: MutableList<FileCommit>){
        // Сохраняем файлы
        for (file in files) {
            val content = file.data ?: continue

            val path = file.getType()?.let { correctPath(baseDir, it) }
            val fileStorage = fileStorageLocator.getDefault<FileStorage>()
            val targetPath = path!!.resolve(file.name.toString()).normalize()

            fileStorage.openStream(content).use { inputStream ->
                Files.copy(
                    inputStream,
                    targetPath.toPath(),
                    StandardCopyOption.REPLACE_EXISTING
                )
            }
        }
    }

    private fun correctPath(baseDir: String, type: TypesFiles):File{
        return when (type) {
            TypesFiles.REPORT -> File(baseDir, "DataProcessorsExt\\erf\\")
            TypesFiles.DATAPROCESSOR -> File(baseDir, "DataProcessorsExt\\epf\\")
            TypesFiles.SCHEDULEDJOBS -> File(baseDir, "ExtCode")
            TypesFiles.EXTERNAL_CODE -> File(baseDir, "ExtCode")
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
        val branches = executeCommand(listOf("git", "branch", "--list", branchName), File(repoPath))
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

    private fun executeCommand(command: List<String?>, workingDir: File = File(".")): String {
        try {
            val process = ProcessBuilder(command)
                .directory(workingDir)
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .redirectError(ProcessBuilder.Redirect.PIPE)
                .start()

            val output = process.inputStream.bufferedReader().readText()
            val error = process.errorStream.bufferedReader().readText()

            process.waitFor(1, TimeUnit.MINUTES)

            if (process.exitValue() != 0) {
                log.error("Command failed: ${command.joinToString(" ")}\nError: $error")
                throw RuntimeException("Git command failed: $error")
            }

            log.debug("Command executed: ${command.joinToString(" ")}\nOutput: $output")
            return output
        } catch (e: IOException) {
            log.error("IO error executing command: ${e.message}")
            throw e
        } catch (e: InterruptedException) {
            log.error("Command interrupted: ${e.message}")
            throw RuntimeException("Operation interrupted")
        }
    }

}