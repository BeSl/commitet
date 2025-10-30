package com.company.commitet_jm.config;

import com.company.commitet_jm.sheduledJob.Committer;
import org.quartz.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QuartzConfig {

    @Bean
    public JobDetail commitJobDetail() {
        return JobBuilder.newJob(Committer.class)
                .withIdentity("commitJob")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger commitTrigger(JobDetail commitJobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(commitJobDetail)
                .withIdentity("commitTrigger")
                .startNow()
                .withSchedule(
                        SimpleScheduleBuilder.simpleSchedule()
                                .withIntervalInSeconds(5)
                                .repeatForever()
                )
                .build();
    }

}