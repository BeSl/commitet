package com.company.commitet_jm.service;

import com.company.commitet_jm.entity.Commit
import com.company.commitet_jm.entity.FileCommit
import com.company.commitet_jm.entity.StatusSheduler
import io.jmix.core.DataManager
import io.jmix.core.FileRef
import io.jmix.core.FileStorage
import io.jmix.core.FileStorageLocator
import io.jmix.localfs.LocalFileStorageProperties
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.*
import java.util.concurrent.TimeUnit


@Service
class GitWorker(
    private val dataManager: DataManager,

    storage: LocalFileStorageProperties
) {
    @Autowired
    private val fileStorageLocator: FileStorageLocator? = null


    companion object {
        private  val log = LoggerFactory.getLogger(GitWorker::class.java)
    }

    fun CloneRepo(repoUrl:String, directoryPath: String, branch: String):Pair<Boolean, String> {

        validateGitUrl(repoUrl)

        val dir = File(directoryPath)

        if (dir.exists() && dir.list()?.isNotEmpty() == true) {
            throw IllegalArgumentException("Target directory must be empty")
        }

        executeCommand(listOf(
            "git", "clone",
            "--branch", "$branch",
            "--single-branch",
            repoUrl,
            directoryPath
        ))

        return Pair(true, "")
    }

    private fun validateGitUrl(url: String) {
        if (!url.matches(Regex("^(https?|git|ssh)://.*"))) {
            throw IllegalArgumentException("Invalid Git URL format")
        }
    }

    fun CreateCommit() {
        var commitInfo = firstNewDataCommit()
        if (commitInfo == null) {
            return
        }

        val repoPath = commitInfo.project!!.localPath!!
        val repoDir = File(commitInfo.project!!.localPath)
        val remoteBranch = commitInfo.project!!.defaultBranch
        val newBranch = "feature/${commitInfo.taskNum}"

        if (!File(repoDir, ".git").exists()) {
            throw IllegalArgumentException("Not a git repository")
        }

        // Переключаемся на develop и сбрасываем изменения
        executeCommand(listOf("git", "checkout", remoteBranch), repoDir)
        executeCommand(listOf("git", "reset", "--hard", "origin/$remoteBranch"), repoDir)

        // Очищаем неотслеживаемые файлы (опционально)
        executeCommand(listOf("git", "clean", "-fd"), repoDir)
        commitInfo.id?.let { setStatusCommit(it, StatusSheduler.PROCESSED) }
        // Проверяем существование ветки develop
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


        saveFileCommit(repoPath, commitInfo.files)

        executeCommand(listOf("git", "push", "origin", newBranch), repoDir)
        commitInfo.id?.let { setStatusCommit(it, StatusSheduler.COMPLETE) }
        log.info("Created new branch $newBranch from $remoteBranch")




    }

    fun saveFileCommit(baseDir: String, files: MutableList<FileCommit>){
        // Сохраняем файлы
        for (file in files) {
            val content = file.data
            if (content == null)
                continue

            val path = File(baseDir)
            val fileStorage = fileStorageLocator?.getDefault<FileStorage>()
            val targetPath = path.resolve(file.name.toString()).normalize()

            if (fileStorage != null) {
                fileStorage.openStream(content).use { inputStream ->
                    Files.copy(
                        inputStream,
                         targetPath,
                        StandardCopyOption.REPLACE_EXISTING
                    )
                }
            }
        }
    }

    fun setStatusCommit(commitId: UUID, status: StatusSheduler){
        val commit = dataManager.load(Commit::class.java)
                    .id(commitId)
                    .one()
        commit.setStatus(status)
        dataManager.save(commit)
    }

    fun branchExists(repoPath: String, branchName: String): Boolean {
        val branches = executeCommand(listOf("git", "branch", "--list", branchName), File(repoPath))
        return branches.isNotBlank()
    }

private fun firstNewDataCommit(): Commit? {
    val entity = dataManager.load(Commit::class.java)
                    .query("select cmt from Commit_ cmt where cmt.status = :status1 order by cmt.id asc")
                    .parameter("status1", StatusSheduler.NEW)
                    .optional()

    if (entity.isEmpty()) {
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




    // Дополнительные методы
    fun pullChanges(repoPath: String, branch: String) {
        executeCommand(listOf("git", "pull", "origin", branch), File(repoPath))
    }

    fun pushBranch(repoPath: String, branch: String) {
        executeCommand(listOf("git", "push", "-u", "origin", branch), File(repoPath))
    }

}