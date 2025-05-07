package com.company.commitet_jm.entity

import io.jmix.core.entity.annotation.JmixGeneratedValue
import io.jmix.core.metamodel.annotation.JmixEntity
import jakarta.persistence.*
import java.time.LocalDateTime

@JmixEntity
@Table(name = "CHAT_SESSION", indexes = [
    Index(name = "IDX_CHAT_SESSION_USER", columnList = "USER_ID")
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
}