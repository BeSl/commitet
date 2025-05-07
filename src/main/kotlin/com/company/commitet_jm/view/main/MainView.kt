package com.company.commitet_jm.view.main

import com.company.commitet_jm.app.ChatHistoryService
import com.company.commitet_jm.entity.ChatMessage
import com.company.commitet_jm.entity.User
import com.vaadin.flow.component.html.H2
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.messages.MessageInput
import com.vaadin.flow.component.messages.MessageInput.SubmitEvent
import com.vaadin.flow.component.messages.MessageList
import com.vaadin.flow.component.messages.MessageListItem
import com.vaadin.flow.component.notification.Notification
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.router.Route
import io.jmix.core.DataManager
import io.jmix.core.security.CurrentAuthentication
import io.jmix.flowui.app.main.StandardMainView
import io.jmix.flowui.view.Subscribe
import io.jmix.flowui.view.ViewComponent
import io.jmix.flowui.view.ViewController
import io.jmix.flowui.view.ViewDescriptor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.info.BuildProperties
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit


@Route("")
@ViewController(id = "MainView")
@ViewDescriptor(path = "main-view.xml")
open class MainView : StandardMainView() {
    @Autowired
    private lateinit var currentAuthentication: CurrentAuthentication

    @ViewComponent
    private lateinit var welcomeMessage: H2

    @ViewComponent
    private lateinit var appVersion:Span

    @ViewComponent
    private lateinit var boxV: VerticalLayout

    @Autowired
    private val buildProperties: BuildProperties? = null

    @Autowired
    private lateinit var chatHistory: ChatHistoryService

    @Autowired
    private lateinit var dataManager: DataManager

    @Subscribe
    private fun onInit(event: InitEvent) {
        val currentUser = currentAuthentication.user as User

        welcomeMessage.text = "${currentUser.firstName}, привет!!! "
        appVersion.text = "Версия сборки ${buildProperties?.version}"

        addChat(user = currentUser)

    }
    private fun addChat(user: User){
        val person = user

        val chatSession = chatHistory.getUserSessions(user)

        val listMessage = chatHistory.getHistory(chatSession)

        val messageListItems = listMessage.map { mapToMessageListItem(it) }

        val list = MessageList()
        val yesterday: Instant = LocalDateTime.now().minusDays(1)
            .toInstant(ZoneOffset.UTC)
        val fiftyMinsAgo: Instant = LocalDateTime.now().minusMinutes(50)
            .toInstant(ZoneOffset.UTC)
        val message1 = MessageListItem(
            "Linsey, could you check if the details with the order are okay?",
            yesterday, "Matt Mambo"
        )

//        addMessage(boxV, )

        message1.userColorIndex = 1
        val message2 = MessageListItem(
            "All good. Ship it.",
            fiftyMinsAgo, "Linsey Listy",
        )
        message2.userColorIndex = 2
        list.setItems(listOf(message1, message2))
        boxV.add(list)
//        boxV.add(messageListItems)

        val input = MessageInput()
        input.addSubmitListener { submitEvent: SubmitEvent ->
            Notification.show(
                "Message received: " + submitEvent.value,
                3000, Notification.Position.MIDDLE
            )
        }
        boxV.add(input)
    }

    fun mapToMessageListItem(chatMessage: ChatMessage): MessageListItem {
        return MessageListItem(
            chatMessage.content,
//            Instant.now().minus(1, ChronoUnit.DAYS),
            Instant.from(chatMessage.timestamp),
            getDisplayName(chatMessage)
        )
    }
    private fun getDisplayName(message: ChatMessage): String? {
        // 1. Проверка метаданных
//        message.metadata["displayName"]?.let { return it.toString() }

        // 2. Логика на основе роли
        return message.session?.user?.firstName
//        when (message.role) {
//            MessageRole.USER -> message.session.userId // или получить из UserService
//            MessageRole.ASSISTANT -> message.session.modelName
//            MessageRole.SYSTEM -> "System"
//        }
    }
    private fun formatRelativeDate(timestamp: LocalDateTime): String {
        val today = LocalDateTime.now().toLocalDate()
        val messageDate = timestamp.toLocalDate()

        return when (val daysDiff = ChronoUnit.DAYS.between(messageDate, today)) {
            0L -> "Today"
            1L -> "Yesterday"
            else -> if (daysDiff < 7) "$daysDiff days ago"
            else messageDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
        }
    }
}
