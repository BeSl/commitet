package com.company.commitet_jm.component

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

@Component
class ShellExecutor(var workingDir: File = File("."), var timeout:Long = 5) {

    companion object {
        private  val log = LoggerFactory.getLogger(ShellExecutor::class.java)
    }

    data class CommandResult(val exitCode: Int, val output: String, val error: String)

    fun executeCommandWithResult(command: List<String?>, customTimeout: Long = timeout): CommandResult {
        try {
            val process = ProcessBuilder(command.filterNotNull())
                .directory(workingDir)
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .redirectError(ProcessBuilder.Redirect.PIPE)
                .start()

            // Используем ограничение по времени для предотвращения блокировки
            val finished = process.waitFor(customTimeout, TimeUnit.MINUTES)
            
            if (!finished) {
                process.destroyForcibly()
                throw RuntimeException("Command timed out after $customTimeout minutes")
            }

            val output = process.inputStream.bufferedReader().use { it.readText() }
            val error = process.errorStream.bufferedReader().use { it.readText() }
            val exitCode = process.exitValue()

            if (exitCode != 0) {
                log.error("Command failed with exit code $exitCode: ${command.filterNotNull().joinToString(" ")}\nError: $error")
            } else {
                log.info("Command executed successfully: ${command.filterNotNull().joinToString(" ")}\nOutput: $output")
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

    fun executeCommand(command: List<String?>, customTimeout: Long = timeout): String {
        val result = executeCommandWithResult(command, customTimeout)
        if (result.exitCode != 0) {
            throw RuntimeException("exec command failed: ${result.error}")
        }
        return result.output
    }
}