package com.company.commitet_jm.entity

import io.jmix.core.entity.annotation.JmixGeneratedValue
import io.jmix.core.metamodel.annotation.JmixEntity
import jakarta.persistence.*
import java.time.LocalDateTime

@JmixEntity
@Table(name = "CHAT_MESSAGE", indexes = [
    Index(name = "IDX_CHAT_MESSAGE_SESSION", columnList = "SESSION_ID"),
    Index(name = "IDX_CHAT_MESSAGE_PARRENT_MESSAGE", columnList = "PARRENT_MESSAGE_ID")
])
@Entity
open class ChatMessage {
    @JmixGeneratedValue
    @Column(name = "ID", nullable = false)
    @Id
    var id: Long? = null

    @Column(name = "ROLE")
    private var role: String? = null

    @Column(name = "TIMESTAMP_")
    var timestamp: LocalDateTime? = null

    @JoinColumn(name = "SESSION_ID")
        @ManyToOne(fetch = FetchType.LAZY)
        var session: ChatSession? = null

    @Column(name = "GENERATED_")
        var generated: Boolean? = null

    @JoinColumn(name = "PARRENT_MESSAGE_ID")
    @ManyToOne(fetch = FetchType.LAZY)
    var parrentMessage: ChatMessage? = null

    @Column(name = "CONTENT")
    @Lob
    var content: String? = null

    @Column(name = "ERROR")
    var error: String? = null

    fun setRole(role: MessageRole?) {
        this.role = role?.id
    }

    fun getRole(): MessageRole? = role?.let { MessageRole.fromId(it) }

}