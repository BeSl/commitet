package com.company.commitet_jm.component

import com.company.commitet_jm.service.GitWorker
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

@Component
class ShellExecutor(var workingDir: File = File("."), var timeout:Long = 1) {

    companion object {
        private  val log = LoggerFactory.getLogger(GitWorker::class.java)
    }

    fun executeCommand(command: List<String?>): String {
        try {
            val process = ProcessBuilder(command)
                .directory(workingDir)
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .redirectError(ProcessBuilder.Redirect.PIPE)
                .start()

            val output = process.inputStream.bufferedReader().readText()
            val error = process.errorStream.bufferedReader().readText()

            process.waitFor(timeout, TimeUnit.MINUTES)

            if (process.exitValue() != 0) {
                log.error("Command failed: ${command.joinToString(" ")}\nError: $error")
                throw RuntimeException("Git command failed: $error")
            }

            log.info("Command executed: ${command.joinToString(" ")}\nOutput: $output")
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