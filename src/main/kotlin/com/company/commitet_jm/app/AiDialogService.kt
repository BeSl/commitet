package com.company.commitet_jm.app

import io.jmix.core.DataManager
import io.jmix.core.FileStorageLocator
import io.jmix.core.security.SystemAuthenticator
import io.jmix.flowui.UiEventPublisher
import kotlinx.coroutines.delay
//import org.springframework.ai.chat.client.ChatClient
//import org.springframework.ai.chat.model.ChatModel
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service



@Component
class AiDialogService(private val dataManager: DataManager,
                      private val uiEventPublisher: UiEventPublisher) {


//    @Autowired
//    private lateinit var chatModel: ChatModel

    fun aiAnswerMessage(){
        val history = ChatHistoryService(dataManager,uiEventPublisher)
        val req = history.messageToResponse()
        //val msg = getQueueMessage(history)

        req?.content = "DONE!!!"

        if (req != null) {
            req.generated = true
            history.saveResponse(req)
        }

//        history.saveResponse(msg)
//        val chatClient: ChatClient = ChatClient.builder(chatModel)
//
//            .build()
//
//        val response: String = chatClient.prompt() // Set advisor parameters at runtime
//            .advisors { advisor ->
//                advisor.param("chat_memory_conversation_id", "678")
//                    .param("chat_memory_response_size", 100)
//            }
//            .user(msg)
//            .call()
//            .content()?.toString() ?:"no connection"
//
//        saveResponse(response)
    }

//    private fun saveResponse(response: String) {
//        TODO("Not yet implemented")
//    }

    private fun getQueueMessage(history: ChatHistoryService): String{
        val request = history.messageToResponse()

        return request?.content ?:"no request"
    }

}