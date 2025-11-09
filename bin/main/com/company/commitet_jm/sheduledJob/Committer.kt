package com.company.commitet_jm.sheduledJob

import com.company.commitet_jm.service.GitWorker
import com.company.commitet_jm.service.git.GitServiceImpl
import io.jmix.core.DataManager
import io.jmix.core.FileStorageLocator
import io.jmix.core.security.SystemAuthenticator
import org.quartz.Job
import org.quartz.JobExecutionContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component


@Component
class Committer(private val dataManager: DataManager): Job {
    @Autowired
    private val systemAuthenticator: SystemAuthenticator? = null

    @Autowired
    private lateinit var fileStorageLocator: FileStorageLocator

    override fun execute(context: JobExecutionContext) {
        systemAuthenticator?.runWithSystem {
            val gitSrv = GitServiceImpl(
                dataManager = dataManager,
                fileStorageLocator = fileStorageLocator,
            )
            gitSrv.createCommit()
//            val gitWorker = GitWorker(
//                dataManager = dataManager,
//                fileStorageLocator = fileStorageLocator,
//            )
//            gitWorker.createCommit()
        }


    }
}