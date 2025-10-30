package com.company.commitet_jm.entity;

import io.jmix.core.entity.annotation.JmixGeneratedValue;
import io.jmix.core.metamodel.annotation.JmixEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@JmixEntity
@Table(name = "CHAT_MESSAGE", indexes = {
    @Index(name = "IDX_CHAT_MESSAGE_SESSION", columnList = "SESSION_ID"),
    @Index(name = "IDX_CHAT_MESSAGE_PARRENT_MESSAGE", columnList = "PARRENT_MESSAGE_ID")
})
@Entity
public class ChatMessage {
    @JmixGeneratedValue
    @Column(name = "ID", nullable = false)
    @Id
    private Long id;

    @Column(name = "ROLE")
    private String role;

    @Column(name = "TIMESTAMP_")
    private LocalDateTime timestamp;

    @JoinColumn(name = "SESSION_ID")
    @ManyToOne(fetch = FetchType.LAZY)
    private ChatSession session;

    @Column(name = "GENERATED_")
    private Boolean generated;

    @JoinColumn(name = "PARRENT_MESSAGE_ID")
    @ManyToOne(fetch = FetchType.LAZY)
    private ChatMessage parrentMessage;

    @Column(name = "CONTENT")
    @Lob
    private String content;

    @Column(name = "ERROR")
    private String error;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public ChatSession getSession() {
        return session;
    }

    public void setSession(ChatSession session) {
        this.session = session;
    }

    public Boolean getGenerated() {
        return generated;
    }

    public void setGenerated(Boolean generated) {
        this.generated = generated;
    }

    public ChatMessage getParrentMessage() {
        return parrentMessage;
    }

    public void setParrentMessage(ChatMessage parrentMessage) {
        this.parrentMessage = parrentMessage;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public void setRole(MessageRole role) {
        this.role = role != null ? role.getId() : null;
    }

    public MessageRole getRoleEnum() {
        return role != null ? MessageRole.fromId(role) : null;
    }
}