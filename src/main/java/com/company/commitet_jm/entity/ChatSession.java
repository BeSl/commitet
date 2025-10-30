package com.company.commitet_jm.entity;

import io.jmix.core.entity.annotation.JmixGeneratedValue;
import io.jmix.core.metamodel.annotation.JmixEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@JmixEntity
@Table(name = "CHAT_SESSION", indexes = {
    @Index(name = "IDX_CHAT_SESSION_USER", columnList = "USER_ID")
})
@Entity
public class ChatSession {
    @JmixGeneratedValue
    @Column(name = "ID", nullable = false)
    @Id
    private Long id;

    @JoinColumn(name = "USER_ID")
    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    @Column(name = "CREATED")
    private LocalDateTime created;

    @Column(name = "BOT_NAME")
    private String botName;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public LocalDateTime getCreated() {
        return created;
    }

    public void setCreated(LocalDateTime created) {
        this.created = created;
    }

    public String getBotName() {
        return botName;
    }

    public void setBotName(String botName) {
        this.botName = botName;
    }
}