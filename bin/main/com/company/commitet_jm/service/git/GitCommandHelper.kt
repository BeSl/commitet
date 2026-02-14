package com.company.commitet_jm.service.git

import com.company.commitet_jm.component.ShellExecutor
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.io.File

/**
 * Вспомогательный класс для выполнения Git-команд с автоматической обработкой результатов
 */
@Component
class GitCommandHelper(
    private val executor: ShellExecutor
) {
    companion object {
        private val log = LoggerFactory.getLogger(GitCommandHelper::class.java)
    }

    /**
     * Результат выполнения Git-команды
     */
    data class GitResult(
        val success: Boolean,
        val output: String,
        val error: String,
        val exitCode: Int
    )

    /**
     * Выполняет Git-команду с настройкой рабочей директории и таймаута
     * @param workingDir Рабочая директория
     * @param timeout Таймаут в секундах
     * @param command Команда для выполнения
     * @return Результат выполнения
     */
    fun execute(
        workingDir: File,
        timeout: Long,
        vararg command: String
    ): GitResult {
        executor.timeout = timeout
        executor.workingDir = workingDir

        val result = executor.executeCommandWithResult(command.toList())
        return GitResult(
            success = result.exitCode == 0,
            output = result.output,
            error = result.error,
            exitCode = result.exitCode
        )
    }

    /**
     * Выполняет Git-команду и логирует предупреждение при ошибке
     * @return true если команда успешна
     */
    fun executeWithWarning(
        workingDir: File,
        timeout: Long,
        operationName: String,
        vararg command: String
    ): Boolean {
        val result = execute(workingDir, timeout, *command)
        if (!result.success) {
            log.warn("Не удалось выполнить $operationName: ${result.error}")
        }
        return result.success
    }

    /**
     * Выполняет Git-команду и выбрасывает исключение при ошибке
     * @throws RuntimeException если команда завершилась с ошибкой
     */
    fun executeOrThrow(
        workingDir: File,
        timeout: Long,
        operationName: String,
        vararg command: String
    ): GitResult {
        val result = execute(workingDir, timeout, *command)
        if (!result.success) {
            log.error("Не удалось выполнить $operationName: ${result.error}")
            throw RuntimeException("Не удалось выполнить $operationName: ${result.error}")
        }
        return result
    }

    /**
     * Выполняет Git-команду и возвращает результат или null при ошибке
     */
    fun executeOrNull(
        workingDir: File,
        timeout: Long,
        operationName: String,
        vararg command: String
    ): GitResult? {
        val result = execute(workingDir, timeout, *command)
        if (!result.success) {
            log.error("Ошибка при выполнении $operationName: ${result.error}")
            return null
        }
        return result
    }
}
