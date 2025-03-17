package com.company.commitet_jm.sheduledJob

import com.company.commitet_jm.service.GitWorker
import org.quartz.Job
import org.quartz.JobExecutionContext
import org.springframework.stereotype.Component

@Component
class Committer: Job {
    override fun execute(context: JobExecutionContext) {
        val gitWorker = GitWorker()
        gitWorker.CreateCommit()
    }
}