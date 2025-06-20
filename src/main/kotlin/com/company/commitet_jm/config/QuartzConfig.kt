package com.company.commitet_jm.config

import com.company.commitet_jm.service.ai.AiComponent
import com.company.commitet_jm.sheduledJob.Committer
import org.quartz.*
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
open class QuartzConfig {

    @Bean
    open fun commitJobDetail(): JobDetail {
        return JobBuilder.newJob(Committer::class.java)
            .withIdentity("commitJob")
            .storeDurably()
            .build()
    }

    @Bean
    open fun commitTrigger(commitJobDetail: JobDetail): Trigger {
        return TriggerBuilder.newTrigger()
            .forJob(commitJobDetail)
            .withIdentity("commitTrigger")
            .startNow()
            .withSchedule(
                SimpleScheduleBuilder.simpleSchedule()
                    .withIntervalInSeconds(5)
                    .repeatForever()
            )
            .build()
    }

    @Bean
    open fun aiJobDetail(): JobDetail {
        return JobBuilder.newJob(AiComponent::class.java)
            .withIdentity("AIDialog")
            .storeDurably()
            .build()
    }

    @Bean
    open fun aiTrigger(aiJobDetail: JobDetail): Trigger {
        return TriggerBuilder.newTrigger()
            .forJob(aiJobDetail)
            .withIdentity("AITrigger")
            .startNow()
            .withSchedule(
                SimpleScheduleBuilder.simpleSchedule()
                    .withIntervalInSeconds(5)
                    .repeatForever()
            )
            .build()
    }
}