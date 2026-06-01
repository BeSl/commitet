package com.company.commitet_jm.component

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Выполняет внешние команды (git, платформа 1С и пр.).
 *
 * Класс намеренно сделан БЕЗ изменяемого состояния: рабочий каталог и таймаут
 * передаются параметрами каждого вызова. Это потокобезопасно — один бин-синглтон
 * может одновременно использоваться Quartz-задачей, слушателем RabbitMQ и UI без
 * гонок (ранее общие поля workingDir/timeout перезаписывались конкурентными
 * вызовами, из-за чего команда могла выполниться в чужом каталоге).
 */
@Component
class ShellExecutor {

    companion object {
        private val log = LoggerFactory.getLogger(ShellExecutor::class.java)
        private val DEFAULT_DIR = File(".")
        private const val DEFAULT_TIMEOUT_MINUTES = 5L
    }

    data class CommandResult(val exitCode: Int, val output: String, val error: String)

    fun executeCommandWithResult(
        command: List<String?>,
        workingDir: File = DEFAULT_DIR,
        timeoutMinutes: Long = DEFAULT_TIMEOUT_MINUTES
    ): CommandResult {
        val cmd = command.filterNotNull()
        try {
            log.info("Executing command: ${cmd.joinToString(" ")}")
            log.info("Working directory: ${workingDir.absolutePath}")

            val process = ProcessBuilder(cmd)
                .directory(workingDir)
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .redirectError(ProcessBuilder.Redirect.PIPE)
                .start()

            // Используем ограничение по времени для предотвращения блокировки
            val finished = process.waitFor(timeoutMinutes, TimeUnit.MINUTES)

            if (!finished) {
                process.destroyForcibly()
                throw RuntimeException("Command timed out after $timeoutMinutes minutes")
            }

            val output = process.inputStream.bufferedReader().use { it.readText() }
            val error = process.errorStream.bufferedReader().use { it.readText() }
            val exitCode = process.exitValue()

            if (exitCode != 0) {
                log.error("Command failed with exit code $exitCode: ${cmd.joinToString(" ")}\nError: $error")
            } else {
                log.info("Command executed successfully: ${cmd.joinToString(" ")}\nOutput: $output")
            }

            return CommandResult(exitCode, output, error)
        } catch (e: IOException) {
            log.error("IO error executing command: ${e.message}")
            throw e
        } catch (e: InterruptedException) {
            log.error("Command interrupted: ${e.message}")
            Thread.currentThread().interrupt() // Восстанавливаем статус прерывания
            throw RuntimeException("Operation interrupted")
        }
    }

    fun executeCommand(
        command: List<String?>,
        workingDir: File = DEFAULT_DIR,
        timeoutMinutes: Long = DEFAULT_TIMEOUT_MINUTES
    ): String {
        val result = executeCommandWithResult(command, workingDir, timeoutMinutes)
        if (result.exitCode != 0) {
            throw RuntimeException("exec command failed: ${result.error}")
        }
        return result.output
    }
}
