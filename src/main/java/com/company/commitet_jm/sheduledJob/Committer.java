package com.company.commitet_jm.sheduledJob;

import com.company.commitet_jm.service.GitWorker;
import com.company.commitet_jm.service.git.GitService;
import io.jmix.core.DataManager;
import io.jmix.core.FileStorageLocator;
import io.jmix.core.security.SystemAuthenticator;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class Committer implements Job {
    @Autowired
    private GitService gitService;
    
    @Autowired
    private DataManager dataManager;
    
    @Autowired
    private FileStorageLocator fileStorageLocator;
    
    @Autowired
    private SystemAuthenticator systemAuthenticator;

    @Override
    public void execute(JobExecutionContext context) {
        systemAuthenticator.runWithSystem(() -> {
            GitWorker gitWorker = new GitWorker(
                    dataManager,
                    fileStorageLocator
            );
            gitWorker.createCommit();
            
            systemAuthenticator.runWithSystem(() -> {
                gitService.createCommit();
            });
            
            return null;
        });
    }
}