package com.company.commitet_jm.component

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

@Component
class ShellExecutor(var workingDir: File = File("."), var timeout:Long = 1) {

    companion object {
        private  val log = LoggerFactory.getLogger(ShellExecutor::class.java)
    }

    fun executeCommand(command: List<String?>): String {
        try {
            val process = ProcessBuilder(command.filterNotNull())
                .directory(workingDir)
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .redirectError(ProcessBuilder.Redirect.PIPE)
                .start()

            // Используем ограничение по времени для предотвращения блокировки
            val finished = process.waitFor(timeout, TimeUnit.MINUTES)
            
            if (!finished) {
                process.destroyForcibly()
                throw RuntimeException("Command timed out after $timeout minutes")
            }

            val output = process.inputStream.bufferedReader().use { it.readText() }
            val error = process.errorStream.bufferedReader().use { it.readText() }

            if (process.exitValue() != 0) {
                log.error("Command failed: ${command.filterNotNull().joinToString(" ")}\nError: $error")
                throw RuntimeException("exec command failed: $error")
            }

            log.info("Command executed: ${command.filterNotNull().joinToString(" ")}\nOutput: $output")
            return output
        } catch (e: IOException) {
            log.error("IO error executing command: ${e.message}")
            throw e
        } catch (e: InterruptedException) {
            log.error("Command interrupted: ${e.message}")
            Thread.currentThread().interrupt() // Восстанавливаем статус прерывания
            throw RuntimeException("Operation interrupted")
        }
    }
}