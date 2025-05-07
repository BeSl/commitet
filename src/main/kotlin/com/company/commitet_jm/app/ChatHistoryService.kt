package com.company.commitet_jm.app

import com.company.commitet_jm.entity.*
import io.jmix.core.DataManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import kotlin.jvm.optionals.getOrNull

@Service
class ChatHistoryService(
    private val dataManager: DataManager,
) {

    fun createSession(user: User): ChatSession {
        var cs = dataManager.create(ChatSession::class.java)
        cs.user = user
        cs.created = LocalDateTime.now()
        dataManager.save(cs)

        return cs

    }

    fun addMessage(sessionId: Long, content: String, role: MessageRole, metadata: Map<String, Any> = emptyMap()) {
//        val session = sessionRepository.findById(sessionId).orElseThrow()
//        val message = ChatMessage(
//            content = content,
//            role = role,
//            session = session,
//            metadata = metadata
//        )
//        messageRepository.save(message)
    }

    fun getHistory(session: ChatSession): List<ChatMessage> {
        val chatHistory = dataManager.load(ChatMessage::class.java)
            .query("select apps from ChatMessage apps where apps.session = :pSession order by apps.id desc")
            .parameter("pSession", session)
            .maxResults(20)
            .list()

        return chatHistory
    }

    fun getUserSessions(user: User): ChatSession {
        val ses = dataManager.load(ChatSession::class.java)
            .query("select apps from ChatSession apps where apps.user = :pUser")
            .parameter("pUser", user)
            .optional()
        if (ses.isEmpty){
            return createSession(user)
        }
        return ses.get()
    }
}