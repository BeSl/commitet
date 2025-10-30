package com.company.commitet_jm.service;

import com.company.commitet_jm.entity.*;
import com.company.commitet_jm.sheduledJob.AiCompanion;
import io.jmix.core.DataManager;
import io.jmix.flowui.UiEventPublisher;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Component
public class ChatHistoryService {

    private final DataManager dataManager;
    private final UiEventPublisher uiEventPublisher;

    public ChatHistoryService(DataManager dataManager, UiEventPublisher uiEventPublisher) {
        this.dataManager = dataManager;
        this.uiEventPublisher = uiEventPublisher;
    }

    public ChatSession createSession(User user) {
        ChatSession cs = dataManager.create(ChatSession.class);
        cs.setUser(user);
        cs.setCreated(LocalDateTime.now());
        cs.setBotName("default");
        dataManager.save(cs);

        return cs;
    }

    public void newMessage(ChatSession session, String content) {
        ChatMessage cm = dataManager.create(ChatMessage.class);

        cm.setContent(content);
        cm.setTimestamp(LocalDateTime.now());
        cm.setSession(session);
        cm.setRole(MessageRole.USER);

        dataManager.save(cm);

        ChatMessage ai_msg = dataManager.create(ChatMessage.class);

        ai_msg.setContent("готовлю ответ...");
        ai_msg.setTimestamp(LocalDateTime.now());
        ai_msg.setSession(session);
        ai_msg.setGenerated(false);
        ai_msg.setRole(MessageRole.ASSISTANT);
        ai_msg.setParrentMessage(cm);

        dataManager.save(ai_msg);
    }

    public List<ChatMessage> getHistory(ChatSession session) {
        List<ChatMessage> chatHistory = dataManager.load(ChatMessage.class)
                .query("select apps from ChatMessage apps where apps.session = :pSession order by apps.id desc")
                .parameter("pSession", session)
                .maxResults(5)
                .list();
        
        // Reverse the list to get chronological order
        Collections.reverse(chatHistory);

        return chatHistory;
    }

    public ChatSession getUserSessions(User user) {
        Optional<ChatSession> ses = dataManager.load(ChatSession.class)
                .query("select apps from ChatSession apps where apps.user = :pUser ")
                .parameter("pUser", user)
                .optional();
        
        if (!ses.isPresent()) {
            return createSession(user);
        }
        
        ChatSession ch = ses.get();
        if (ch.getBotName() == null || ch.getBotName().isBlank()) {
            ch.setBotName("myBot");
            dataManager.save(ch);
        }

        return ch;
    }

    public ChatMessage messageToResponse() {
        Optional<ChatMessage> resp = dataManager.load(ChatMessage.class)
                .query("select apps from ChatMessage apps where apps.generated = :pG and apps.parrentMessage <> :pP  order by apps.timestamp")
                .parameter("pP", null)
                .parameter("pG", false)
                .optional();

        if (!resp.isPresent()) {
            return null;
        }
        
        return resp.get();
    }

    public void saveResponse(ChatMessage message) {
        dataManager.save(message);

        uiEventPublisher.publishEventForUsers(
                new AiCompanion(this),
                Collections.singleton(message.getSession().getUser().getUsername())
        );
    }
}