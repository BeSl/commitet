package com.company.commitet_jm.component;

import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
public class ShellExecutor {
    private File workingDir = new File(".");
    private long timeout = 1;
    
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(ShellExecutor.class);

    public ShellExecutor() {
    }

    public ShellExecutor(File workingDir, long timeout) {
        this.workingDir = workingDir;
        this.timeout = timeout;
    }

    public String executeCommand(List<String> command) {
        try {
            List<String> filteredCommand = command.stream()
                    .filter(s -> s != null)
                    .collect(Collectors.toList());
            
            Process process = new ProcessBuilder(filteredCommand)
                    .directory(workingDir)
                    .redirectOutput(ProcessBuilder.Redirect.PIPE)
                    .redirectError(ProcessBuilder.Redirect.PIPE)
                    .start();

            // Используем ограничение по времени для предотвращения блокировки
            boolean finished = process.waitFor(timeout, TimeUnit.MINUTES);

            if (!finished) {
                process.destroyForcibly();
                throw new RuntimeException("Command timed out after " + timeout + " minutes");
            }

            String output = new String(process.getInputStream().readAllBytes());
            String error = new String(process.getErrorStream().readAllBytes());

            if (process.exitValue() != 0) {
                log.error("Command failed: " + String.join(" ", filteredCommand) + "\nError: " + error);
                throw new RuntimeException("exec command failed: " + error);
            }

            log.info("Command executed: " + String.join(" ", filteredCommand) + "\nOutput: " + output);
            return output;
        } catch (IOException e) {
            log.error("IO error executing command: " + e.getMessage());
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            log.error("Command interrupted: " + e.getMessage());
            Thread.currentThread().interrupt(); // Восстанавливаем статус прерывания
            throw new RuntimeException("Operation interrupted");
        }
    }

    // Getters and setters
    public File getWorkingDir() {
        return workingDir;
    }

    public void setWorkingDir(File workingDir) {
        this.workingDir = workingDir;
    }

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }
}