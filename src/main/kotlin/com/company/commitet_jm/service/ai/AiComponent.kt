package com.company.commitet_jm.service.ai

import com.company.commitet_jm.service.ChatHistoryService
import io.jmix.core.DataManager
import io.jmix.core.security.SystemAuthenticator
import io.jmix.flowui.UiEventPublisher
import org.quartz.Job
import org.quartz.JobExecutionContext
import org.springframework.ai.chat.model.ChatModel
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

@Component
class AiComponent(
    @Qualifier("chatHistoryService")
    private val history: ChatHistoryService,
    private val chatModel: ChatModel
): Job {

    @Autowired
    private val systemAuthenticator: SystemAuthenticator? = null

    override fun execute(context: JobExecutionContext) {
        systemAuthenticator?.runWithSystem {
            val ai = AiDialogService(history , chatModel)
            ai.aiAnswerMessage()
        }
    }

}