package com.company.commitet_jm.batch

import org.quartz.Job
import org.quartz.JobExecutionContext
import org.springframework.stereotype.Component

@Component
class SampleJob : Job {
    override fun execute(context: JobExecutionContext) {
        println("Задача выполняется: ${System.currentTimeMillis()}")
    }
}