package com.company.commitet_jm.service.ai

import com.company.commitet_jm.service.ChatHistoryService
import io.jmix.core.DataManager
import io.jmix.flowui.UiEventPublisher
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.model.ChatModel


import org.springframework.stereotype.Component


@Component
class AiDialogService(private val dataManager: DataManager,
                      private val uiEventPublisher: UiEventPublisher,
                      private val chatModel: ChatModel) {


    fun aiAnswerMessage(){
        val history = ChatHistoryService(dataManager,uiEventPublisher)
        val req = history.messageToResponse()
//        val msg = getQueueMessage(history)

        req?.content = "DONE!!!"

        if (req != null) {
            val chatClient: ChatClient = ChatClient.builder(chatModel)
                .build()
//
            req.content = req.parrentMessage!!.content?.let {
                chatClient.prompt("ты помощник разработчика отвечай в рамках проекта") // Set advisor parameters at runtime
//                    .advisors { advisor ->
//                        advisor.param("chat_memory_conversation_id", "678")
//                            .param("chat_memory_response_size", 100)
//                    }

                    .user(it)
//                    .options(ChatOptions.builder()
//                        .temperature(0.1)  // Very deterministic output
//                        .build())
                    .call()
                    .content()?.toString()
            } ?:"no connection"

            req.generated = true
            history.saveResponse(req)
        }

    }

}