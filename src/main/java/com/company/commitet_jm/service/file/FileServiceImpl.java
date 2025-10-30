package com.company.commitet_jm.service.file;

import com.company.commitet_jm.component.ShellExecutor;
import com.company.commitet_jm.entity.FileCommit;
import com.company.commitet_jm.entity.Platform;
import com.company.commitet_jm.entity.TypesFiles;
import com.company.commitet_jm.service.GitWorker;
import com.company.commitet_jm.service.ones.OneRunner;
import io.jmix.core.FileStorage;
import io.jmix.core.FileStorageLocator;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

@Service
public class FileServiceImpl implements FileService {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(FileServiceImpl.class);

    private final FileStorageLocator fileStorageLocator;
    private final OneRunner ones;

    @Autowired
    public FileServiceImpl(FileStorageLocator fileStorageLocator, OneRunner ones) {
        this.fileStorageLocator = fileStorageLocator;
        this.ones = ones;
    }

    @Override
    public void saveFileCommit(String baseDir, List<FileCommit> files, Platform platform) {
        ShellExecutor executor = new ShellExecutor(new File(baseDir), 7);
        List<GitWorker.Pair<String, String>> filesToUnpack = new ArrayList<>();
        for (FileCommit file : files) {
            if (file.getData() == null) continue;

            // correctPath возвращает File, приводим к Path
            Path path = file.getType() != null ? correctPath(baseDir, file.getType()).toPath() : null;
            if (path == null) continue;
            Path targetPath = path.resolve(file.getName()).normalize();

            try {
                // Создаем директории, если нужно
                Files.createDirectories(targetPath.getParent());
            } catch (IOException e) {
                throw new RuntimeException("Не удалось создать директорию: " + targetPath.getParent(), e);
            }

            FileStorage fileStorage = fileStorageLocator.getDefault();
            try {
                fileStorage.openStream(file.getData()).use(inputStream -> {
                    try {
                        Files.copy(
                                inputStream,
                                targetPath,
                                StandardCopyOption.REPLACE_EXISTING
                        );
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to copy file", e);
                    }
                });
            } catch (Exception e) {
                throw new RuntimeException("Failed to process file", e);
            }

            if (file.getType() != null) {
                TypesFiles fileType = file.getType();
                String unpackPath = null;
                switch (fileType) {
                    case REPORT:
                        unpackPath = baseDir + "\\DataProcessorsExt\\erf";
                        break;
                    case DATAPROCESSOR:
                        unpackPath = baseDir + "\\DataProcessorsExt\\epf";
                        break;
                    default:
                        break;
                }
                if (unpackPath != null) {
                    filesToUnpack.add(new GitWorker.Pair<>(targetPath.toString(), unpackPath));
                }
            }
        }
        if (!filesToUnpack.isEmpty()) {
            unpackFiles(filesToUnpack, platform, executor, baseDir);
        }
    }

    @Override
    public File correctPath(String baseDir, TypesFiles type) {
        switch (type) {
            case REPORT:
                return new File(baseDir, "DataProcessorsExt\\Отчет\\");
            case DATAPROCESSOR:
                return new File(baseDir, "DataProcessorsExt\\Обработка\\");
            case SCHEDULEDJOBS:
            case EXTERNAL_CODE:
                return new File(baseDir, "CodeExt");
            case EXCHANGE_RULES:
                return new File(baseDir, "EXCHANGE_RULES");
            default:
                throw new IllegalArgumentException("Unknown file type: " + type);
        }
    }

    @Override
    public List<File> findBinaryFilesFromGitStatus(String repoDir, ShellExecutor executor) {
        // Получаем список изменённых файлов
        String gitOutput = executor.executeCommand(List.of("git", "-C", repoDir, "status", "--porcelain")).trim();
        if (gitOutput.isBlank()) return new ArrayList<>();

        // Выделяем директории из вывода git status
        List<File> changedDirs = new ArrayList<>();
        String[] lines = gitOutput.split("\n");
        for (String line : lines) {
            if (!line.trim().isEmpty()) {
                String filePath = line.substring(3).trim();
                if (!filePath.isBlank()) {
                    changedDirs.add(new File(repoDir, filePath));
                }
            }
        }

        // Ищем .bin файлы в изменённых директориях
        List<File> tDir = new ArrayList<>();
        for (File dir : changedDirs) {
            try {
                Files.walk(dir.toPath())
                        .filter(path -> path.toFile().isFile() && path.getFileName().toString().endsWith("Form.bin"))
                        .forEach(path -> tDir.add(path.toFile()));
            } catch (IOException e) {
                log.error("Error walking directory: " + dir, e);
            }
        }
        return tDir;
    }

    @Override
    public void unpackFiles(List<GitWorker.Pair<String, String>> files, Platform platform, ShellExecutor executor, String baseDir) {
        if (files.isEmpty()) {
            return;
        }

        for (GitWorker.Pair<String, String> filePair : files) {
            String sourcePath = filePair.getFirst();
            String unpackPath = filePair.getSecond();
            ones.uploadExtFiles(new File(sourcePath), unpackPath, platform.getPathInstalled(), platform.getVersion());
        }

        List<File> bFiles = findBinaryFilesFromGitStatus(baseDir, executor);
        if (bFiles.isEmpty()) {
            return;
        }
        for (File binFile : bFiles) {
            ones.unpackExtFiles(binFile, binFile.getParent());
        }
    }
}