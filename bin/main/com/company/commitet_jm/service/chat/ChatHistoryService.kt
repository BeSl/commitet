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
        cs.setChatType(ChatType.LLM)
        cs.lastMessageTime = LocalDateTime.now()
        dataManager.save(cs)

        return cs
    }

    fun newMessage(session: ChatSession, content: String) {
        var cm = dataManager.create(ChatMessage::class.java)

        cm.content = content
        cm.timestamp = LocalDateTime.now()
        cm.session = session
        cm.setRole(MessageRole.USER)

        dataManager.save(cm)

        // Обновляем время последнего сообщения в сессии
        session.lastMessageTime = LocalDateTime.now()
        dataManager.save(session)

        // Для LLM чатов создаём заглушку для ответа AI
        if (session.getChatType() == ChatType.LLM) {
            val ai_msg = dataManager.create(ChatMessage::class.java)

            ai_msg.content = "готовлю ответ..."
            ai_msg.timestamp = LocalDateTime.now()
            ai_msg.session = session
            ai_msg.generated = false
            ai_msg.setRole(MessageRole.ASSISTANT)
            ai_msg.parrentMessage = cm

            dataManager.save(ai_msg)
        }
    }

    fun getHistory(session: ChatSession): List<ChatMessage> {
        val chatHistory = dataManager.load(ChatMessage::class.java)
            .query("select apps from ChatMessage apps where apps.session = :pSession order by apps.id desc")
            .parameter("pSession", session)
            .maxResults(5)
            .list().reversed()

        return chatHistory
    }

    fun getChatHistory(session: ChatSession, limit: Int = 50): List<ChatMessage> {
        return dataManager.load(ChatMessage::class.java)
            .query("select m from ChatMessage m where m.session = :session order by m.id desc")
            .parameter("session", session)
            .maxResults(limit)
            .list().reversed()
    }

    fun getUserSessions(user: User): ChatSession {
        val ses = dataManager.load(ChatSession::class.java)
            .query("select apps from ChatSession apps where apps.user = :pUser and (apps.chatType = :pType or apps.chatType is null)")
            .parameter("pUser", user)
            .parameter("pType", ChatType.LLM.id)
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

    fun getUserChats(user: User): List<ChatSession> {
        return dataManager.load(ChatSession::class.java)
            .query("""
                select cs from ChatSession cs
                where cs.user = :user or cs.recipient = :user
                order by cs.lastMessageTime desc nulls last
            """)
            .parameter("user", user)
            .list()
    }

    fun getOrCreateLlmChat(user: User): ChatSession {
        val existing = dataManager.load(ChatSession::class.java)
            .query("select cs from ChatSession cs where cs.user = :user and cs.chatType = :chatType")
            .parameter("user", user)
            .parameter("chatType", ChatType.LLM.id)
            .optional()

        if (existing.isPresent) {
            return existing.get()
        }

        val session = dataManager.create(ChatSession::class.java)
        session.user = user
        session.created = LocalDateTime.now()
        session.setChatType(ChatType.LLM)
        session.botName = "AI Assistant"
        session.title = "AI Assistant"
        session.lastMessageTime = LocalDateTime.now()
        return dataManager.save(session)
    }

    fun getOrCreateUserChat(user1: User, user2: User): ChatSession {
        // Ищем существующий чат между двумя пользователями (в любом направлении)
        val existing = dataManager.load(ChatSession::class.java)
            .query("""
                select cs from ChatSession cs
                where cs.chatType = :chatType
                and ((cs.user = :user1 and cs.recipient = :user2)
                     or (cs.user = :user2 and cs.recipient = :user1))
            """)
            .parameter("chatType", ChatType.USER.id)
            .parameter("user1", user1)
            .parameter("user2", user2)
            .optional()

        if (existing.isPresent) {
            return existing.get()
        }

        // Создаём новый чат
        val session = dataManager.create(ChatSession::class.java)
        session.user = user1
        session.recipient = user2
        session.created = LocalDateTime.now()
        session.setChatType(ChatType.USER)
        session.title = user2.displayName
        session.lastMessageTime = LocalDateTime.now()
        return dataManager.save(session)
    }

    fun sendMessage(session: ChatSession, sender: User, content: String): ChatMessage {
        val message = dataManager.create(ChatMessage::class.java)
        message.content = content
        message.timestamp = LocalDateTime.now()
        message.session = session
        message.setRole(MessageRole.USER)
        message.generated = true

        dataManager.save(message)

        // Обновляем время последнего сообщения
        session.lastMessageTime = LocalDateTime.now()
        dataManager.save(session)

        // Для LLM чатов создаём заглушку для ответа
        if (session.getChatType() == ChatType.LLM) {
            val aiMsg = dataManager.create(ChatMessage::class.java)
            aiMsg.content = "готовлю ответ..."
            aiMsg.timestamp = LocalDateTime.now()
            aiMsg.session = session
            aiMsg.generated = false
            aiMsg.setRole(MessageRole.ASSISTANT)
            aiMsg.parrentMessage = message
            dataManager.save(aiMsg)
        }

        return message
    }

    fun getAvailableRecipients(currentUser: User): List<User> {
        return dataManager.load(User::class.java)
            .query("select u from User u where u.llmAvailable = true and u <> :currentUser and u.active = true")
            .parameter("currentUser", currentUser)
            .list()
    }

    fun getChatTitle(session: ChatSession, currentUser: User): String {
        return when (session.getChatType()) {
            ChatType.LLM -> session.title ?: "AI Assistant"
            ChatType.USER -> {
                val otherUser = if (session.user == currentUser) session.recipient else session.user
                otherUser?.displayName ?: session.title ?: "Чат"
            }
            null -> session.botName ?: "Чат"
        }
    }

    fun messageToResponse(): ChatMessage? {
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