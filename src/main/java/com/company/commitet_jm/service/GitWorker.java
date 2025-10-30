package com.company.commitet_jm.service;

import com.company.commitet_jm.component.ShellExecutor;
import com.company.commitet_jm.entity.*;
import com.company.commitet_jm.entity.TypesFiles;
import com.company.commitet_jm.service.ones.OneRunner;
import io.jmix.core.DataManager;
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
import java.util.*;
import java.util.regex.Pattern;

@Service
public class GitWorker {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(GitWorker.class);

    private final DataManager dataManager;
    private final FileStorageLocator fileStorageLocator;

    @Autowired
    private OneRunner ones;

    public GitWorker(DataManager dataManager, FileStorageLocator fileStorageLocator) {
        this.dataManager = dataManager;
        this.fileStorageLocator = fileStorageLocator;
    }

    public Pair<Boolean, String> cloneRepo(String repoUrl, String directoryPath, String branch) {
        ShellExecutor executor = new ShellExecutor(null, 7);
        log.info("start clone repo " + repoUrl);
        validateGitUrl(repoUrl);

        File dir = new File(directoryPath);

        if (dir.exists() && dir.list() != null && dir.list().length > 0) {
            throw new IllegalArgumentException("Target directory must be empty");
        }

        executor.executeCommand(Arrays.asList(
                "git", "clone",
                "--branch", branch,
                "--single-branch",
                repoUrl,
                directoryPath
        ));
        log.info("end clone repo " + repoUrl);
        return new Pair<>(true, "");
    }

    public void createCommit() {
        Commit commitInfo = firstNewDataCommit();
        if (commitInfo == null) return;

        log.info("start createCommit " + commitInfo.getTaskNum());

        String repoPath = commitInfo.getProject().getLocalPath();
        File repoDir = commitInfo.getProject().getLocalPath() != null ? new File(commitInfo.getProject().getLocalPath()) : null;
        String remoteBranch = commitInfo.getProject().getDefaultBranch();
        String newBranch = "feature/" + (commitInfo.getTaskNum() != null ? sanitizeGitBranchName(commitInfo.getTaskNum()) : "");

        try {
            if (repoDir != null) {
                beforeCmdCommit(repoDir, remoteBranch, newBranch, commitInfo);
            } else {
                throw new RuntimeException(repoDir + " not exist!!!");
            }

            if (commitInfo.getProject().getPlatform() != null) {
                saveFileCommit(repoPath, commitInfo.getFiles(), commitInfo.getProject().getPlatform());
            }
            afterCmdCommit(commitInfo, repoDir, newBranch);
        } catch (Exception e) {
            log.error("Error occurred while creating commit " + e.getMessage());
            commitInfo.setErrorInfo(e.getMessage());
            commitInfo.setStatus(StatusSheduler.ERROR);
            dataManager.save(commitInfo);
        }
    }

    private void validateGitUrl(String url) {
        if (!url.matches("^(https?|git|ssh)://.*")) {
            throw new IllegalArgumentException("Invalid Git URL format");
        }
    }

    private void beforeCmdCommit(File repoDir, String remoteBranch, String newBranch, Commit commitInfo) {
        if (!new File(repoDir, ".git").exists()) {
            throw new IllegalArgumentException("Not a git repository");
        }
        String repoPath = commitInfo.getProject().getLocalPath();
        ShellExecutor executor = new ShellExecutor(repoDir, 7);
        try {
            executor.executeCommand(Arrays.asList("git", "checkout", remoteBranch));
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("index.lock")) {
                log.info("Обнаружена блокировка Git. Удаление index.lock...");

                File lockFile = new File(repoDir, ".git/index.lock");
                if (lockFile.exists()) {
                    lockFile.delete();
                    log.info("Файл index.lock удалён. Повторная попытка...");

                    executor.executeCommand(Arrays.asList("git", "checkout", remoteBranch));
                } else {
                    throw new IllegalStateException("Файл блокировки не найден, но ошибка возникла: " + e.getMessage());
                }
            } else if (e.getMessage() != null && e.getMessage().contains("would be overwritten by checkout")) {
                executor.executeCommand(Arrays.asList("git", "reset", "--hard"));
                executor.executeCommand(Arrays.asList("git", "clean", "-fd"));
            } else {
                throw new RuntimeException(e);
            }
        }

        executor.executeCommand(Arrays.asList("git", "reset", "--hard", "origin/" + remoteBranch));
        executor.executeCommand(Arrays.asList("git", "clean", "-fd"));

        if (commitInfo.getId() != null) {
            setStatusCommit(commitInfo.getId(), StatusSheduler.PROCESSED);
        }

        String remoteBranches = executor.executeCommand(Arrays.asList("git", "branch", "-a"));

        if (!remoteBranches.contains("remotes/origin/" + remoteBranch)) {
            throw new IllegalStateException("Default branch does not exist");
        }

