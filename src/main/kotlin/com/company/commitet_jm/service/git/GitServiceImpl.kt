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

    companion object {
        private val log = LoggerFactory.getLogger(GitServiceImpl::class.java)
    }

    override fun cloneRepo(repoUrl: String, directoryPath: String, branch: String): Pair<Boolean, String> {
        val executor = ShellExecutor(timeout = 7)
        log.info("start clone repo $repoUrl")
        validateGitUrl(repoUrl)
        
        val dir = File(directoryPath)
        
        if (dir.exists() && dir.list()?.isNotEmpty() == true) {
            throw IllegalArgumentException("Target directory must be empty")
        }
        
        return try {
            executor.executeCommand(listOf(
                "git", "clone",
                "--branch", branch,
                "--single-branch",
                repoUrl,
                directoryPath
            ))
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
        val executor = ShellExecutor(workingDir = repoDir, timeout = 7)
        try {
            executor.executeCommand(listOf("git", "checkout", remoteBranch))
        } catch (e: Exception) {

            if (e.message?.contains("index.lock") == true) {
                log.info("Обнаружена блокировка Git. Удаление index.lock...")

                val lockFile = File("$repoDir${File.separator}.git${File.separator}index.lock")
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

        try {
            executor.executeCommand(listOf("git", "reset", "--hard", "origin/$remoteBranch"))
        } catch (e: Exception) {
            log.error("Не удалось сбросить репозиторий к состоянию удаленной ветки $remoteBranch: ${e.message}")
            throw RuntimeException("Не удалось сбросить репозиторий к состоянию удаленной ветки $remoteBranch", e)
        }
        
        try {
            executor.executeCommand(listOf("git", "clean", "-fd"))
        } catch (e: Exception) {
            log.error("Не удалось очистить неотслеживаемые файлы: ${e.message}")
            throw RuntimeException("Не удалось очистить неотслеживаемые файлы", e)
        }

        commitInfo.id?.let { setStatusCommit(it, StatusSheduler.PROCESSED) }

        val remoteBranches: String
        try {
            remoteBranches = executor.executeCommand(listOf("git", "branch", "-a"))
        } catch (e: Exception) {
            log.error("Не удалось получить список веток: ${e.message}")
            throw RuntimeException("Не удалось получить список веток", e)
        }

        if (!remoteBranches.contains("remotes/origin/$remoteBranch")) {
            throw IllegalStateException("Default branch does not exist")
        }

        try {
            try {
                executor.executeCommand(listOf("git", "checkout", remoteBranch))
            } catch (e: Exception) {
                log.error("Не удалось переключиться на ветку $remoteBranch: ${e.message}")
                throw RuntimeException("Не удалось переключиться на ветку $remoteBranch", e)
            }
            
            try {
                executor.executeCommand(listOf("git", "fetch", "origin", remoteBranch))
            } catch (e: Exception) {
                log.error("Не удалось получить обновления для ветки $remoteBranch: ${e.message}")
                throw RuntimeException("Не удалось получить обновления для ветки $remoteBranch", e)
            }
            
            try {
                executor.executeCommand(listOf("git", "checkout", remoteBranch))
            } catch (e: Exception) {
                log.error("Не удалось переключиться на ветку $remoteBranch после получения обновлений: ${e.message}")
                throw RuntimeException("Не удалось переключиться на ветку $remoteBranch после получения обновлений", e)
            }

            // Проверяем существование ветки локально и удаленно
            val localExists = try {
                branchExists(repoPath, newBranch)
            } catch (e: Exception) {
                log.error("Ошибка при проверке существования локальной ветки $newBranch: ${e.message}")
                throw RuntimeException("Ошибка при проверке существования локальной ветки $newBranch", e)
            }
            
            val remoteExists = try {
                remoteBranchExists(repoPath, newBranch)
            } catch (e: Exception) {
                log.error("Ошибка при проверке существования удаленной ветки $newBranch: ${e.message}")
                throw RuntimeException("Ошибка при проверке существования удаленной ветки $newBranch", e)
            }
            
            // Если ветка существует локально или удаленно, проверяем разницу с develop
            if (localExists || remoteExists) {
                log.info("Ветка $newBranch уже существует ${if (localExists && remoteExists) "(локально и удаленно)" else if (localExists) "(локально)" else "(удаленно)"}")
                
                // Проверяем разницу с develop веткой
                val hasConflicts = try {
                    !checkBranchDifference(repoPath, remoteBranch, newBranch)
                } catch (e: Exception) {
                    log.error("Ошибка при проверке различий между ветками $remoteBranch и $newBranch: ${e.message}")
                    throw RuntimeException("Ошибка при проверке различий между ветками $remoteBranch и $newBranch", e)
                }
                
                if (hasConflicts) {
                    log.warn("При слиянии ветки $newBranch с $remoteBranch возможны конфликты")
                    // Можно добавить дополнительную логику обработки конфликтов
                    // Например, создать ветку с уникальным именем
                    // throw RuntimeException("Невозможно продолжить из-за конфликтов слияния")
                } else {
                    log.info("Слияние ветки $newBranch с $remoteBranch не вызовет конфликтов")
                }
                
                // Переходим на существующую ветку
                when {
                    localExists && remoteExists -> {
                        log.info("Переключаемся на локальную ветку $newBranch, которая также существует удаленно")
                        executor.executeCommand(listOf("git", "checkout", newBranch))
                        // Синхронизируем с удаленной веткой
                        try {
                            executor.executeCommand(listOf("git", "pull", "origin", newBranch))
                        } catch (e: Exception) {
                            log.error("Не удалось синхронизировать локальную ветку $newBranch с удаленной: ${e.message}")
                            throw RuntimeException("Не удалось синхронизировать локальную ветку $newBranch с удаленной", e)
                        }
                    }
                    localExists -> {
                        log.info("Переключаемся на локальную ветку $newBranch")
                        try {
                            executor.executeCommand(listOf("git", "checkout", newBranch))
                        } catch (e: Exception) {
                            log.error("Не удалось переключиться на локальную ветку $newBranch: ${e.message}")
                            throw RuntimeException("Не удалось переключиться на локальную ветку $newBranch", e)
                        }
                    }
                    remoteExists -> {
                        log.info("Создаем локальную ветку $newBranch из удаленной")
                        try {
                            executor.executeCommand(listOf("git", "checkout", "-b", newBranch, "origin/$newBranch"))
                        } catch (e: Exception) {
                            log.error("Не удалось создать локальную ветку $newBranch из удаленной: ${e.message}")
                            throw RuntimeException("Не удалось создать локальную ветку $newBranch из удаленной", e)
                        }
                    }
                }
            } else {
                // Создаем новую ветку
                log.info("Создаем новую ветку $newBranch")
                try {
                    executor.executeCommand(listOf("git", "checkout", "-b", newBranch))
                } catch (e: Exception) {
                    log.error("Не удалось создать ветку $newBranch: ${e.message}")
                    throw RuntimeException("Не удалось создать ветку $newBranch", e)
                }
            }
        } catch (e: Exception) {
            log.error("beforeCmdCommit ${e.message}")
            throw RuntimeException("Error cmd git ${e.message}")
        }
    }

    private fun afterCmdCommit(commitInfo: Commit, repoDir: File, newBranch: String) {
        val executor = ShellExecutor(workingDir = repoDir, timeout = 7)
        
        try {
            executor.executeCommand(listOf("git", "add", "."))
        } catch (e: Exception) {
            log.error("Не удалось добавить файлы в индекс: ${e.message}")
            throw RuntimeException("Не удалось добавить файлы в индекс", e)
        }
        
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
        try {
            executor.executeCommand(listOf("git", "push", "--force", "-u", "origin", newBranch))
        } catch (e: Exception) {
            log.error("Не удалось отправить изменения в удаленный репозиторий: ${e.message}")
            throw RuntimeException("Не удалось отправить изменения в удаленный репозиторий", e)
        }
        log.info("git push end")
        
        try {
            commitInfo.hashCommit = executor.executeCommand(listOf("git", "rev-parse", "HEAD"))
        } catch (e: Exception) {
            log.error("Не удалось получить хэш коммита: ${e.message}")
            throw RuntimeException("Не удалось получить хэш коммита", e)
        }
        
        log.info("Successfully committed and pushed changes to branch $newBranch")
        
        commitInfo.urlBranch = "${commitInfo.project!!.urlRepo}/tree/$newBranch"
        
        commitInfo.setStatus(StatusSheduler.COMPLETE)
        dataManager.save(commitInfo)
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
        val executor = ShellExecutor(workingDir = File(repoPath))
        return try {
            val branches = executor.executeCommand(listOf("git", "branch", "--list", branchName))
            branches.isNotBlank()
        } catch (e: Exception) {
            log.error("Ошибка при проверке существования локальной ветки $branchName: ${e.message}")
            false
        }
    }
    
    private fun remoteBranchExists(repoPath: String, branchName: String): Boolean {
        val executor = ShellExecutor(workingDir = File(repoPath))
        return try {
            val remoteBranches = executor.executeCommand(listOf("git", "branch", "-r"))
            remoteBranches.lines().any { it.trim().startsWith("origin/$branchName") }
        } catch (e: Exception) {
            log.error("Ошибка при проверке существования удаленной ветки $branchName: ${e.message}")
            false
        }
    }
    
    private fun checkBranchDifference(repoPath: String, branch1: String, branch2: String): Boolean {
        val executor = ShellExecutor(workingDir = File(repoPath))
        try {
            // Сохраняем текущую ветку, чтобы потом вернуться
            val currentBranch = executor.executeCommand(listOf("git", "rev-parse", "--abbrev-ref", "HEAD")).trim()
            
            // Проверяем, можно ли выполнить слияние без конфликтов
            executor.executeCommand(listOf("git", "checkout", branch1))
            
            // Выполняем слияние в режиме "только проверка" (без коммита)
            val mergeResult = executor.executeCommand(listOf("git", "merge", "--no-commit", "--no-ff", branch2))
            
            // Проверяем, есть ли конфликты
            val statusResult = executor.executeCommand(listOf("git", "status", "--porcelain"))
            val hasConflicts = statusResult.lines().any { it.startsWith("UU ") }
            
            // Также проверяем вывод команды merge на наличие сообщений о конфликтах
            val hasMergeConflicts = mergeResult.contains("CONFLICT")
            
            // Отменяем слияние
            executor.executeCommand(listOf("git", "merge", "--abort"))
            
            // Возвращаемся на исходную ветку
            executor.executeCommand(listOf("git", "checkout", currentBranch))
            
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
                // Пытаемся отменить слияние, если оно частично прошло
                executor.executeCommand(listOf("git", "merge", "--abort"))
                // Возвращаемся на исходную ветку
                val currentBranch = executor.executeCommand(listOf("git", "rev-parse", "--abbrev-ref", "HEAD")).trim()
                executor.executeCommand(listOf("git", "checkout", currentBranch))
            } catch (abortException: Exception) {
                log.error("Не удалось отменить слияние: ${abortException.message}")
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