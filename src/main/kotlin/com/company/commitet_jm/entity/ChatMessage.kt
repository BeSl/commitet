package com.company.commitet_jm.entity

import io.jmix.core.entity.annotation.JmixGeneratedValue
import io.jmix.core.metamodel.annotation.JmixEntity
import jakarta.persistence.*
import java.time.LocalDateTime

@JmixEntity
@Table(name = "CHAT_MESSAGE", indexes = [
    Index(name = "IDX_CHAT_MESSAGE_SESSION", columnList = "SESSION_ID")
])
@Entity
open class ChatMessage {
    @JmixGeneratedValue
    @Column(name = "ID", nullable = false)
    @Id
    var id: Long? = null

    @Column(name = "CONTENT")
    @Lob
    var content: String? = null

    @Column(name = "ROLE", length = 10)
    var role: String? = null

    @Column(name = "TIMESTAMP_")
    var timestamp: LocalDateTime? = null

    @JoinColumn(name = "SESSION_ID")
    @ManyToOne(fetch = FetchType.LAZY)
    var session: ChatSession? = null

    fun getRole(): MessageRole? = role?.let { MessageRole.fromId(it) }

    fun setRole(role: MessageRole?) {
        this.role = role?.id
    }
}