        try {
            executor.executeCommand(Arrays.asList("git", "checkout", remoteBranch));
            executor.executeCommand(Arrays.asList("git", "fetch", "origin", remoteBranch));
            executor.executeCommand(Arrays.asList("git", "checkout", remoteBranch));

            // Создаем новую ветку
            if (branchExists(repoPath, newBranch)) {
                log.info("create new branch " + newBranch);
                executor.executeCommand(Arrays.asList("git", "checkout", newBranch));
            } else {
                log.info("checkout branch " + newBranch);
                executor.executeCommand(Arrays.asList("git", "checkout", "-b", newBranch));
            }
        } catch (Exception e) {
            log.error("beforeCmdCommit " + e.getMessage());
            throw new RuntimeException("Error cmd git " + e.getMessage());
        }
    }

    private void afterCmdCommit(Commit commitInfo, File repoDir, String newBranch) {
        ShellExecutor executor = new ShellExecutor(repoDir, 7);
        executor.executeCommand(Arrays.asList("git", "add", "."));

        // 4. Создаем коммит от указанного пользователя
        // Создаем временный файл с сообщением коммита
        String commitMessage = commitInfo.getDescription() != null ? commitInfo.getDescription() : "Default commit message";
        try {
            File tempFile = File.createTempFile("commit-message", ".txt");
            tempFile.deleteOnExit();
            Files.write(tempFile.toPath(), commitMessage.getBytes("UTF-8"));

            try {
                executor.executeCommand(
                        Arrays.asList(
                                "git",
                                "-c", "user.name=" + commitInfo.getAuthor().getGitLogin(),
                                "-c", "user.email=" + commitInfo.getAuthor().getEmail(),
                                "commit",
                                "-F", tempFile.getAbsolutePath()
                        )
                );
            } finally {
                // Удаляем временный файл
                tempFile.delete();
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to create temporary file for commit message", e);
        }

        log.info("git push start");
        executor.executeCommand(Arrays.asList("git", "push", "--force", "-u", "origin", newBranch));
        log.info("git push end");
        commitInfo.setHashCommit(executor.executeCommand(Arrays.asList("git", "rev-parse", "HEAD")));

        log.info("Successfully committed and pushed changes to branch " + newBranch);

        commitInfo.setUrlBranch(commitInfo.getProject().getUrlRepo() + "/tree/" + newBranch);

        commitInfo.setStatus(StatusSheduler.COMPLETE);
        dataManager.save(commitInfo);
    }

    private void saveFileCommit(String baseDir, List<FileCommit> files, Platform platform) {
        ShellExecutor executor = new ShellExecutor(new File(baseDir), 7);
        List<Pair<String, String>> filesToUnpack = new ArrayList<>();
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
                    filesToUnpack.add(new Pair<>(targetPath.toString(), unpackPath));
                }
            }
        }
        if (!filesToUnpack.isEmpty()) {
            unpackFiles(filesToUnpack, platform, executor, baseDir);
        }
    }

    private void unpackFiles(List<Pair<String, String>> files, Platform platform, ShellExecutor executor, String baseDir) {
        if (files.isEmpty()) {
            return;
        }

        for (Pair<String, String> filePair : files) {
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

    private List<File> findBinaryFilesFromGitStatus(String repoDir, ShellExecutor executor) {
        // Получаем список изменённых файлов
        String gitOutput = executor.executeCommand(Arrays.asList("git", "-C", repoDir, "status", "--porcelain")).trim();
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

    private File correctPath(String baseDir, TypesFiles type) {
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

    private void setStatusCommit(UUID commitId, StatusSheduler status) {
        Commit commit = dataManager.load(Commit.class)
                .id(commitId)
                .one();
        commit.setStatus(status);
        dataManager.save(commit);
    }

    private boolean branchExists(String repoPath, String branchName) {
        ShellExecutor executor = new ShellExecutor(new File(repoPath), 7);
        String branches = executor.executeCommand(Arrays.asList("git", "branch", "--list", branchName));

        return !branches.isBlank();
    }

    private Commit firstNewDataCommit() {
        Optional<Commit> entity = dataManager.load(Commit.class)
                .query("select cmt from Commit_ cmt where cmt.status = :status1 order by cmt.id asc")
                .parameter("status1", StatusSheduler.NEW)
                .optional();

        if (!entity.isPresent()) {
            return null;
        } else {
            Commit commit = entity.get();
            // These loads are likely unnecessary as the relationships should be loaded with the query
            // but keeping them for now to match the original behavior
            commit.setAuthor(entity.get().getAuthor());
            commit.setFiles(entity.get().getFiles());
            commit.setProject(entity.get().getProject());

            return commit;
        }
    }

    private String escapeShellArgument(String arg) {
        if (arg.isEmpty()) {
            return "''";
        }

        // Если строка содержит пробелы, кавычки или другие спецсимволы, заключаем её в кавычки
        if (!arg.matches("^[a-zA-Z0-9_\\-+=%/:.,@]+$")) {
            return "'" + arg.replace("'", "'\"'\"'") + "'";
        }

        return arg;
    }

    private String sanitizeGitBranchName(String input) {
        // Правила для имён веток Git:
        // - Не могут начинаться с '-'
        // - Не могут содержать:
        //   - пробелы
        //   - символы: ~, ^, :, *, ?, [, ], @, \, /, {, }, ...
        // - Не могут заканчиваться на .lock
        // - Не могут содержать последовательность //

        Set<Character> forbiddenChars = new HashSet<>(Arrays.asList(
                ' ', '~', '^', ':', '*', '?', '[', ']', '@', '\\', '/', '{', '}',
                '<', '>', '|', '"', '\'', '!', '#', '$', '%', '&', '(', ')', ',', ';', '='
        ));

        StringBuilder result = new StringBuilder();
        for (char c : input.toCharArray()) {
            if (forbiddenChars.contains(c)) {
                result.append('_');
            } else {
                result.append(c);
            }
        }

        String sanitized = result.toString()
                .replaceAll("^\\.", "") // Убираем точку в начале (если есть)
                .replaceAll("[/\\\\]+", "_") // Заменяем несколько / или \ на один _
                .replaceAll("[._]{2,}", "_") // Заменяем несколько точек или _ подряд на один _
                .replaceAll("_+$", ""); // Убираем _ в конце

        return sanitized;
    }

    // Simple Pair class since Java doesn't have one in standard library
    public static class Pair<T, U> {
        private final T first;
        private final U second;

        public Pair(T first, U second) {
            this.first = first;
            this.second = second;
        }

        public T getFirst() {
            return first;
        }

        public U getSecond() {
            return second;
        }
    }
}