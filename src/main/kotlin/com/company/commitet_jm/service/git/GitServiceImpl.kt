package com.company.commitet_jm.service.git

import com.company.commitet_jm.component.ShellExecutor
import com.company.commitet_jm.entity.*
import com.company.commitet_jm.service.file.FileService
import io.jmix.core.DataManager
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.File
import java.util.*

@Service
class GitServiceImpl(
    private val dataManager: DataManager,
    private val fileService: FileService,
    private val executor: ShellExecutor,
    private val gitHelper: GitCommandHelper
) : GitService {

    @Value("\${git.timeout:7}")
    private var gitTimeout: Long = 7

    @Value("\${git.user:}")
    private var gitUserName: String = ""

    @Value("\${git.token:}")
    private var gitUserToken: String = ""

    companion object {
        private val log = LoggerFactory.getLogger(GitServiceImpl::class.java)
    }

    override fun cloneRepo(repoUrl: String, directoryPath: String, branch: String): Pair<Boolean, String> {
        log.info("start clone repo $repoUrl")
        validateGitUrl(repoUrl)

        val dir = File(directoryPath)

        if (dir.exists() && dir.list()?.isNotEmpty() == true) {
            throw IllegalArgumentException("Target directory must be empty")
        }

        return try {
            val result = gitHelper.execute(
                dir.parentFile ?: File("."),
                gitTimeout,
                "git", "clone",
                "--branch", branch,
                "--single-branch",
                "--depth", "1",
                repoUrl,
                directoryPath
            )

            if (!result.success) {
                throw RuntimeException("Failed to clone repository: ${result.error}")
            }
            log.info("end clone repo $repoUrl")
            Pair(true, "")
        } catch (e: Exception) {
            log.error("Ошибка при клонировании репозитория $repoUrl: ${e.message}")
            Pair(false, e.message ?: "Unknown error")
        }
    }

    override fun createCommit() {
        val commitInfo = firstNewDataCommit() ?: return

        log.info("start createCommit ${commitInfo.taskNum}")

        val repoPath = commitInfo.project!!.localPath!!
        val repoDir = File(repoPath)
        val remoteBranch = commitInfo.project!!.defaultBranch
        val newBranch = "feature/${commitInfo.taskNum?.let { sanitizeGitBranchName(it) }}"
        val uri = commitInfo.project!!.urlRepo!!

        setAuthRepo(uri, gitUserName, gitUserToken, repoPath)

        try {
            beforeCmdCommit(repoDir, remoteBranch!!, newBranch, commitInfo)

            commitInfo.project?.platform?.let {
                fileService.saveFileCommit(repoPath, commitInfo.files, it)
            }
            afterCmdCommit(commitInfo, repoDir, newBranch)
        } catch (e: Exception) {
            log.error("Error occurred while creating commit ${e.message}", e)
            commitInfo.errorInfo = e.message
            commitInfo.setStatus(StatusSheduler.ERROR)
            try {
                dataManager.save(commitInfo)
            } catch (saveException: Exception) {
                log.error("Не удалось сохранить информацию об ошибке коммита: ${saveException.message}", saveException)
            }
        }
    }

    private fun validateGitUrl(url: String) {
        if (!url.matches(Regex("^(https?|git|ssh)://.*"))) {
            throw IllegalArgumentException("Invalid Git URL format")
        }
    }

    private fun beforeCmdCommit(repoDir: File, remoteBranch: String, newBranch: String, commitInfo: Commit) {
        if (!File(repoDir, ".git").exists()) {
            throw IllegalArgumentException("Not a git repository")
        }

        // Устанавливаем статус PROCESS как можно раньше
        commitInfo.id?.let { setStatusCommit(it, StatusSheduler.PROCESSED) }

        try {
            // Сбрасываем репозиторий к состоянию удаленной ветки
            gitHelper.executeWithWarning(repoDir, gitTimeout, "fetch", "git", "fetch", "origin", remoteBranch)
            gitHelper.executeWithWarning(repoDir, gitTimeout, "reset", "git", "reset", "--hard", "origin/$remoteBranch")
            gitHelper.executeWithWarning(repoDir, gitTimeout, "clean", "git", "clean", "-fd")
        } catch (e: Exception) {
            log.error("Не удалось подготовить репозиторий: ${e.message}")
            throw RuntimeException("Не удалось подготовить репозиторий", e)
        }

        // Работаем с веткой
        handleBranchCheckout(repoDir, newBranch)
    }

    private fun handleBranchCheckout(repoDir: File, newBranch: String) {
        try {
            val localExists = branchExists(repoDir, newBranch)
            val remoteExists = remoteBranchExists(repoDir, newBranch)

            when {
                localExists && remoteExists -> {
                    log.info("Переключаемся на локальную ветку $newBranch, которая также существует удаленно")
                    gitHelper.executeWithWarning(repoDir, gitTimeout, "checkout $newBranch", "git", "checkout", newBranch)

                    val hasConflicts = checkBranchDifference(repoDir, newBranch, "origin/$newBranch")
                    if (!hasConflicts) {
                        val pullResult = gitHelper.execute(repoDir, gitTimeout, "git", "pull", "origin", newBranch)
                        if (!pullResult.success) {
                            log.error("Не удалось синхронизировать локальную ветку $newBranch с удаленной: ${pullResult.error}")
                        }
                    } else {
                        log.warn("Обнаружены конфликты при слиянии веток. Пропускаем автоматическую синхронизацию.")
                    }
                }
                localExists -> {
                    log.info("Переключаемся на локальную ветку $newBranch")
                    gitHelper.executeWithWarning(repoDir, gitTimeout, "checkout $newBranch", "git", "checkout", newBranch, "--force")
                }
                remoteExists -> {
                    log.info("Создаем локальную ветку $newBranch из удаленной")
                    gitHelper.executeWithWarning(repoDir, gitTimeout, "создание ветки из удаленной", "git", "checkout", "-b", newBranch, "origin/$newBranch", "--force")
                }
                else -> {
                    log.info("Создаем новую ветку $newBranch")
                    gitHelper.executeWithWarning(repoDir, gitTimeout, "создание новой ветки", "git", "checkout", "-b", newBranch, "--force")
                }
            }
        } catch (e: Exception) {
            log.error("Ошибка при работе с ветками: ${e.message}")
            throw RuntimeException("Ошибка при работе с ветками", e)
        }
    }

    private fun afterCmdCommit(commitInfo: Commit, repoDir: File, newBranch: String) {
        // Проверяем, есть ли изменения в репозитории
        val statusResult = gitHelper.execute(repoDir, gitTimeout, "git", "status", "--porcelain")
        if (statusResult.success && statusResult.output.isBlank()) {
            log.info("Нет изменений для коммита в ветке $newBranch")
            commitInfo.urlBranch = "${commitInfo.project!!.urlRepo}/src/branch/$newBranch"
            commitInfo.setStatus(StatusSheduler.COMPLETE)
            dataManager.save(commitInfo)
            return
        }

        // Добавляем файлы в индекс
        gitHelper.executeOrThrow(repoDir, gitTimeout, "добавление файлов в индекс", "git", "add", ".")

        // Создаем коммит
        val commitMessage = commitInfo.description ?: "Default commit message"
        val tempFile = File.createTempFile("commit-message", ".txt")
        tempFile.writeText(commitMessage, Charsets.UTF_8)

        try {
            gitHelper.executeOrThrow(
                repoDir, gitTimeout, "создание коммита",
                "git",
                "-c", "user.name=${commitInfo.author!!.gitLogin}",
                "-c", "user.email=${commitInfo.author!!.email}",
                "commit",
                "-F", tempFile.absolutePath
            )
        } finally {
            tempFile.delete()
        }

        // Push изменений
        log.info("git push start")
        gitHelper.executeOrThrow(repoDir, gitTimeout, "push в удаленный репозиторий", "git", "push", "--force", "-u", "origin", newBranch)
        log.info("git push end")

        // Получаем хэш коммита
        val hashResult = gitHelper.executeOrThrow(repoDir, gitTimeout, "получение хэша коммита", "git", "rev-parse", "HEAD")
        commitInfo.hashCommit = hashResult.output.trim()

        log.info("Successfully committed and pushed changes to branch $newBranch")

        commitInfo.urlBranch = "${commitInfo.project!!.urlRepo}/tree/$newBranch"
        commitInfo.setStatus(StatusSheduler.COMPLETE)
        dataManager.save(commitInfo)
    }

    private fun setAuthRepo(urlRepo: String, userName: String, token: String, repoPath: String) {
        val pref = when {
            urlRepo.contains("https") -> "https://"
            urlRepo.contains("http") -> "http://"
            else -> ""
        }

        val authUrlRepo = "${pref}$userName:$token@${urlRepo.removePrefix(pref)}"
        executor.workingDir = File(repoPath)
        executor.executeCommand(listOf("git", "remote", "set-url", "origin", authUrlRepo))
    }

    private fun setStatusCommit(commitId: UUID, status: StatusSheduler) {
        try {
            val commit = dataManager.load(Commit::class.java)
                .id(commitId)
                .one()
            commit.setStatus(status)
            dataManager.save(commit)
        } catch (e: Exception) {
            log.error("Не удалось обновить статус коммита $commitId: ${e.message}")
            throw RuntimeException("Не удалось обновить статус коммита $commitId", e)
        }
    }

    private fun branchExists(repoDir: File, branchName: String): Boolean {
        val result = gitHelper.executeOrNull(repoDir, gitTimeout, "проверка локальной ветки $branchName", "git", "branch", "--list", branchName)
        return result?.output?.isNotBlank() == true
    }

    private fun remoteBranchExists(repoDir: File, branchName: String): Boolean {
        val result = gitHelper.executeOrNull(repoDir, gitTimeout, "проверка удаленной ветки $branchName", "git", "branch", "-r")
        return result?.output?.lines()?.any { it.trim().startsWith("origin/$branchName") } == true
    }

    private fun checkBranchDifference(repoDir: File, branch1: String, branch2: String): Boolean {
        try {
            // Сохраняем текущую ветку
            val currentBranchResult = gitHelper.execute(repoDir, gitTimeout, "git", "rev-parse", "--abbrev-ref", "HEAD")
            if (!currentBranchResult.success) {
                log.error("Не удалось получить текущую ветку: ${currentBranchResult.error}")
                return false
            }
            val currentBranch = currentBranchResult.output.trim()

            // Переключаемся на первую ветку
            val checkoutResult = gitHelper.execute(repoDir, gitTimeout, "git", "checkout", branch1)
            if (!checkoutResult.success) {
                log.error("Не удалось переключиться на ветку $branch1: ${checkoutResult.error}")
                return false
            }

            // Пробуем слияние
            val mergeResult = gitHelper.execute(repoDir, gitTimeout, "git", "merge", "--no-commit", "--no-ff", branch2)

            // Проверяем конфликты
            val statusResult = gitHelper.execute(repoDir, gitTimeout, "git", "status", "--porcelain")
            val hasConflicts = statusResult.success && statusResult.output.lines().any { it.startsWith("UU ") }
            val hasMergeConflicts = !mergeResult.success || mergeResult.output.contains("CONFLICT")

            // Возвращаемся на исходную ветку
            gitHelper.executeWithWarning(repoDir, gitTimeout, "возврат на ветку $currentBranch", "git", "checkout", currentBranch)

            if (hasConflicts || hasMergeConflicts) {
                log.warn("Обнаружены конфликты при слиянии веток $branch1 и $branch2")
                return false
            }

            log.info("Слияние веток $branch1 и $branch2 не вызовет конфликтов")
            return true
        } catch (e: Exception) {
            log.warn("Ошибка при проверке различий между ветками $branch1 и $branch2: ${e.message}")
            return false
        }
    }

    private fun firstNewDataCommit(): Commit? {
        return try {
            val entity = dataManager.load(Commit::class.java)
                .query("select cmt from Commit_ cmt where cmt.status = :status1 order by cmt.id asc")
                .parameter("status1", StatusSheduler.NEW)
                .optional()

            if (entity.isEmpty) {
                null
            } else {
                entity.get()
            }
        } catch (e: Exception) {
            log.error("Ошибка при получении данных о новом коммите: ${e.message}")
            throw RuntimeException("Ошибка при получении данных о новом коммите", e)
        }
    }

    private fun sanitizeGitBranchName(input: String): String {
        val forbiddenChars = setOf(
            ' ', '~', '^', ':', '*', '?', '[', ']', '@', '\\', '/', '{', '}',
            '<', '>', '|', '"', '\'', '!', '#', '$', '%', '&', '(', ')', ',', ';', '='
        )

        return input.map { char ->
            if (char in forbiddenChars) '_' else char
        }.joinToString("")
            .removePrefix(".")
            .replace(Regex("[/\\\\]+"), "_")
            .replace(Regex("[._]{2,}"), "_")
            .replace(Regex("_+$"), "")
    }
}
