package com.company.commitet_jm.sheduledJob;

import com.company.commitet_jm.service.git.GitService;
import io.jmix.core.DataManager;
import io.jmix.core.FileStorageLocator;
import io.jmix.flowui.backgroundtask.BackgroundTask;
import io.jmix.flowui.backgroundtask.TaskLifeCycle;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class GitCloneTask extends BackgroundTask<Integer, Void> {
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(GitCloneTask.class);

    private final DataManager dataManager;
    private final FileStorageLocator fileStorageLocator;
    private final GitService gitService;

    private String urlRepo = "";
    private String localPath = "";
    private String defaultBranch = "";

    public GitCloneTask(
            DataManager dataManager,
            FileStorageLocator fileStorageLocator,
            GitService gitService
    ) {
        super(10, TimeUnit.MINUTES);
        this.dataManager = dataManager;
        this.fileStorageLocator = fileStorageLocator;
        this.gitService = gitService;
    }

    @Override
    public Void run(TaskLifeCycle<Integer> taskLifeCycle) throws Exception {
        GitWorker.Pair<Boolean, String> result = gitService.cloneRepo(urlRepo + ".git", localPath, defaultBranch);

        if (!result.getFirst()) {
            log.error("Ошибка клонирования: " + result.getSecond());
        } else {
            log.info("репозиторий склонирован");
        }

        return null;
    }

    // Getters and setters
    public String getUrlRepo() {
        return urlRepo;
    }

    public void setUrlRepo(String urlRepo) {
        this.urlRepo = urlRepo;
    }

    public String getLocalPath() {
        return localPath;
    }

    public void setLocalPath(String localPath) {
        this.localPath = localPath;
    }

    public String getDefaultBranch() {
        return defaultBranch;
    }

    public void setDefaultBranch(String defaultBranch) {
        this.defaultBranch = defaultBranch;
    }
}