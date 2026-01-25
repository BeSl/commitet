package com.company.commitet_jm.entity

import io.jmix.core.entity.annotation.JmixGeneratedValue
import io.jmix.core.metamodel.annotation.JmixEntity
import jakarta.persistence.*
import java.time.LocalDateTime

@JmixEntity
@Table(name = "CHAT_SESSION", indexes = [
    Index(name = "IDX_CHAT_SESSION_USER", columnList = "USER_ID"),
    Index(name = "IDX_CHAT_SESSION_RECIPIENT", columnList = "RECIPIENT_ID"),
    Index(name = "IDX_CHAT_SESSION_CHAT_TYPE", columnList = "CHAT_TYPE")
])
@Entity
open class ChatSession {
    @JmixGeneratedValue
    @Column(name = "ID", nullable = false)
    @Id
    var id: Long? = null

    @JoinColumn(name = "USER_ID")
    @ManyToOne(fetch = FetchType.LAZY)
    var user: User? = null

    @Column(name = "CREATED")
    var created: LocalDateTime? = null

    @Column(name = "BOT_NAME")
    var botName: String? = null

    @JoinColumn(name = "RECIPIENT_ID")
    @ManyToOne(fetch = FetchType.LAZY)
    var recipient: User? = null

    @Column(name = "CHAT_TYPE")
    private var chatType: String? = null

    @Column(name = "TITLE")
    var title: String? = null

    @Column(name = "LAST_MESSAGE_TIME")
    var lastMessageTime: LocalDateTime? = null

    fun getChatType(): ChatType? = chatType?.let { ChatType.fromId(it) }

    fun setChatType(type: ChatType?) {
        this.chatType = type?.id
    }
}