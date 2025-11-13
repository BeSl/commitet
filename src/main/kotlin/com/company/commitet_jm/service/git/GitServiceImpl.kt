package com.company.commitet_jm.service.git

import com.company.commitet_jm.component.ShellExecutor
import com.company.commitet_jm.entity.*
import com.company.commitet_jm.entity.TypesFiles.*
import com.company.commitet_jm.service.file.FileService
import com.company.commitet_jm.service.ones.OneCService
import io.jmix.core.DataManager
import io.jmix.core.FileStorageLocator
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.File
import java.util.*

@Service
class GitServiceImpl(
    private val dataManager: DataManager,
    private val fileStorageLocator: FileStorageLocator,
    private val fileService: FileService,
    private val oneCService: OneCService
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

    @Autowired
    lateinit var executor: ShellExecutor

    override fun cloneRepo(repoUrl: String, directoryPath: String, branch: String): Pair<Boolean, String> {
        executor.timeout = gitTimeout
        log.info("start clone repo $repoUrl")
        validateGitUrl(repoUrl)
        
        val dir = File(directoryPath)
        
        if (dir.exists() && dir.list()?.isNotEmpty() == true) {
            throw IllegalArgumentException("Target directory must be empty")
        }
        
        return try {
            val result = executor.executeCommandWithResult(listOf(
                "git", "clone",
                "--branch", branch,
                "--single-branch",
                "--depth", "1",
                repoUrl,
                directoryPath
            ))
            
            if (result.exitCode != 0) {
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
        val repoDir = commitInfo.project!!.localPath?.let { File(it) }
        val remoteBranch = commitInfo.project!!.defaultBranch
        val newBranch = "feature/${commitInfo.taskNum?.let { sanitizeGitBranchName(it) }}"
        val uri = commitInfo.project!!.urlRepo!!

        setAuthRepo(uri, gitUserName, gitUserToken, repoPath)

        try {
            if (repoDir != null) {
                beforeCmdCommit(repoDir, remoteBranch!!, newBranch, commitInfo)
            } else {
                throw RuntimeException("$repoDir not exist!!!")
            }
            
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
        try {
            if (!url.matches(Regex("^(https?|git|ssh)://.*"))) {
                throw IllegalArgumentException("Invalid Git URL format")
            }
        } catch (e: Exception) {
            log.error("Ошибка при валидации Git URL: ${e.message}")
            throw e
        }
    }

    private fun beforeCmdCommit(repoDir: File, remoteBranch: String, newBranch: String, commitInfo: Commit) {
        if (!File(repoDir, ".git").exists()) {
            throw IllegalArgumentException("Not a git repository")
        }
        val repoPath = commitInfo.project!!.localPath!!
        executor.timeout = gitTimeout
        executor.workingDir = repoDir
        
        // Устанавливаем статус PROCESS как можно раньше
        commitInfo.id?.let { setStatusCommit(it, StatusSheduler.PROCESSED) }

        try {
            // Сбрасываем репозиторий к состоянию удаленной ветки
            val fetchResult = executor.executeCommandWithResult(listOf("git", "fetch", "origin", remoteBranch))
            if (fetchResult.exitCode != 0) {
                log.warn("Не удалось выполнить fetch: ${fetchResult.error}")
            }
            
            val resetResult = executor.executeCommandWithResult(listOf("git", "reset", "--hard", "origin/$remoteBranch"))
            if (resetResult.exitCode != 0) {
                log.warn("Не удалось выполнить reset: ${resetResult.error}")
            }
            
            val cleanResult = executor.executeCommandWithResult(listOf("git", "clean", "-fd"))
            if (cleanResult.exitCode != 0) {
                log.warn("Не удалось выполнить clean: ${cleanResult.error}")
            }
        } catch (e: Exception) {
            log.error("Не удалось подготовить репозиторий: ${e.message}")
            throw RuntimeException("Не удалось подготовить репозиторий", e)
        }

        // Работаем с веткой
        try {
            // Проверяем существование ветки локально и удаленно
            val localExists = branchExists(repoPath, newBranch)
            val remoteExists = remoteBranchExists(repoPath, newBranch)
            
            // Если ветка существует локально или удаленно, просто переходим на нее
            when {
                localExists && remoteExists -> {
                    log.info("Переключаемся на локальную ветку $newBranch, которая также существует удаленно")
                    val checkoutResult = executor.executeCommandWithResult(listOf("git", "checkout", newBranch))
                    if (checkoutResult.exitCode != 0) {
                        log.warn("Не удалось переключиться на ветку $newBranch: ${checkoutResult.error}")
                    }
                    
                    // Проверяем наличие конфликтов перед синхронизацией
                    val hasConflicts = checkBranchDifference(repoPath, newBranch, "origin/$newBranch")
                    if (!hasConflicts) {
                        // Синхронизируем с удаленной веткой
                        val pullResult = executor.executeCommandWithResult(listOf("git", "pull", "origin", newBranch))
                        if (pullResult.exitCode != 0) {
                            log.error("Не удалось синхронизировать локальную ветку $newBranch с удаленной: ${pullResult.error}")
                            // Продолжаем выполнение даже при ошибке синхронизации
                        }
                    } else {
                        log.warn("Обнаружены конфликты при слиянии веток. Пропускаем автоматическую синхронизацию.")
                    }
                }
                localExists -> {
                    log.info("Переключаемся на локальную ветку $newBranch")
                    val checkoutResult = executor.executeCommandWithResult(listOf("git", "checkout", newBranch, "--force"))
                    if (checkoutResult.exitCode != 0) {
                        log.warn("Не удалось переключиться на ветку $newBranch: ${checkoutResult.error}")
                    }
                }
                remoteExists -> {
                    log.info("Создаем локальную ветку $newBranch из удаленной")
                    val checkoutResult = executor.executeCommandWithResult(listOf("git", "checkout", "-b", newBranch, "origin/$newBranch", "--force"))
                    if (checkoutResult.exitCode != 0) {
                        log.warn("Не удалось создать локальную ветку $newBranch из удаленной: ${checkoutResult.error}")
                    }
                }
                else -> {
                    // Создаем новую ветку
                    log.info("Создаем новую ветку $newBranch")
                    val checkoutResult = executor.executeCommandWithResult(listOf("git", "checkout", "-b", newBranch,"--force"))
                    if (checkoutResult.exitCode != 0) {
                        log.warn("Не удалось создать новую ветку $newBranch: ${checkoutResult.error}")
                    }
                }
            }
        } catch (e: Exception) {
            log.error("Ошибка при работе с ветками: ${e.message}")
            throw RuntimeException("Ошибка при работе с ветками", e)
        }
    }

    private fun afterCmdCommit(commitInfo: Commit, repoDir: File, newBranch: String) {
        executor.timeout = gitTimeout
        executor.workingDir = repoDir

        // Проверяем, есть ли изменения в репозитории перед добавлением файлов в индекс
        try {
            val statusOutput = executor.executeCommandWithResult(listOf("git", "status", "--porcelain"))
            if (statusOutput.exitCode != 0) {
                log.warn("Не удалось получить статус репозитория: ${statusOutput.error}")
            } else if (statusOutput.output.isBlank()) {
                log.info("Нет изменений для коммита в ветке $newBranch")
                // Обновляем статус коммита как завершенного, даже если нет изменений
                commitInfo.urlBranch = "${commitInfo.project!!.urlRepo}/src/branch/$newBranch"
                commitInfo.setStatus(StatusSheduler.COMPLETE)
                dataManager.save(commitInfo)
                return
            }
        } catch (e: Exception) {
            log.error("Ошибка при проверке статуса репозитория: ${e.message}")
            // Продолжаем выполнение, даже если не удалось проверить статус
        }
        
        val addResult = executor.executeCommandWithResult(listOf("git", "add", "."))
        if (addResult.exitCode != 0) {
            log.error("Не удалось добавить файлы в индекс: ${addResult.error}")
            throw RuntimeException("Не удалось добавить файлы в индекс: ${addResult.error}")
        }
        
        // 4. Создаем коммит от указанного пользователя
        // Создаем временный файл с сообщением коммита
        val commitMessage = commitInfo.description ?: "Default commit message"
        val tempFile = File.createTempFile("commit-message", ".txt")
        tempFile.writeText(commitMessage, Charsets.UTF_8)
        
        try {
            val commitResult = executor.executeCommandWithResult(
                listOf(
                    "git",
                    "-c", "user.name=${commitInfo.author!!.gitLogin}",
                    "-c", "user.email=${commitInfo.author!!.email}",
                    "commit",
                    "-F", tempFile.absolutePath
                )
            )
            
            if (commitResult.exitCode != 0) {
                log.error("Не удалось создать коммит: ${commitResult.error}")
                throw RuntimeException("Не удалось создать коммит: ${commitResult.error}")
            }
        } catch (e: Exception) {
            log.error("Не удалось создать коммит: ${e.message}")
            throw RuntimeException("Не удалось создать коммит", e)
        } finally {
            // Удаляем временный файл
            try {
                tempFile.delete()
            } catch (e: Exception) {
                log.warn("Не удалось удалить временный файл: ${e.message}")
            }
        }
        
        log.info("git push start")
        val pushResult = executor.executeCommandWithResult(listOf("git", "push", "--force", "-u", "origin", newBranch))
        if (pushResult.exitCode != 0) {
            log.error("Не удалось отправить изменения в удаленный репозиторий: ${pushResult.error}")
            throw RuntimeException("Не удалось отправить изменения в удаленный репозиторий: ${pushResult.error}")
        }
        log.info("git push end")
        
        val hashResult = executor.executeCommandWithResult(listOf("git", "rev-parse", "HEAD"))
        if (hashResult.exitCode != 0) {
            log.error("Не удалось получить хэш коммита: ${hashResult.error}")
            throw RuntimeException("Не удалось получить хэш коммита: ${hashResult.error}")
        } else {
            commitInfo.hashCommit = hashResult.output.trim()
        }
        
        log.info("Successfully committed and pushed changes to branch $newBranch")
        
        commitInfo.urlBranch = "${commitInfo.project!!.urlRepo}/tree/$newBranch"
        
        commitInfo.setStatus(StatusSheduler.COMPLETE)
        dataManager.save(commitInfo)
    }

    private fun setAuthRepo(urlRepo:String, userName:String, token:String, repoPath: String){
        var pref=""
        if (urlRepo.contains("http")){
            pref = "http://"
        }

        if (urlRepo.contains("https")){
            pref = "https://"
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

    private fun branchExists(repoPath: String, branchName: String): Boolean {
        executor.timeout = gitTimeout
        executor.workingDir = File(repoPath)
        return try {
            val result = executor.executeCommandWithResult(listOf("git", "branch", "--list", branchName))
            if (result.exitCode != 0) {
                log.error("Ошибка при проверке существования локальной ветки $branchName: ${result.error}")
                false
            } else {
                result.output.isNotBlank()
            }
        } catch (e: Exception) {
            log.error("Ошибка при проверке существования локальной ветки $branchName: ${e.message}")
            false
        }
    }
    
    private fun remoteBranchExists(repoPath: String, branchName: String): Boolean {
        executor.timeout = gitTimeout
        executor.workingDir = File(repoPath)
        return try {
            val result = executor.executeCommandWithResult(listOf("git", "branch", "-r"))
            if (result.exitCode != 0) {
                log.error("Ошибка при проверке существования удаленной ветки $branchName: ${result.error}")
                false
            } else {
                result.output.lines().any { it.trim().startsWith("origin/$branchName") }
            }
        } catch (e: Exception) {
            log.error("Ошибка при проверке существования удаленной ветки $branchName: ${e.message}")
            false
        }
    }
    
    private fun checkBranchDifference(repoPath: String, branch1: String, branch2: String): Boolean {
        executor.timeout = gitTimeout
        executor.workingDir = File(repoPath)
        try {
            // Сохраняем текущую ветку, чтобы потом вернуться
            val currentBranchResult = executor.executeCommandWithResult(listOf("git", "rev-parse", "--abbrev-ref", "HEAD"))
            if (currentBranchResult.exitCode != 0) {
                log.error("Не удалось получить текущую ветку: ${currentBranchResult.error}")
                return false
            }
            val currentBranch = currentBranchResult.output.trim()
            
            // Проверяем, можно ли выполнить слияние без конфликтов
            val checkoutResult = executor.executeCommandWithResult(listOf("git", "checkout", branch1))
            if (checkoutResult.exitCode != 0) {
                log.error("Не удалось переключиться на ветку $branch1: ${checkoutResult.error}")
                return false
            }
            
            // Выполняем слияние в режиме "только проверка" (без коммита)
            val mergeResult = executor.executeCommandWithResult(listOf("git", "merge", "--no-commit", "--no-ff", branch2))
            
            // Проверяем, есть ли конфликты
            val statusResult = executor.executeCommandWithResult(listOf("git", "status", "--porcelain"))
            val hasConflicts = if (statusResult.exitCode == 0) {
                statusResult.output.lines().any { it.startsWith("UU ") }
            } else {
                log.warn("Не удалось получить статус репозитория: ${statusResult.error}")
                false
            }
            
            // Также проверяем вывод команды merge на наличие сообщений о конфликтах
            val hasMergeConflicts = mergeResult.exitCode != 0 || mergeResult.output.contains("CONFLICT")
            
            // Возвращаемся на исходную ветку без отмены слияния
            val returnCheckoutResult = executor.executeCommandWithResult(listOf("git", "checkout", currentBranch))
            if (returnCheckoutResult.exitCode != 0) {
                log.error("Не удалось вернуться на исходную ветку $currentBranch: ${returnCheckoutResult.error}")
            }
            
            if (hasConflicts || hasMergeConflicts) {
                log.warn("Обнаружены конфликты при слиянии веток $branch1 и $branch2")
                return false
            } else {
                log.info("Слияние веток $branch1 и $branch2 не вызовет конфликтов")
                return true
            }
        } catch (e: Exception) {
            log.warn("Ошибка при проверке различий между ветками $branch1 и $branch2: ${e.message}")
            try {
                // Возвращаемся на исходную ветку без отмены слияния
                val currentBranchResult = executor.executeCommandWithResult(listOf("git", "rev-parse", "--abbrev-ref", "HEAD"))
                if (currentBranchResult.exitCode == 0) {
                    val currentBranch = currentBranchResult.output.trim()
                    val returnCheckoutResult = executor.executeCommandWithResult(listOf("git", "checkout", currentBranch))
                    if (returnCheckoutResult.exitCode != 0) {
                        log.error("Не удалось вернуться на исходную ветку: ${returnCheckoutResult.error}")
                    }
                } else {
                    log.error("Не удалось получить текущую ветку: ${currentBranchResult.error}")
                }
            } catch (abortException: Exception) {
                log.error("Не удалось вернуться на исходную ветку: ${abortException.message}")
            }
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
                return null
            } else {
                val commit = entity.get()
                commit.author = entity.get().author
                commit.files = entity.get().files
                commit.project = entity.get().project
                
                return commit
            }
        } catch (e: Exception) {
            log.error("Ошибка при получении данных о новом коммите: ${e.message}")
            throw RuntimeException("Ошибка при получении данных о новом коммите", e)
        }
    }

    private fun escapeShellArgument(arg: String): String {
        return try {
            if (arg.isEmpty()) {
                return "''"
            }
            
            // Если строка содержит пробелы, кавычки или другие спецсимволы, заключаем её в кавычки
            if (!arg.matches(Regex("^[a-zA-Z0-9_\\-+=%/:.,@]+$"))) {
                return "'" + arg.replace("'", "'\"'\"'") + "'"
            }
            
            return arg
        } catch (e: Exception) {
            log.error("Ошибка при экранировании аргумента оболочки: ${e.message}")
            throw RuntimeException("Ошибка при экранировании аргумента оболочки", e)
        }
    }

    private fun sanitizeGitBranchName(input: String): String {
        return try {
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
        } catch (e: Exception) {
            log.error("Ошибка при санитизации имени ветки Git: ${e.message}")
            throw RuntimeException("Ошибка при санитизации имени ветки Git", e)
        }
    }
}