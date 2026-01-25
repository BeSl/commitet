package com.company.commitet_jm.service.git

import com.company.commitet_jm.component.ShellExecutor
import com.company.commitet_jm.entity.*
import com.company.commitet_jm.service.file.FileService
import io.jmix.core.DataManager
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.File

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

    // ==================== PUBLIC API ====================

    override fun cloneRepo(repoUrl: String, directoryPath: String, branch: String): Pair<Boolean, String> {
        log.info("[CLONE] Начало клонирования: url=$repoUrl, branch=$branch, path=$directoryPath")

        return try {
            validateGitUrl(repoUrl)
            validateTargetDirectory(directoryPath)

            val dir = File(directoryPath)
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
                val errorMessage = parseCloneError(result.error, repoUrl, branch)
                log.error("[CLONE] Ошибка клонирования: $errorMessage")
                return Pair(false, errorMessage)
            }

            log.info("[CLONE] Успешно завершено: $repoUrl -> $directoryPath")
            Pair(true, directoryPath)
        } catch (e: GitOperationException) {
            log.error("[CLONE] ${e.logMessage}")
            Pair(false, e.userFriendlyMessage)
        } catch (e: Exception) {
            log.error("[CLONE] Неожиданная ошибка: ${e.message}", e)
            Pair(false, "Ошибка клонирования: ${e.message}")
        }
    }

    override fun createCommit() {
        val commitInfo = findNextCommitToProcess() ?: run {
            log.debug("[COMMIT] Нет новых задач для обработки")
            return
        }

        val taskNum = commitInfo.taskNum ?: "unknown"
        val projectName = commitInfo.project?.name ?: "unknown"

        log.info("[COMMIT] ===== Начало обработки задачи ===== taskNum=$taskNum, project=$projectName")

        try {
            validateCommitData(commitInfo)

            val repoPath = commitInfo.project!!.localPath!!
            val repoDir = File(repoPath)
            val remoteBranch = commitInfo.project!!.defaultBranch!!
            val newBranch = "feature/${sanitizeGitBranchName(taskNum)}"

            log.debug("[COMMIT] Параметры: repoPath=$repoPath, remoteBranch=$remoteBranch, newBranch=$newBranch")

            // Устанавливаем статус "В обработке"
            updateCommitStatus(commitInfo, StatusSheduler.PROCESSED)

            // Настраиваем аутентификацию
            setupRepoAuthentication(commitInfo.project!!.urlRepo!!, repoPath)

            // Подготавливаем репозиторий
            prepareRepository(repoDir, remoteBranch, taskNum)

            // Переключаемся на рабочую ветку
            checkoutWorkingBranch(repoDir, newBranch, taskNum)

            // Сохраняем файлы коммита
            log.info("[COMMIT][$taskNum] Сохранение файлов коммита")
            commitInfo.project?.platform?.let {
                fileService.saveFileCommit(repoPath, commitInfo.files, it)
            }

            // Выполняем коммит и пуш
            performCommitAndPush(commitInfo, repoDir, newBranch)

            log.info("[COMMIT] ===== Задача успешно завершена ===== taskNum=$taskNum")

        } catch (e: GitOperationException) {
            handleCommitError(commitInfo, e.userFriendlyMessage, e)
        } catch (e: Exception) {
            handleCommitError(commitInfo, "Внутренняя ошибка: ${e.message}", e)
        }
    }

    // ==================== VALIDATION ====================

    private fun validateGitUrl(url: String) {
        if (!url.matches(Regex("^(https?|git|ssh)://.*"))) {
            throw GitOperationException.invalidUrl(url)
        }
    }

    private fun validateTargetDirectory(path: String) {
        val dir = File(path)
        if (dir.exists() && dir.list()?.isNotEmpty() == true) {
            throw GitOperationException(
                GitErrorType.CLONE_FAILED,
                "Целевой каталог не пуст: $path"
            )
        }
    }

    private fun validateCommitData(commitInfo: Commit) {
        val missing = mutableListOf<String>()

        if (commitInfo.project == null) missing.add("проект")
        if (commitInfo.project?.localPath.isNullOrBlank()) missing.add("путь к репозиторию")
        if (commitInfo.project?.defaultBranch.isNullOrBlank()) missing.add("ветка по умолчанию")
        if (commitInfo.project?.urlRepo.isNullOrBlank()) missing.add("URL репозитория")
        if (commitInfo.author == null) missing.add("автор")
        if (commitInfo.author?.gitLogin.isNullOrBlank()) missing.add("git логин автора")
        if (commitInfo.author?.email.isNullOrBlank()) missing.add("email автора")
        if (commitInfo.taskNum.isNullOrBlank()) missing.add("номер задачи")

        if (missing.isNotEmpty()) {
            throw GitOperationException.missingConfig(missing.joinToString(", "))
        }

        val repoDir = File(commitInfo.project!!.localPath!!)
        if (!File(repoDir, ".git").exists()) {
            throw GitOperationException.notARepository(repoDir.absolutePath)
        }
    }

    // ==================== REPOSITORY OPERATIONS ====================

    private fun setupRepoAuthentication(urlRepo: String, repoPath: String) {
        log.debug("[AUTH] Настройка аутентификации для репозитория")

        val pref = when {
            urlRepo.contains("https") -> "https://"
            urlRepo.contains("http") -> "http://"
            else -> ""
        }

        val authUrlRepo = "${pref}$gitUserName:$gitUserToken@${urlRepo.removePrefix(pref)}"
        executor.workingDir = File(repoPath)
        executor.executeCommand(listOf("git", "remote", "set-url", "origin", authUrlRepo))
    }

    private fun prepareRepository(repoDir: File, remoteBranch: String, taskNum: String) {
        log.info("[COMMIT][$taskNum] Подготовка репозитория: fetch, reset, clean")

        val fetchResult = gitHelper.execute(repoDir, gitTimeout, "git", "fetch", "origin", remoteBranch)
        if (!fetchResult.success) {
            log.warn("[COMMIT][$taskNum] Fetch завершился с ошибкой: ${fetchResult.error}")
            // Продолжаем, т.к. fetch может не сработать при отсутствии связи, но локальные данные есть
        }

        val resetResult = gitHelper.execute(repoDir, gitTimeout, "git", "reset", "--hard", "origin/$remoteBranch")
        if (!resetResult.success) {
            throw GitOperationException(
                GitErrorType.FETCH_FAILED,
                "Не удалось сбросить репозиторий к origin/$remoteBranch: ${resetResult.error}"
            )
        }

        val cleanResult = gitHelper.execute(repoDir, gitTimeout, "git", "clean", "-fd")
        if (!cleanResult.success) {
            log.warn("[COMMIT][$taskNum] Clean завершился с ошибкой: ${cleanResult.error}")
        }

        log.debug("[COMMIT][$taskNum] Репозиторий подготовлен")
    }

    private fun checkoutWorkingBranch(repoDir: File, newBranch: String, taskNum: String) {
        log.info("[COMMIT][$taskNum] Переключение на ветку: $newBranch")

        val localExists = branchExists(repoDir, newBranch)
        val remoteExists = remoteBranchExists(repoDir, newBranch)

        log.debug("[COMMIT][$taskNum] Состояние веток: localExists=$localExists, remoteExists=$remoteExists")

        val checkoutResult = when {
            localExists && remoteExists -> {
                log.debug("[COMMIT][$taskNum] Ветка существует локально и удалённо, синхронизируем")
                val result = gitHelper.execute(repoDir, gitTimeout, "git", "checkout", newBranch)
                if (result.success) {
                    // Пытаемся подтянуть изменения
                    gitHelper.execute(repoDir, gitTimeout, "git", "pull", "origin", newBranch, "--rebase")
                }
                result
            }
            localExists -> {
                log.debug("[COMMIT][$taskNum] Ветка существует только локально")
                gitHelper.execute(repoDir, gitTimeout, "git", "checkout", newBranch, "--force")
            }
            remoteExists -> {
                log.debug("[COMMIT][$taskNum] Ветка существует только удалённо, создаём локальную")
                gitHelper.execute(repoDir, gitTimeout, "git", "checkout", "-b", newBranch, "origin/$newBranch", "--force")
            }
            else -> {
                log.debug("[COMMIT][$taskNum] Создаём новую ветку")
                gitHelper.execute(repoDir, gitTimeout, "git", "checkout", "-b", newBranch, "--force")
            }
        }

        if (!checkoutResult.success) {
            throw GitOperationException.checkoutFailed(newBranch, checkoutResult.error)
        }

        log.debug("[COMMIT][$taskNum] Успешно переключились на ветку $newBranch")
    }

    private fun performCommitAndPush(commitInfo: Commit, repoDir: File, newBranch: String) {
        val taskNum = commitInfo.taskNum ?: "unknown"

        // Проверяем наличие изменений
        val statusResult = gitHelper.execute(repoDir, gitTimeout, "git", "status", "--porcelain")
        if (statusResult.success && statusResult.output.isBlank()) {
            log.info("[COMMIT][$taskNum] Нет изменений для коммита")
            completeCommit(commitInfo, newBranch, null, null)
            return
        }

        // Добавляем файлы в индекс
        log.debug("[COMMIT][$taskNum] Добавление файлов в индекс")
        val addResult = gitHelper.execute(repoDir, gitTimeout, "git", "add", ".")
        if (!addResult.success) {
            throw GitOperationException.commitFailed("Не удалось добавить файлы: ${addResult.error}")
        }

        // Собираем информацию о diff ДО коммита (пока изменения в staged)
        log.debug("[COMMIT][$taskNum] Сбор информации об изменениях")
        val diffInfo = collectDiffInfo(repoDir, taskNum)

        // Создаём коммит
        log.info("[COMMIT][$taskNum] Создание коммита")
        val commitMessage = commitInfo.description ?: "Изменения по задаче $taskNum"
        val tempFile = File.createTempFile("commit-message", ".txt")
        try {
            tempFile.writeText(commitMessage, Charsets.UTF_8)

            val commitResult = gitHelper.execute(
                repoDir, gitTimeout,
                "git",
                "-c", "user.name=${commitInfo.author!!.gitLogin}",
                "-c", "user.email=${commitInfo.author!!.email}",
                "commit",
                "-F", tempFile.absolutePath
            )

            if (!commitResult.success) {
                throw GitOperationException.commitFailed(commitResult.error)
            }
        } finally {
            tempFile.delete()
        }

        // Push изменений
        log.info("[COMMIT][$taskNum] Push в удалённый репозиторий")
        val pushResult = gitHelper.execute(repoDir, gitTimeout, "git", "push", "--force", "-u", "origin", newBranch)
        if (!pushResult.success) {
            throw GitOperationException.pushFailed(newBranch, pushResult.error)
        }

        // Получаем хэш коммита
        val hashResult = gitHelper.execute(repoDir, gitTimeout, "git", "rev-parse", "HEAD")
        val commitHash = if (hashResult.success) hashResult.output.trim() else null

        log.info("[COMMIT][$taskNum] Push завершён успешно, hash=$commitHash, files=${diffInfo.totalFiles}")

        completeCommit(commitInfo, newBranch, commitHash, diffInfo)
    }

    /**
     * Собирает информацию об изменениях (diff) для staged файлов
     */
    private fun collectDiffInfo(repoDir: File, taskNum: String): CommitDiffInfo {
        try {
            // Получаем список изменённых файлов со статистикой
            val statResult = gitHelper.execute(repoDir, gitTimeout, "git", "diff", "--cached", "--numstat")
            // Получаем полный diff
            val diffResult = gitHelper.execute(repoDir, gitTimeout, "git", "diff", "--cached")
            // Получаем имена файлов с типом изменения
            val nameStatusResult = gitHelper.execute(repoDir, gitTimeout, "git", "diff", "--cached", "--name-status")

            if (!statResult.success || !nameStatusResult.success) {
                log.warn("[COMMIT][$taskNum] Не удалось получить diff: ${statResult.error}")
                return CommitDiffInfo.empty()
            }

            val entries = mutableListOf<DiffEntry>()
            var totalAdditions = 0
            var totalDeletions = 0

            // Парсим --name-status для получения типа изменения
            val changeTypes = mutableMapOf<String, DiffChangeType>()
            val renames = mutableMapOf<String, String>() // newPath -> oldPath

            nameStatusResult.output.lines().filter { it.isNotBlank() }.forEach { line ->
                val parts = line.split("\t")
                if (parts.size >= 2) {
                    val status = parts[0]
                    val path = parts.last()

                    val changeType = when {
                        status.startsWith("A") -> DiffChangeType.ADDED
                        status.startsWith("M") -> DiffChangeType.MODIFIED
                        status.startsWith("D") -> DiffChangeType.DELETED
                        status.startsWith("R") -> {
                            if (parts.size >= 3) renames[parts[2]] = parts[1]
                            DiffChangeType.RENAMED
                        }
                        status.startsWith("C") -> DiffChangeType.COPIED
                        else -> DiffChangeType.MODIFIED
                    }
                    changeTypes[path] = changeType
                }
            }

            // Парсим --numstat для статистики
            statResult.output.lines().filter { it.isNotBlank() }.forEach { line ->
                val parts = line.split("\t")
                if (parts.size >= 3) {
                    val additions = parts[0].toIntOrNull() ?: 0
                    val deletions = parts[1].toIntOrNull() ?: 0
                    val path = parts[2]

                    totalAdditions += additions
                    totalDeletions += deletions

                    entries.add(
                        DiffEntry(
                            path = path,
                            changeType = changeTypes[path] ?: DiffChangeType.MODIFIED,
                            oldPath = renames[path],
                            additions = additions,
                            deletions = deletions,
                            diffContent = extractFileDiff(diffResult.output, path)
                        )
                    )
                }
            }

            log.debug("[COMMIT][$taskNum] Собран diff: ${entries.size} файлов, +$totalAdditions/-$totalDeletions")

            return CommitDiffInfo(
                entries = entries,
                totalAdditions = totalAdditions,
                totalDeletions = totalDeletions,
                totalFiles = entries.size,
                rawDiff = diffResult.output.take(100000) // Ограничиваем размер
            )
        } catch (e: Exception) {
            log.error("[COMMIT][$taskNum] Ошибка при сборе diff: ${e.message}")
            return CommitDiffInfo.empty()
        }
    }

    /**
     * Извлекает diff для конкретного файла из общего diff
     */
    private fun extractFileDiff(fullDiff: String, filePath: String): String? {
        val escapedPath = Regex.escape(filePath)
        val pattern = Regex("diff --git a/.*?$escapedPath.*?(?=diff --git|$)", RegexOption.DOT_MATCHES_ALL)
        return pattern.find(fullDiff)?.value?.take(50000) // Ограничиваем размер одного файла
    }

    // ==================== COMMIT STATUS MANAGEMENT ====================

    private fun findNextCommitToProcess(): Commit? {
        return try {
            dataManager.load(Commit::class.java)
                .query("select c from Commit_ c where c.status = :status order by c.id asc")
                .parameter("status", StatusSheduler.NEW)
                .optional()
                .orElse(null)
        } catch (e: Exception) {
            log.error("[COMMIT] Ошибка при поиске задач для обработки: ${e.message}")
            null
        }
    }

    private fun updateCommitStatus(commitInfo: Commit, status: StatusSheduler) {
        try {
            commitInfo.id?.let { id ->
                val commit = dataManager.load(Commit::class.java).id(id).one()
                commit.setStatus(status)
                dataManager.save(commit)
                log.debug("[COMMIT][${commitInfo.taskNum}] Статус обновлён: $status")
            }
        } catch (e: Exception) {
            log.error("[COMMIT][${commitInfo.taskNum}] Ошибка обновления статуса: ${e.message}")
        }
    }

    private fun completeCommit(commitInfo: Commit, branch: String, commitHash: String?, diffInfo: CommitDiffInfo?) {
        commitInfo.hashCommit = commitHash
        commitInfo.urlBranch = "${commitInfo.project!!.urlRepo}/tree/$branch"
        commitInfo.setStatus(StatusSheduler.COMPLETE)
        commitInfo.errorInfo = null
        commitInfo.diffData = diffInfo?.toJson()
        dataManager.save(commitInfo)
    }

    private fun handleCommitError(commitInfo: Commit, errorMessage: String, exception: Exception) {
        val taskNum = commitInfo.taskNum ?: "unknown"
        log.error("[COMMIT][$taskNum] ОШИБКА: $errorMessage", exception)

        commitInfo.errorInfo = errorMessage
        commitInfo.setStatus(StatusSheduler.ERROR)

        try {
            dataManager.save(commitInfo)
        } catch (saveException: Exception) {
            log.error("[COMMIT][$taskNum] Не удалось сохранить информацию об ошибке: ${saveException.message}")
        }
    }

    // ==================== HELPER METHODS ====================

    private fun branchExists(repoDir: File, branchName: String): Boolean {
        val result = gitHelper.execute(repoDir, gitTimeout, "git", "branch", "--list", branchName)
        return result.success && result.output.isNotBlank()
    }

    private fun remoteBranchExists(repoDir: File, branchName: String): Boolean {
        val result = gitHelper.execute(repoDir, gitTimeout, "git", "branch", "-r")
        return result.success && result.output.lines().any { it.trim().startsWith("origin/$branchName") }
    }

    private fun parseCloneError(error: String, url: String, branch: String): String {
        return when {
            error.contains("Authentication failed") || error.contains("could not read Username") ->
                "Ошибка аутентификации. Проверьте учётные данные для доступа к репозиторию"
            error.contains("not found") || error.contains("does not exist") ->
                "Репозиторий не найден: $url"
            error.contains("Remote branch") && error.contains("not found") ->
                "Ветка '$branch' не найдена в репозитории"
            error.contains("Connection refused") || error.contains("Could not resolve host") ->
                "Не удалось подключиться к серверу. Проверьте сетевое соединение"
            error.contains("Permission denied") ->
                "Доступ запрещён. Проверьте права доступа к репозиторию"
            else -> "Ошибка клонирования: $error"
        }
    }

    private fun sanitizeGitBranchName(input: String): String {
        val forbiddenChars = setOf(
            ' ', '~', '^', ':', '*', '?', '[', ']', '@', '\\', '/', '{', '}',
            '<', '>', '|', '"', '\'', '!', '#', '$', '%', '&', '(', ')', ',', ';', '='
        )

        return input
            .map { if (it in forbiddenChars) '_' else it }
            .joinToString("")
            .removePrefix(".")
            .replace(Regex("[/\\\\]+"), "_")
            .replace(Regex("[._]{2,}"), "_")
            .replace(Regex("_+$"), "")
    }
}
