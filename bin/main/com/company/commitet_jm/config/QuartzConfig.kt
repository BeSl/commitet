package com.company.commitet_jm.config

import com.company.commitet_jm.sheduledJob.Committer
import org.quartz.*
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
open class QuartzConfig {

    @Bean
    open fun jobDetail(): JobDetail {
        return JobBuilder.newJob(Committer::class.java)
            .withIdentity("commitJob")
            .storeDurably()
            .build()
    }

    @Bean
    open fun trigger(jobDetail: JobDetail): Trigger {
        return TriggerBuilder.newTrigger()
            .forJob(jobDetail)
            .withIdentity("commitTrigger")
            .startNow()
            .withSchedule(
                SimpleScheduleBuilder.simpleSchedule()
                    .withIntervalInSeconds(5)
                    .repeatForever()
            )
            .build()
    }
}