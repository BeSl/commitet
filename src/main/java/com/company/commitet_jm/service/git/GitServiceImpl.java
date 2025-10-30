package com.company.commitet_jm.service.git;

import com.company.commitet_jm.component.ShellExecutor;
import com.company.commitet_jm.entity.*;
import com.company.commitet_jm.entity.TypesFiles;
import com.company.commitet_jm.service.file.FileService;
import com.company.commitet_jm.service.ones.OneCService;
import io.jmix.core.DataManager;
import io.jmix.core.FileStorageLocator;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class GitServiceImpl implements GitService {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(GitServiceImpl.class);

    private final DataManager dataManager;
    private final FileStorageLocator fileStorageLocator;
    private final FileService fileService;
    private final OneCService oneCService;

    @Autowired
    public GitServiceImpl(DataManager dataManager, FileStorageLocator fileStorageLocator, 
                          FileService fileService, OneCService oneCService) {
        this.dataManager = dataManager;
        this.fileStorageLocator = fileStorageLocator;
        this.fileService = fileService;
        this.oneCService = oneCService;
    }

    @Override
    public GitWorker.Pair<Boolean, String> cloneRepo(String repoUrl, String directoryPath, String branch) {
        ShellExecutor executor = new ShellExecutor(null, 7);
        log.info("start clone repo " + repoUrl);
        validateGitUrl(repoUrl);

        File dir = new File(directoryPath);

        if (dir.exists() && dir.list() != null && dir.list().length > 0) {
            throw new IllegalArgumentException("Target directory must be empty");
        }

        try {
            executor.executeCommand(List.of(
                    "git", "clone",
                    "--branch", branch,
                    "--single-branch",
                    repoUrl,
                    directoryPath
            ));
            log.info("end clone repo " + repoUrl);
            return new GitWorker.Pair<>(true, "");
        } catch (Exception e) {
            log.error("Ошибка при клонировании репозитория " + repoUrl + ": " + e.getMessage());
            return new GitWorker.Pair<>(false, e.getMessage() != null ? e.getMessage() : "Unknown error");
        }
    }

    @Override
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
                fileService.saveFileCommit(repoPath, commitInfo.getFiles(), commitInfo.getProject().getPlatform());
            }
            afterCmdCommit(commitInfo, repoDir, newBranch);
        } catch (Exception e) {
            log.error("Error occurred while creating commit " + e.getMessage(), e);
            commitInfo.setErrorInfo(e.getMessage());
            commitInfo.setStatus(StatusSheduler.ERROR);
            try {
                dataManager.save(commitInfo);
            } catch (Exception saveException) {
                log.error("Не удалось сохранить информацию об ошибке коммита: " + saveException.getMessage(), saveException);
            }
        }
    }

    private void validateGitUrl(String url) {
        try {
            if (!url.matches("^(https?|git|ssh)://.*")) {
                throw new IllegalArgumentException("Invalid Git URL format");
            }
        } catch (Exception e) {
            log.error("Ошибка при валидации Git URL: " + e.getMessage());
            throw e;
        }
    }

    private void beforeCmdCommit(File repoDir, String remoteBranch, String newBranch, Commit commitInfo) {
        if (!new File(repoDir, ".git").exists()) {
            throw new IllegalArgumentException("Not a git repository");
        }
        String repoPath = commitInfo.getProject().getLocalPath();
        ShellExecutor executor = new ShellExecutor(repoDir, 7);
        try {
            executor.executeCommand(List.of("git", "checkout", remoteBranch));
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("index.lock")) {
                log.info("Обнаружена блокировка Git. Удаление index.lock...");

                File lockFile = new File(repoDir, ".git/index.lock");
                if (lockFile.exists()) {
                    lockFile.delete();
                    log.info("Файл index.lock удалён. Повторная попытка...");

                    executor.executeCommand(List.of("git", "checkout", remoteBranch));
                } else {
                    throw new IllegalStateException("Файл блокировки не найден, но ошибка возникла: " + e.getMessage());
                }
            } else if (e.getMessage() != null && e.getMessage().contains("would be overwritten by checkout")) {
                executor.executeCommand(List.of("git", "reset", "--hard"));
                executor.executeCommand(List.of("git", "clean", "-fd"));
            } else {
                throw e;
            }
        }

        try {
            executor.executeCommand(List.of("git", "reset", "--hard", "origin/" + remoteBranch));
        } catch (Exception e) {
            log.error("Не удалось сбросить репозиторий к состоянию удаленной ветки " + remoteBranch + ": " + e.getMessage());
            throw new RuntimeException("Не удалось сбросить репозиторий к состоянию удаленной ветки " + remoteBranch, e);
        }

        try {
            executor.executeCommand(List.of("git", "clean", "-fd"));
        } catch (Exception e) {
            log.error("Не удалось очистить неотслеживаемые файлы: " + e.getMessage());
            throw new RuntimeException("Не удалось очистить неотслеживаемые файлы", e);
        }

        if (commitInfo.getId() != null) {
            setStatusCommit(commitInfo.getId(), StatusSheduler.PROCESSED);
        }

        String remoteBranches;
        try {
            remoteBranches = executor.executeCommand(List.of("git", "branch", "-a"));
        } catch (Exception e) {
            log.error("Не удалось получить список веток: " + e.getMessage());
            throw new RuntimeException("Не удалось получить список веток", e);
        }

        if (!remoteBranches.contains("remotes/origin/" + remoteBranch)) {
            throw new IllegalStateException("Default branch does not exist");
        }

        try {
            try {
                executor.executeCommand(List.of("git", "checkout", remoteBranch));
            } catch (Exception e) {
                log.error("Не удалось переключиться на ветку " + remoteBranch + ": " + e.getMessage());
                throw new RuntimeException("Не удалось переключиться на ветку " + remoteBranch, e);
            }

            try {
                executor.executeCommand(List.of("git", "fetch", "origin", remoteBranch));
            } catch (Exception e) {
                log.error("Не удалось получить обновления для ветки " + remoteBranch + ": " + e.getMessage());
                throw new RuntimeException("Не удалось получить обновления для ветки " + remoteBranch, e);
            }

            try {
                executor.executeCommand(List.of("git", "checkout", remoteBranch));
            } catch (Exception e) {
                log.error("Не удалось переключиться на ветку " + remoteBranch + " после получения обновлений: " + e.getMessage());
                throw new RuntimeException("Не удалось переключиться на ветку " + remoteBranch + " после получения обновлений", e);
            }

            // Проверяем существование ветки локально и удаленно
            boolean localExists;
            try {
                localExists = branchExists(repoPath, newBranch);
            } catch (Exception e) {
                log.error("Ошибка при проверке существования локальной ветки " + newBranch + ": " + e.getMessage());
                throw new RuntimeException("Ошибка при проверке существования локальной ветки " + newBranch, e);
            }

            boolean remoteExists;
            try {
                remoteExists = remoteBranchExists(repoPath, newBranch);
            } catch (Exception e) {
                log.error("Ошибка при проверке существования удаленной ветки " + newBranch + ": " + e.getMessage());
                throw new RuntimeException("Ошибка при проверке существования удаленной ветки " + newBranch, e);
            }

            // Если ветка существует локально или удаленно, проверяем разницу с develop
            if (localExists || remoteExists) {
                log.info("Ветка " + newBranch + " уже существует " + 
                        (localExists && remoteExists ? "(локально и удаленно)" : 
                         localExists ? "(локально)" : "(удаленно)"));

                // Проверяем разницу с develop веткой
                boolean hasConflicts;
                try {
                    hasConflicts = !checkBranchDifference(repoPath, remoteBranch, newBranch);
                } catch (Exception e) {
                    log.error("Ошибка при проверке различий между ветками " + remoteBranch + " и " + newBranch + ": " + e.getMessage());
                    throw new RuntimeException("Ошибка при проверке различий между ветками " + remoteBranch + " и " + newBranch, e);
                }

                if (hasConflicts) {
                    log.warn("При слиянии ветки " + newBranch + " с " + remoteBranch + " возможны конфликты");
                    // Можно добавить дополнительную логику обработки конфликтов
                    // Например, создать ветку с уникальным именем
                    // throw new RuntimeException("Невозможно продолжить из-за конфликтов слияния");
                } else {
                    log.info("Слияние ветки " + newBranch + " с " + remoteBranch + " не вызовет конфликтов");
                }

                // Переходим на существующую ветку
                if (localExists && remoteExists) {
                    log.info("Переключаемся на локальную ветку " + newBranch + ", которая также существует удаленно");
                    executor.executeCommand(List.of("git", "checkout", newBranch));
                    // Синхронизируем с удаленной веткой
                    try {
                        executor.executeCommand(List.of("git", "pull", "origin", newBranch));
                    } catch (Exception e) {
                        log.error("Не удалось синхронизировать локальную ветку " + newBranch + " с удаленной: " + e.getMessage());
                        throw new RuntimeException("Не удалось синхронизировать локальную ветку " + newBranch + " с удаленной", e);
                    }
                } else if (localExists) {
                    log.info("Переключаемся на локальную ветку " + newBranch);
                    try {
                        executor.executeCommand(List.of("git", "checkout", newBranch));
                    } catch (Exception e) {
                        log.error("Не удалось переключиться на локальную ветку " + newBranch + ": " + e.getMessage());
                        throw new RuntimeException("Не удалось переключиться на локальную ветку " + newBranch, e);
                    }
                } else if (remoteExists) {
                    log.info("Создаем локальную ветку " + newBranch + " из удаленной");
                    try {
                        executor.executeCommand(List.of("git", "checkout", "-b", newBranch, "origin/" + newBranch));
                    } catch (Exception e) {
                        log.error("Не удалось создать локальную ветку " + newBranch + " из удаленной: " + e.getMessage());
                        throw new RuntimeException("Не удалось создать локальную ветку " + newBranch + " из удаленной", e);
                    }
                }
            } else {
                // Создаем новую ветку
                log.info("Создаем новую ветку " + newBranch);
                try {
                    executor.executeCommand(List.of("git", "checkout", "-b", newBranch));
                } catch (Exception e) {
                    log.error("Не удалось создать ветку " + newBranch + ": " + e.getMessage());
                    throw new RuntimeException("Не удалось создать ветку " + newBranch, e);
                }
            }
        } catch (Exception e) {
            log.error("beforeCmdCommit " + e.getMessage());
            throw new RuntimeException("Error cmd git " + e.getMessage());
        }
    }

    private void afterCmdCommit(Commit commitInfo, File repoDir, String newBranch) {
        ShellExecutor executor = new ShellExecutor(repoDir, 7);

        try {
            executor.executeCommand(List.of("git", "add", "."));
        } catch (Exception e) {
            log.error("Не удалось добавить файлы в индекс: " + e.getMessage());
            throw new RuntimeException("Не удалось добавить файлы в индекс", e);
        }

        // 4. Создаем коммит от указанного пользователя
        // Создаем временный файл с сообщением коммита
        String commitMessage = commitInfo.getDescription() != null ? commitInfo.getDescription() : "Default commit message";
        File tempFile;
        try {
            tempFile = File.createTempFile("commit-message", ".txt");
            tempFile.deleteOnExit();
            java.nio.file.Files.write(tempFile.toPath(), commitMessage.getBytes("UTF-8"));
        } catch (IOException e) {
            throw new RuntimeException("Failed to create temporary file for commit message", e);
        }

        try {
            executor.executeCommand(
                    List.of(
                            "git",
                            "-c", "user.name=" + commitInfo.getAuthor().getGitLogin(),
                            "-c", "user.email=" + commitInfo.getAuthor().getEmail(),
                            "commit",
                            "-F", tempFile.getAbsolutePath()
                    )
            );
        } catch (Exception e) {
            log.error("Не удалось создать коммит: " + e.getMessage());
            throw new RuntimeException("Не удалось создать коммит", e);
        } finally {
            // Удаляем временный файл
            try {
                tempFile.delete();
            } catch (Exception e) {
                log.warn("Не удалось удалить временный файл: " + e.getMessage());
            }
        }

        log.info("git push start");
        try {
            executor.executeCommand(List.of("git", "push", "--force", "-u", "origin", newBranch));
        } catch (Exception e) {
            log.error("Не удалось отправить изменения в удаленный репозиторий: " + e.getMessage());
            throw new RuntimeException("Не удалось отправить изменения в удаленный репозиторий", e);
        }
        log.info("git push end");

        try {
            commitInfo.setHashCommit(executor.executeCommand(List.of("git", "rev-parse", "HEAD")));
        } catch (Exception e) {
            log.error("Не удалось получить хэш коммита: " + e.getMessage());
            throw new RuntimeException("Не удалось получить хэш коммита", e);
        }

        log.info("Successfully committed and pushed changes to branch " + newBranch);

        commitInfo.setUrlBranch(commitInfo.getProject().getUrlRepo() + "/tree/" + newBranch);

        commitInfo.setStatus(StatusSheduler.COMPLETE);
        dataManager.save(commitInfo);
    }

    private void setStatusCommit(UUID commitId, StatusSheduler status) {
        try {
            Commit commit = dataManager.load(Commit.class)
                    .id(commitId)
                    .one();
            commit.setStatus(status);
            dataManager.save(commit);
        } catch (Exception e) {
            log.error("Не удалось обновить статус коммита " + commitId + ": " + e.getMessage());
            throw new RuntimeException("Не удалось обновить статус коммита " + commitId, e);
        }
    }

    private boolean branchExists(String repoPath, String branchName) {
        ShellExecutor executor = new ShellExecutor(new File(repoPath), 7);
        try {
            String branches = executor.executeCommand(List.of("git", "branch", "--list", branchName));
            return !branches.isBlank();
        } catch (Exception e) {
            log.error("Ошибка при проверке существования локальной ветки " + branchName + ": " + e.getMessage());
            return false;
        }
    }

    private boolean remoteBranchExists(String repoPath, String branchName) {
        ShellExecutor executor = new ShellExecutor(new File(repoPath), 7);
        try {
            String remoteBranches = executor.executeCommand(List.of("git", "branch", "-r"));
            String[] lines = remoteBranches.split("\n");
            for (String line : lines) {
                if (line.trim().startsWith("origin/" + branchName)) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            log.error("Ошибка при проверке существования удаленной ветки " + branchName + ": " + e.getMessage());
            return false;
        }
    }

    private boolean checkBranchDifference(String repoPath, String branch1, String branch2) {
        ShellExecutor executor = new ShellExecutor(new File(repoPath), 7);
        try {
            // Сохраняем текущую ветку, чтобы потом вернуться
            String currentBranch = executor.executeCommand(List.of("git", "rev-parse", "--abbrev-ref", "HEAD")).trim();

            // Проверяем, можно ли выполнить слияние без конфликтов
            executor.executeCommand(List.of("git", "checkout", branch1));

            // Выполняем слияние в режиме "только проверка" (без коммита)
            String mergeResult = executor.executeCommand(List.of("git", "merge", "--no-commit", "--no-ff", branch2));

            // Проверяем, есть ли конфликты
            String statusResult = executor.executeCommand(List.of("git", "status", "--porcelain"));
            String[] lines = statusResult.split("\n");
            boolean hasConflicts = false;
            for (String line : lines) {
                if (line.startsWith("UU ")) {
                    hasConflicts = true;
                    break;
                }
            }

            // Также проверяем вывод команды merge на наличие сообщений о конфликтах
            boolean hasMergeConflicts = mergeResult.contains("CONFLICT");

            // Отменяем слияние
            executor.executeCommand(List.of("git", "merge", "--abort"));

            // Возвращаемся на исходную ветку
            executor.executeCommand(List.of("git", "checkout", currentBranch));

            if (hasConflicts || hasMergeConflicts) {
                log.warn("Обнаружены конфликты при слиянии веток " + branch1 + " и " + branch2);
                return false;
            } else {
                log.info("Слияние веток " + branch1 + " и " + branch2 + " не вызовет конфликтов");
                return true;
            }
        } catch (Exception e) {
            log.warn("Ошибка при проверке различий между ветками " + branch1 + " и " + branch2 + ": " + e.getMessage());
            try {
                // Пытаемся отменить слияние, если оно частично прошло
                executor.executeCommand(List.of("git", "merge", "--abort"));
                // Возвращаемся на исходную ветку
                String currentBranch = executor.executeCommand(List.of("git", "rev-parse", "--abbrev-ref", "HEAD")).trim();
                executor.executeCommand(List.of("git", "checkout", currentBranch));
            } catch (Exception abortException) {
                log.error("Не удалось отменить слияние: " + abortException.getMessage());
            }
            return false;
        }
    }

    private Commit firstNewDataCommit() {
        try {
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
        } catch (Exception e) {
            log.error("Ошибка при получении данных о новом коммите: " + e.getMessage());
            throw new RuntimeException("Ошибка при получении данных о новом коммите", e);
        }
    }

    private String escapeShellArgument(String arg) {
        try {
            if (arg.isEmpty()) {
                return "''";
            }

            // Если строка содержит пробелы, кавычки или другие спецсимволы, заключаем её в кавычки
            if (!arg.matches("^[a-zA-Z0-9_\\-+=%/:.,@]+$")) {
                return "'" + arg.replace("'", "'\"'\"'") + "'";
            }

            return arg;
        } catch (Exception e) {
            log.error("Ошибка при экранировании аргумента оболочки: " + e.getMessage());
            throw new RuntimeException("Ошибка при экранировании аргумента оболочки", e);
        }
    }

    private String sanitizeGitBranchName(String input) {
        try {
            // Правила для имён веток Git:
            // - Не могут начинаться с '-'
            // - Не могут содержать:
            //   - пробелы
            //   - символы: ~, ^, :, *, ?, [, ], @, \, /, {, }, ...
            // - Не могут заканчиваться на .lock
            // - Не могут содержать последовательность //

            Set<Character> forbiddenChars = Set.of(
                    ' ', '~', '^', ':', '*', '?', '[', ']', '@', '\\', '/', '{', '}',
                    '<', '>', '|', '"', '\'', '!', '#', '$', '%', '&', '(', ')', ',', ';', '='
            );

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
        } catch (Exception e) {
            log.error("Ошибка при санитизации имени ветки Git: " + e.getMessage());
            throw new RuntimeException("Ошибка при санитации имени ветки Git", e);
        }
    }
}