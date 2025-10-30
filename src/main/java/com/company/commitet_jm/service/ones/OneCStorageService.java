package com.company.commitet_jm.service.ones;

//ПРИМЕР ПУТИ после имени диска 2 СЛЭША
//        String pathSource = "\"C:\\\\develop\\test\\repo\\src\\\"";

import com.company.commitet_jm.component.ShellExecutor;
import com.company.commitet_jm.entity.OneCStorage;
import com.company.commitet_jm.entity.Platform;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class OneCStorageService {
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(OneCStorageService.class);
    private static final String CONFIG_DIR = "src";
    
    private final ShellExecutor executor;
    private final OneRunner oneRunner;

    public OneCStorageService(ShellExecutor executor, OneRunner oneRunner) {
        this.executor = executor;
        this.oneRunner = oneRunner;
    }

    public void createOneCStorage(OneCStorage storage) {
        if (storage.getProject() == null) {
            throw new IllegalArgumentException("Storage must be linked to project");
        }

        Path basePath = prepareStorageDirectory(storage);
        createEmptyBase(storage, basePath);
        loadConfiguration(storage, basePath);
        setupRepository(storage, basePath);
    }

    private Path prepareStorageDirectory(OneCStorage storage) {
        Path path = Paths.get(
                storage.getProject().getTempBasePath() != null ? storage.getProject().getTempBasePath() : "./temp",
                storage.getName()
        );

        if (!clearOrCreateDirectory(path)) {
            throw new IllegalStateException("Failed to prepare directory: " + path);
        }
        return path;
    }

    private boolean clearOrCreateDirectory(Path path) {
        try {
            if (Files.exists(path)) {
                Files.walk(path)
                        .sorted(Comparator.reverseOrder())
                        .forEach(file -> {
                            try {
                                Files.deleteIfExists(file);
                            } catch (IOException e) {
                                log.error("Failed to delete file: " + file, e);
                            }
                        });
            }
            Files.createDirectories(path);
            return true;
        } catch (IOException e) {
            log.error("Directory operation failed: " + e.getMessage());
            return false;
        }
    }

    private void createEmptyBase(OneCStorage storage, Path path) {
        String absPath = path.toAbsolutePath().toString();
        executor.executeCommand(
                java.util.Arrays.asList(
                        getPlatformPath(storage),
                        "CREATEINFOBASE",
                        "File=\"" + absPath + "\""
                )
        );
    }

    private void loadConfiguration(OneCStorage storage, Path basePath) {
        Path srcPath = Paths.get(storage.getProject().getLocalPath() != null ? storage.getProject().getLocalPath() : "", CONFIG_DIR);

        executor.executeCommand(
                java.util.Arrays.asList(
                        getPlatformPath(storage),
                        "DESIGNER",
                        "/F", basePath.toString(),
                        "/LoadConfigFromFiles", srcPath.toString(),
                        "/UpdateDBCfg"
                )
        );
    }

    private void setupRepository(OneCStorage storage, Path basePath) {
        executor.executeCommand(
                java.util.Arrays.asList(
                        getPlatformPath(storage),
                        "DESIGNER",
                        "/F", basePath.toString(),
                        "/ConfigurationRepositoryF", storage.getPath(),
                        "/ConfigurationRepositoryN", storage.getUser(),
                        "/ConfigurationRepositoryP", storage.getPassword(),
                        "/ConfigurationRepositoryCreate"
                )
        );
    }

    public boolean clearDirectoryNio(String directoryPath) {
        Path path = Paths.get(directoryPath);

        if (!Files.exists(path) || !Files.isDirectory(path)) {
            log.info("Каталог " + directoryPath + " не существует или это не каталог");
            return false;
        }

        try {
            Files.walk(path)
                    .sorted(Comparator.reverseOrder())
                    .filter(filePath -> !filePath.equals(path)) // Не удаляем сам корневой каталог
                    .forEach(filePath -> {
                        try {
                            Files.deleteIfExists(filePath);
                        } catch (IOException e) {
                            log.error("Failed to delete file: " + filePath, e);
                        }
                    });
            log.info("Содержимое каталога " + directoryPath + " успешно удалено (NIO)");
            return true;
        } catch (SecurityException e) {
            log.error("Ошибка безопасности при удалении содержимого каталога: " + e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("Произошла ошибка при удалении содержимого каталога: " + e.getMessage());
            return false;
        }
    }

    public void generateHistoryReport(OneCStorage storage, HistoryOptions options) {
        // Реализация генерации отчета
    }

    public void addUserToStorage(
            OneCStorage storage,
            String username,
            String password,
            UserRights rights,
            boolean restoreDeleted
    ) {
        // Реализация добавления пользователя
    }

    public void copyUsersBetweenStorages(
            OneCStorage source,
            OneCStorage target,
            boolean restoreDeleted
    ) {
        // Реализация копирования пользователей
    }

    public void historyStorage(
            OneCStorage storage,
            Path outputFile,
            HistoryStorageOptions options
    ) {
        if (storage.getPath() == null) {
            throw new IllegalArgumentException("Storage path must be configured");
        }
        validateFileWriteAccess(outputFile);

        List<String> command = buildHistoryCommand(storage, outputFile, options);
        executeConfigurationCommand(storage, command);
    }
    
    // Overloaded method with default options
    public void historyStorage(
            OneCStorage storage,
            Path outputFile
    ) {
        historyStorage(storage, outputFile, new HistoryStorageOptions());
    }

    public void addUserStorage(
            OneCStorage storage,
            String name,
            String password,
            UserRights rights,
            UserStorageOptions options
    ) {
        validateCredentials(name, password);
        if (storage.getPath() == null) {
            throw new IllegalArgumentException("Storage path must be configured");
        }

        List<String> command = buildAddUserCommand(storage, name, password, rights, options);
        executeConfigurationCommand(storage, command);
    }
    
    // Overloaded method with default options
    public void addUserStorage(
            OneCStorage storage,
            String name,
            String password,
            UserRights rights
    ) {
        addUserStorage(storage, name, password, rights, new UserStorageOptions());
    }

    public void copyUsersStorage(
            OneCStorage sourceStorage,
            OneCStorage targetStorage,
            String adminUser,
            String adminPassword,
            CopyUsersOptions options
    ) {
        if (sourceStorage.getPath() == null) {
            throw new IllegalArgumentException("Source storage path must be configured");
        }
        if (targetStorage.getPath() == null) {
            throw new IllegalArgumentException("Target storage path must be configured");
        }
        validateCredentials(adminUser, adminPassword);

        List<String> command = buildCopyUsersCommand(sourceStorage, targetStorage, adminUser, adminPassword, options);
        executeConfigurationCommand(sourceStorage, command);
    }
    
    // Overloaded method with default options
    public void copyUsersStorage(
            OneCStorage sourceStorage,
            OneCStorage targetStorage,
            String adminUser,
            String adminPassword
    ) {
        copyUsersStorage(sourceStorage, targetStorage, adminUser, adminPassword, new CopyUsersOptions());
    }

    private List<String> buildHistoryCommand(
            OneCStorage storage,
            Path outputFile,
            HistoryStorageOptions options
    ) {
        List<String> command = new ArrayList<>();
        command.add("DESIGNER");
        command.add("/ConfigurationRepositoryReport");
        command.add(outputFile.toAbsolutePath().toString());

        if (options.getVersionStart() != null) {
            command.add("-NBegin");
            command.add(options.getVersionStart().toString());
        }
        if (options.getVersionEnd() != null) {
            command.add("-NEnd");
            command.add(options.getVersionEnd().toString());
        }
        if (options.getDateStart() != null) {
            command.add("-DateBegin");
            command.add(options.getDateStart());
        }
        if (options.getDateEnd() != null) {
            command.add("-DateEnd");
            command.add(options.getDateEnd());
        }

        if (options.isGroupByObject()) command.add("-GroupByObject");
        if (options.isGroupByComment()) command.add("-GroupByComment");

        command.add("-ReportFormat");
        command.add(options.getReportFormat().name());

        return command;
    }

    private void executeConfigurationCommand(OneCStorage storage, List<String> commandParts) {
        try {
            List<String> baseCommand = java.util.Arrays.asList(
                    getPlatformPath(storage),
                    "/F", storage.getPath()
            );

            List<String> fullCommand = new ArrayList<>(baseCommand);
            fullCommand.addAll(commandParts);
            log.info("Executing command: " + fullCommand.stream().collect(Collectors.joining(" ")));

            executor.executeCommand(fullCommand);
        } catch (RuntimeException e) {
            log.error("Command execution failed: " + e.getMessage());
            throw new StorageOperationException("Failed to execute configuration command", e);
        }
    }

    private void validateCredentials(String username, String password) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username cannot be empty");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Password cannot be empty");
        }
    }

    private void validateFileWriteAccess(Path file) {
        if (Files.exists(file) && !Files.isWritable(file)) {
            throw new RuntimeException("No write access to file: " + file);
        }
    }

    private String getPlatformPath(OneCStorage storage) {
        Platform platform = storage.getProject().getPlatform();
        if (platform == null) {
            throw new IllegalArgumentException("Platform not configured");
        }
        return oneRunner.pathPlatform(platform.getPathInstalled(), platform.getVersion());
    }

    // Data classes for options
    public static class HistoryStorageOptions {
        private Integer versionStart;
        private Integer versionEnd;
        private String dateStart;
        private String dateEnd;
        private ReportFormat reportFormat = ReportFormat.TXT;
        private boolean groupByObject = false;
        private boolean groupByComment = false;

        public HistoryStorageOptions() {}

        // Getters and setters
        public Integer getVersionStart() { return versionStart; }
        public void setVersionStart(Integer versionStart) { this.versionStart = versionStart; }
        
        public Integer getVersionEnd() { return versionEnd; }
        public void setVersionEnd(Integer versionEnd) { this.versionEnd = versionEnd; }
        
        public String getDateStart() { return dateStart; }
        public void setDateStart(String dateStart) { this.dateStart = dateStart; }
        
        public String getDateEnd() { return dateEnd; }
        public void setDateEnd(String dateEnd) { this.dateEnd = dateEnd; }
        
        public ReportFormat getReportFormat() { return reportFormat; }
        public void setReportFormat(ReportFormat reportFormat) { this.reportFormat = reportFormat; }
        
        public boolean isGroupByObject() { return groupByObject; }
        public void setGroupByObject(boolean groupByObject) { this.groupByObject = groupByObject; }
        
        public boolean isGroupByComment() { return groupByComment; }
        public void setGroupByComment(boolean groupByComment) { this.groupByComment = groupByComment; }
    }

    public enum ReportFormat { TXT, MXL }

    public static class UserStorageOptions {
        private boolean restoreDeleted = false;
        private String extension = null;

        public UserStorageOptions() {}

        // Getters and setters
        public boolean isRestoreDeleted() { return restoreDeleted; }
        public void setRestoreDeleted(boolean restoreDeleted) { this.restoreDeleted = restoreDeleted; }
        
        public String getExtension() { return extension; }
        public void setExtension(String extension) { this.extension = extension; }
    }

    public static class CopyUsersOptions {
        private boolean restoreDeleted = false;
        private String extension = null;

        public CopyUsersOptions() {}

        // Getters and setters
        public boolean isRestoreDeleted() { return restoreDeleted; }
        public void setRestoreDeleted(boolean restoreDeleted) { this.restoreDeleted = restoreDeleted; }
        
        public String getExtension() { return extension; }
        public void setExtension(String extension) { this.extension = extension; }
    }

    public enum UserRights {
        READ_ONLY("ReadOnly"),
        FULL_ACCESS("FullAccess"),
        VERSION_MANAGEMENT("VersionManagement");

        private final String cliValue;

        UserRights(String cliValue) {
            this.cliValue = cliValue;
        }

        public String getCliValue() {
            return cliValue;
        }
    }

    public static class StorageOperationException extends RuntimeException {
        public StorageOperationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}