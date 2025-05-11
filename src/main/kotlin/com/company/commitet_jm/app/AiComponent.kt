package com.company.commitet_jm.app

import io.jmix.core.DataManager
import io.jmix.core.security.SystemAuthenticator
import io.jmix.flowui.UiEventPublisher
import org.quartz.Job
import org.quartz.JobExecutionContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class AiComponent(private val dataManager: DataManager,
                  private val uiEventPublisher: UiEventPublisher): Job {
    @Autowired
    private val systemAuthenticator: SystemAuthenticator? = null

    override fun execute(context: JobExecutionContext) {
        systemAuthenticator?.runWithSystem {
            println("job ai started")
            val ai = AiDialogService(dataManager, uiEventPublisher)
            ai.aiAnswerMessage()
        }
    }

}