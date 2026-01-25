package com.company.commitet_jm.service.chat

import com.company.commitet_jm.entity.*
import com.company.commitet_jm.sheduledJob.AiCompanion
import io.jmix.core.DataManager
import io.jmix.flowui.UiEventPublisher
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.util.*

@Component
class ChatHistoryService(
    private val dataManager: DataManager,
    private val uiEventPublisher: UiEventPublisher
) {

    fun createSession(user: User): ChatSession {
        var cs = dataManager.create(ChatSession::class.java)
        cs.user = user
        cs.created = LocalDateTime.now()
        cs.botName = "default"
        dataManager.save(cs)

        return cs

    }

    fun newMessage(session: ChatSession,  content: String) {

        var cm = dataManager.create(ChatMessage::class.java)

        cm.content = content
        cm.timestamp = LocalDateTime.now()
        cm.session = session
        cm.setRole(MessageRole.USER)

        dataManager.save(cm)

        val ai_msg = dataManager.create(ChatMessage::class.java)

        ai_msg.content = "готовлю ответ..."
        ai_msg.timestamp = LocalDateTime.now()
        ai_msg.session = session
        ai_msg.generated = false
        ai_msg.setRole(MessageRole.ASSISTANT)
        ai_msg.parrentMessage = cm

        dataManager.save(ai_msg)

    }

    fun getHistory(session: ChatSession): List<ChatMessage> {
        val chatHistory = dataManager.load(ChatMessage::class.java)
            .query("select apps from ChatMessage apps where apps.session = :pSession order by apps.id desc")
            .parameter("pSession", session)
            .maxResults(5)
            .list().reversed()

        return chatHistory
    }

    fun getUserSessions(user: User): ChatSession {
        val ses = dataManager.load(ChatSession::class.java)
            .query("select apps from ChatSession apps where apps.user = :pUser ")
            .parameter("pUser", user)
            .optional()
        if (ses.isEmpty){
            return createSession(user)
        }
        val ch = ses.get()
        if (ch.botName.isNullOrBlank()) {
            ch.botName = "myBot"
            dataManager.save(ch)
        }

        return ch
    }

    fun messageToResponse():ChatMessage? {
        val resp = dataManager.load(ChatMessage::class.java)
            .query("select apps from ChatMessage apps where apps.generated = :pG and apps.parrentMessage <> :pP  order by apps.timestamp")
            .parameter("pP", null)
            .parameter("pG", false)
            .optional()

        if (resp.isEmpty) {
            return null
        }
        return resp.get()

    }

    fun saveResponse(message: ChatMessage) {

        dataManager.save(message)

        uiEventPublisher.publishEventForUsers(
            AiCompanion(this),
            Collections.singleton(message.session?.user?.getUsername())
        )
    }

}