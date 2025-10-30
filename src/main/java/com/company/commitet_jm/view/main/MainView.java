package com.company.commitet_jm.view.main;

import com.company.commitet_jm.service.ChatHistoryService;
import com.company.commitet_jm.entity.ChatMessage;
import com.company.commitet_jm.entity.ChatSession;
import com.company.commitet_jm.entity.MessageRole;
import com.company.commitet_jm.entity.User;
import com.company.commitet_jm.entity.Commit;
import com.company.commitet_jm.sheduledJob.AiCompanion;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.messages.MessageInput;
import com.vaadin.flow.component.messages.MessageInput.SubmitEvent;
import com.vaadin.flow.component.messages.MessageList;
import com.vaadin.flow.component.messages.MessageListItem;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import io.jmix.core.DataManager;
import io.jmix.core.security.CurrentAuthentication;
import io.jmix.flowui.UiEventPublisher;
import io.jmix.flowui.app.main.StandardMainView;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.view.Subscribe;
import io.jmix.flowui.view.ViewComponent;
import io.jmix.flowui.view.ViewController;
import io.jmix.flowui.view.ViewDescriptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.event.EventListener;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Route("")
@ViewController("MainView")
@ViewDescriptor("main-view.xml")
public class MainView extends StandardMainView {

    @Autowired
    private CurrentAuthentication currentAuthentication;

    @Autowired
    private DataManager dataManager;

    @ViewComponent
    private H2 welcomeMessage;

    @ViewComponent
    private Span appVersion;

    @ViewComponent
    private VerticalLayout boxV;

    @ViewComponent
    private VerticalLayout vbox;

    @ViewComponent
    private Scroller scroll4at;

    @ViewComponent
    private H2 weekCommitCount;

    @ViewComponent
    private H2 monthCommitCount;

    @Autowired
    private BuildProperties buildProperties;

    @Autowired
    private ChatHistoryService chatHistory;

    private List<MessageListItem> messageListItems;

    @ViewComponent
    private MessageList msList;

    @Autowired
    private UiEventPublisher uiEventPublisher;

    @Subscribe
    private void onInit(InitEvent event) {
        User currentUser = (User) currentAuthentication.getUser();

        welcomeMessage.setText(currentUser.getFirstName() + ", привет!!! ");
        appVersion.setText("Версия сборки " + (buildProperties != null ? buildProperties.getVersion() : ""));

        if (currentUser.getLlmAvailable() == Boolean.TRUE) {
            addChat(currentUser);
            boxV.setVisible(true);
        }

        // Load and display commit statistics
        loadCommitStatistics();
    }

    @Subscribe("AddEvent")
    public void onCancelButtonClick(ClickEvent<JmixButton> event) {
        if (uiEventPublisher != null) {
            uiEventPublisher.publishEventForUsers(
                new AiCompanion(this),
                Collections.singleton(((User) currentAuthentication.getUser()).getUsername())
            );
        }
    }

    private void loadCommitStatistics() {
        long weekCount = countCommitsLastWeek();
        long monthCount = countCommitsCurrentMonth();

        weekCommitCount.setText(String.valueOf(weekCount));
        monthCommitCount.setText(String.valueOf(monthCount));
    }

    private long countCommitsLastWeek() {
        LocalDateTime oneWeekAgo = LocalDateTime.now().minusDays(7);

        Long result = dataManager.loadValue(
            "select count(c) from Commit_ c where c.dateCreated >= :startDate and c.author = :author",
            Long.class
        )
            .parameter("startDate", oneWeekAgo)
            .parameter("author", currentAuthentication.getUser())
            .one();

        return result != null ? result : 0L;
    }

    private long countCommitsCurrentMonth() {
        LocalDateTime firstDayOfMonth = LocalDateTime.now().withDayOfMonth(1).toLocalDate().atStartOfDay();
        
        Long result = dataManager.loadValue(
            "select count(c) from Commit_ c where c.dateCreated >= :startDate and c.author = :author",
            Long.class
        )
            .parameter("startDate", firstDayOfMonth)
            .parameter("author", currentAuthentication.getUser())
            .one();

        return result != null ? result : 0L;
    }

    private void updateMessageList(ChatSession session) {
        messageListItems = chatHistory
            .getHistory(session)
            .stream()
            .map(this::mapToMessageListItem)
            .collect(java.util.stream.Collectors.toList());

        msList.setItems(messageListItems);
    }

    private void addChat(User user) {
        ChatSession chatSession = chatHistory.getUserSessions(user);

        List<ChatMessage> listMessage = chatHistory.getHistory(chatSession);

        messageListItems = listMessage.stream()
            .map(this::mapToMessageListItem)
            .collect(java.util.stream.Collectors.toList());
        msList = new MessageList();
        msList.setItems(messageListItems);
        vbox.add(msList);
//        boxV.add(msList);

        MessageInput input = new MessageInput();
        input.addSubmitListener((MessageInput.SubmitListener) submitEvent -> {
            eventNewQuery(submitEvent, chatSession);
        });
        vbox.add(input);
//        boxV.add(input);
    }

    private MessageListItem mapToMessageListItem(ChatMessage chatMessage) {
        MessageListItem mli = new MessageListItem(
            chatMessage.getContent(),
            chatMessage.getTimestamp().toInstant(ZoneOffset.ofHours(3)),
            getDisplayName(chatMessage)
        );
        if (chatMessage.getRoleEnum() == MessageRole.ASSISTANT) {
            mli.setUserColorIndex(1);
        } else {
            mli.setUserColorIndex(2);
        }

        return mli;
    }

    private void eventNewQuery(SubmitEvent submitEvent, ChatSession session) {
        if (submitEvent.getValue().isEmpty()) {
            return;
        }

        chatHistory.newMessage(session, submitEvent.getValue());

        messageListItems = chatHistory
                .getHistory(session)
                .stream()
                .map(this::mapToMessageListItem)
                .collect(java.util.stream.Collectors.toList());

        msList.setItems(messageListItems);
    }

    private String getDisplayName(ChatMessage message) {
        ChatSession session = message.getSession();

        MessageRole role = message.getRoleEnum();
        if (role == MessageRole.USER) {
            return session != null && session.getUser() != null ? session.getUser().getFirstName() : null;
        } else if (role == MessageRole.ASSISTANT) {
            return session != null ? session.getBotName() : null;
        } else if (role == MessageRole.SYSTEM) {
            return "System";
        } else {
            return "none";
        }
    }

    @EventListener
    private void addNewMessage(AiCompanion event) {
//        chatHistory.newMessage(session = chatHistory.getUserSessions(currentAuthentication.user as User), "PODPISKA")
        updateMessageList(chatHistory.getUserSessions((User) currentAuthentication.getUser()));
    }

    private String formatRelativeDate(LocalDateTime timestamp) {
        java.time.LocalDate today = LocalDateTime.now().toLocalDate();
        java.time.LocalDate messageDate = timestamp.toLocalDate();

        long daysDiff = ChronoUnit.DAYS.between(messageDate, today);
        if (daysDiff == 0L) {
            return "Today";
        } else if (daysDiff == 1L) {
            return "Yesterday";
        } else if (daysDiff < 7) {
            return daysDiff + " days ago";
        } else {
            return messageDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"));
        }
    }
}