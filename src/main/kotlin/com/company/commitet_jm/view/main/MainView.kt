package com.company.commitet_jm.view.main

import com.company.commitet_jm.service.chat.ChatHistoryService
import com.company.commitet_jm.entity.ChatMessage
import com.company.commitet_jm.entity.ChatSession
import com.company.commitet_jm.entity.MessageRole
import com.company.commitet_jm.entity.User
import com.company.commitet_jm.entity.Commit
import com.company.commitet_jm.sheduledJob.AiCompanion
import com.vaadin.flow.component.ClickEvent
import com.vaadin.flow.component.html.H2
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.messages.MessageInput
import com.vaadin.flow.component.messages.MessageInput.SubmitEvent
import com.vaadin.flow.component.messages.MessageList
import com.vaadin.flow.component.messages.MessageListItem
import com.vaadin.flow.component.orderedlayout.Scroller
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.router.Route
import io.jmix.core.DataManager
import io.jmix.core.security.CurrentAuthentication
import io.jmix.flowui.UiEventPublisher
import io.jmix.flowui.app.main.StandardMainView
import io.jmix.flowui.kit.component.button.JmixButton
import io.jmix.flowui.view.Subscribe
import io.jmix.flowui.view.ViewComponent
import io.jmix.flowui.view.ViewController
import io.jmix.flowui.view.ViewDescriptor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.info.BuildProperties
import org.springframework.context.event.EventListener
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.*

@Route("")
@ViewController(id = "MainView")
@ViewDescriptor(path = "main-view.xml")
open class MainView : StandardMainView() {

    @Autowired
    private lateinit var currentAuthentication: CurrentAuthentication

    @Autowired
    private lateinit var dataManager: DataManager

    @ViewComponent
    private lateinit var welcomeMessage: H2

    @ViewComponent
    private lateinit var appVersion:Span

    @ViewComponent
    private lateinit var boxV: VerticalLayout

    @ViewComponent
    private lateinit var vbox: VerticalLayout

    @ViewComponent
    private lateinit var scroll4at:Scroller

    @ViewComponent
    private lateinit var weekCommitCount: H2

    @ViewComponent
    private lateinit var monthCommitCount: H2

    @Autowired
    private val buildProperties: BuildProperties? = null

    @Autowired
    private lateinit var chatHistory: ChatHistoryService

    private lateinit var messageListItems:List<MessageListItem>

    @ViewComponent
    private lateinit var msList: MessageList

    @Autowired
    private val uiEventPublisher: UiEventPublisher? = null

    @Subscribe
    private fun onInit(event: InitEvent) {
        val currentUser = currentAuthentication.user as User

        welcomeMessage.text = "${currentUser.firstName}, привет!!! "
        appVersion.text = "Версия сборки ${buildProperties?.version}"

        if (currentUser.llmAvailable == true){
            addChat(user = currentUser)
            boxV.isVisible = true
        }

        // Load and display commit statistics
        loadCommitStatistics()
    }

@Subscribe("AddEvent")
fun onCancelButtonClick(event: ClickEvent<JmixButton>) {
    uiEventPublisher?.publishEventForUsers(
        AiCompanion(this),
        Collections.singleton((currentAuthentication.user as User).getUsername())
    )
}

    private fun loadCommitStatistics() {
        val weekCount = countCommitsLastWeek()
        val monthCount = countCommitsCurrentMonth()

        weekCommitCount.text = weekCount.toString()
        monthCommitCount.text = monthCount.toString()
    }

    private fun countCommitsLastWeek(): Long  {
        val oneWeekAgo = LocalDateTime.now().minusDays(7)

        val result = dataManager.loadValue(
            "select count(c) from Commit_ c where c.dateCreated >= :startDate and c.author = :author",
            Long::class.java
        )
            .parameter("startDate", oneWeekAgo)
            .parameter("author", currentAuthentication.user)
            .one()

        return result
    }

    private fun countCommitsCurrentMonth():  Long {
        val firstDayOfMonth = LocalDateTime.now().withDayOfMonth(1).toLocalDate().atStartOfDay()
        
        val result = dataManager.loadValue(
            "select count(c) from Commit_ c where c.dateCreated >= :startDate and c.author = :author",
            Long::class.java
        )
            .parameter("startDate", firstDayOfMonth)
            .parameter("author", currentAuthentication.user)
            .one()

        return result
    }

    private fun updateMessageList(session: ChatSession) {
        messageListItems = chatHistory
            .getHistory(session)
            .map { mapToMessageListItem(it) }

        msList.setItems(messageListItems)

    }

    private fun addChat(user: User){
        val chatSession = chatHistory.getUserSessions(user)

        val listMessage = chatHistory.getHistory(chatSession)

        messageListItems = listMessage.map { mapToMessageListItem(it) }
        msList = MessageList()
        msList.setItems(messageListItems)
        vbox.add(msList)
//        boxV.add(msList)

        val input = MessageInput()
        input.addSubmitListener { submitEvent: SubmitEvent ->
            eventNewQuery(submitEvent, chatSession)
        }
        vbox.add(input)
//        boxV.add(input)
    }

    private fun mapToMessageListItem(chatMessage: ChatMessage): MessageListItem {

        val mli = MessageListItem(
            chatMessage.content,
            chatMessage.timestamp!!.toInstant(ZoneOffset.ofHours(3)),
            getDisplayName(chatMessage)
        )
        if (chatMessage.getRole() == MessageRole.ASSISTANT){
            mli.userColorIndex = 1
        }else{
            mli.userColorIndex = 2
        }

        return mli
    }

    private fun eventNewQuery(submitEvent: SubmitEvent, session: ChatSession) {

        if (submitEvent.value.isEmpty()){
            return
        }

        chatHistory.newMessage(session, submitEvent.value)

        messageListItems = chatHistory
                .getHistory(session)
                .map { mapToMessageListItem(it) }

        msList.setItems(messageListItems)

    }

    private fun getDisplayName(message: ChatMessage): String? {
        val session = message.session

        return when (message.getRole()) {
            MessageRole.USER -> session?.user?.firstName
            MessageRole.ASSISTANT -> session?.botName
            MessageRole.SYSTEM -> "System"
            else -> {"none"}
        }
    }

    @EventListener
    private fun addNewMessage(event: AiCompanion) {

//        chatHistory.newMessage(session = chatHistory.getUserSessions(currentAuthentication.user as User), "PODPISKA")
        updateMessageList(session = chatHistory.getUserSessions(currentAuthentication.user as User))
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
