package com.company.commitet_jm.service.git;

import com.company.commitet_jm.service.GitWorker;

public interface GitService {
    GitWorker.Pair<Boolean, String> cloneRepo(String repoUrl, String directoryPath, String branch);
    void createCommit();
}