package com.company.commitet_jm.sheduledJob

import com.company.commitet_jm.service.git.GitService
import com.company.commitet_jm.service.ones.OneRunner
import io.jmix.core.DataManager
import io.jmix.core.FileStorageLocator
import io.jmix.core.security.SystemAuthenticator
import org.quartz.Job
import org.quartz.JobExecutionContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component


@Component

class Committer(
//    private val dataManager: DataManager,
    private val gitService: GitService
): Job {
    @Autowired
    private lateinit var dataManager: DataManager
    @Autowired
    private lateinit var fileStorageLocator: FileStorageLocator
    @Autowired
    private lateinit var systemAuthenticator: SystemAuthenticator
    @Autowired
    private lateinit var oneRunner: OneRunner

    override fun execute(context: JobExecutionContext) {
        systemAuthenticator.runWithSystem {
            systemAuthenticator.runWithSystem {
                gitService.createCommit()
            }
        }
    }
}