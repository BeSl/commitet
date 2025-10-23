package com.company.commitet_jm.sheduledJob

import com.company.commitet_jm.service.git.GitService
import io.jmix.core.DataManager
import io.jmix.core.security.SystemAuthenticator
import org.quartz.Job
import org.quartz.JobExecutionContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component


@Component
class Committer(
    private val dataManager: DataManager,
    private val gitService: GitService
): Job {
    @Autowired
    private val systemAuthenticator: SystemAuthenticator? = null

    override fun execute(context: JobExecutionContext) {
        systemAuthenticator?.runWithSystem {
            gitService.createCommit()
        }
    }
}