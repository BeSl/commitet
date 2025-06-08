package com.company.commitet_jm.service.ai


import com.company.commitet_jm.entity.ChatSession
import com.company.commitet_jm.service.ChatHistoryService
import io.jmix.core.DataManager
import io.jmix.flowui.UiEventPublisher
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor
import org.springframework.ai.chat.memory.ChatMemory
import org.springframework.ai.chat.memory.MessageWindowChatMemory
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.chat.prompt.ChatOptions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component


@Component
class AiDialogService(
    private val history : ChatHistoryService,
    private val chatModel: ChatModel
) {

  fun aiAnswerMessage(){
        val req = history.messageToResponse()
        if (req==null){
            return
        }

        req?.content = "DONE!!!"
        val id_chat = req.session!!.user!!.getUsername()

        val chatMemory = loadMemory(req.session, id_chat!!)

        val chatClient: ChatClient = ChatClient.builder(chatModel)
            .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build()

        req.content =
                chatClient.prompt("ты помощник разработчика отвечай на вопросы программирования") // Set advisor parameters at runtime
                    .advisors { advisor ->
                        advisor
                            .param(ChatMemory.CONVERSATION_ID, id_chat)
                            .param("chat_memory_response_size", 5)
                    }
                    .user(req.parrentMessage!!.content)
                    .options(
                        ChatOptions.builder()
                        .temperature(0.5)  // Very deterministic output
                        .build())
                    .call()
                    .content()?.toString()

            req.generated = true
            history.saveResponse(req)
        }

    private fun loadMemory(ses: ChatSession?, id:String): ChatMemory {
        var chatM: ChatMemory = MessageWindowChatMemory.builder()
            .build()

        if (ses!=null) {
            val msg = history.getHistory(ses)
            for (ms in msg) {
                chatM.add(id, UserMessage(ms.content))
            }
        }

        return chatM
    }

}