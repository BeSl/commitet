package com.company.commitet_jm.service.ai

import io.jmix.core.DataManager
import io.jmix.core.security.SystemAuthenticator
import io.jmix.flowui.UiEventPublisher
import org.quartz.Job
import org.quartz.JobExecutionContext
import org.springframework.ai.chat.model.ChatModel
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class AiComponent(private val dataManager: DataManager,
                  private val uiEventPublisher: UiEventPublisher,
                  private val chatModel: ChatModel
): Job {
    @Autowired
    private val systemAuthenticator: SystemAuthenticator? = null



    override fun execute(context: JobExecutionContext) {
        systemAuthenticator?.runWithSystem {
            val ai = AiDialogService(dataManager, uiEventPublisher, chatModel)
            ai.aiAnswerMessage()
        }
    }

}