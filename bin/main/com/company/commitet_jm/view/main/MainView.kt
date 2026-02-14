package com.company.commitet_jm.view.main

import com.company.commitet_jm.service.chat.ChatHistoryService
import com.company.commitet_jm.entity.ChatMessage
import com.company.commitet_jm.entity.ChatSession
import com.company.commitet_jm.entity.ChatType
import com.company.commitet_jm.entity.MessageRole
import com.company.commitet_jm.entity.User
import com.company.commitet_jm.sheduledJob.AiCompanion
import com.vaadin.flow.component.ClickEvent
import com.vaadin.flow.component.html.H2
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.messages.MessageInput
import com.vaadin.flow.component.messages.MessageInput.SubmitEvent
import com.vaadin.flow.component.messages.MessageList
import com.vaadin.flow.component.messages.MessageListItem
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.tabs.Tab
import com.vaadin.flow.router.Route
import io.jmix.flowui.component.tabsheet.JmixTabSheet
import io.jmix.core.DataManager
import io.jmix.core.security.CurrentAuthentication
import io.jmix.flowui.Dialogs
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

@Route("")
@ViewController(id = "MainView")
@ViewDescriptor(path = "main-view.xml")
open class MainView : StandardMainView() {

    @Autowired
    private lateinit var currentAuthentication: CurrentAuthentication

    @Autowired
    private lateinit var dataManager: DataManager

    @Autowired
    private lateinit var dialogs: Dialogs

    @ViewComponent
    private lateinit var welcomeMessage: H2

    @ViewComponent
    private lateinit var appVersion: Span

    @ViewComponent
    private lateinit var boxV: VerticalLayout

    @ViewComponent
    private lateinit var chatTabs: JmixTabSheet

    @ViewComponent
    private lateinit var weekCommitCount: H2

    @ViewComponent
    private lateinit var monthCommitCount: H2

    @ViewComponent
    private lateinit var newChatBtn: JmixButton

    @ViewComponent
    private lateinit var refreshChatsBtn: JmixButton

    @ViewComponent
    private lateinit var deleteChatBtn: JmixButton

    @Autowired
    private val buildProperties: BuildProperties? = null

    @Autowired
    private lateinit var chatHistory: ChatHistoryService

    @Autowired
    private val uiEventPublisher: UiEventPublisher? = null

    private val chatTabsMap = mutableMapOf<Long, Tab>()
    private val messageListsMap = mutableMapOf<Long, MessageList>()
    private var currentUser: User? = null

    @Subscribe
    private fun onInit(event: InitEvent) {
        currentUser = currentAuthentication.user as User

        welcomeMessage.text = "${currentUser?.firstName}, привет!!! "
        appVersion.text = "Версия сборки ${buildProperties?.version}"

        if (currentUser?.llmAvailable == true) {
            initChatUI()
            boxV.isVisible = true
        }

        loadCommitStatistics()
    }

    private fun initChatUI() {
        val user = currentUser ?: return

        // Загружаем все чаты пользователя
        val chats = chatHistory.getUserChats(user)

        // Если нет чатов, создаём LLM чат по умолчанию
        if (chats.isEmpty()) {
            val llmChat = chatHistory.getOrCreateLlmChat(user)
            addChatTab(llmChat)
        } else {
            // Добавляем табы для всех существующих чатов
            chats.forEach { session ->
                addChatTab(session)
            }
        }
    }

    private fun addChatTab(session: ChatSession) {
        val user = currentUser ?: return
        val sessionId = session.id ?: return

        // Проверяем, не добавлен ли уже таб для этой сессии
        chatTabsMap[sessionId]?.let { existingTab ->
            chatTabs.setSelectedTab(existingTab)
            return
        }

        // Создаём контент таба
        val tabContent = VerticalLayout()
        tabContent.setSizeFull()
        tabContent.isPadding = false
        tabContent.isSpacing = true

        // Создаём список сообщений
        val messageList = MessageList()
        messageList.setSizeFull()
        messageListsMap[sessionId] = messageList

        // Загружаем историю сообщений
        updateMessageListForSession(session, messageList)

        // Создаём поле ввода
        val messageInput = MessageInput()
        messageInput.setWidthFull()
        messageInput.addSubmitListener { submitEvent ->
            handleMessageSubmit(submitEvent, session, messageList)
        }

        tabContent.add(messageList, messageInput)
        tabContent.expand(messageList)

        // Определяем название таба
        val tabTitle = chatHistory.getChatTitle(session, user)
        val tab = chatTabs.add(tabTitle, tabContent)
        chatTabsMap[sessionId] = tab
    }

    private fun handleMessageSubmit(submitEvent: SubmitEvent, session: ChatSession, messageList: MessageList) {
        if (submitEvent.value.isEmpty()) {
            return
        }

        chatHistory.newMessage(session, submitEvent.value)
        updateMessageListForSession(session, messageList)
    }

    private fun updateMessageListForSession(session: ChatSession, messageList: MessageList) {
        val messages = chatHistory.getChatHistory(session)
        val items = messages.map { mapToMessageListItem(it, session) }
        messageList.setItems(items)
    }

    @Subscribe("refreshChatsBtn")
    private fun onRefreshChatsBtnClick(event: ClickEvent<JmixButton>) {
        refreshAllChats()
    }

    private fun refreshAllChats() {
        messageListsMap.forEach { (sessionId, messageList) ->
            val session = dataManager.load(ChatSession::class.java)
                .id(sessionId)
                .optional()
            if (session.isPresent) {
                updateMessageListForSession(session.get(), messageList)
            }
        }
    }

    @Subscribe("deleteChatBtn")
    private fun onDeleteChatBtnClick(event: ClickEvent<JmixButton>) {
        val selectedTab = chatTabs.selectedTab ?: return

        // Находим сессию по выбранному табу
        val sessionId = chatTabsMap.entries.find { it.value == selectedTab }?.key ?: return

        val session = dataManager.load(ChatSession::class.java)
            .id(sessionId)
            .optional()

        if (session.isEmpty) return

        dialogs.createOptionDialog()
            .withHeader("Удаление чата")
            .withText("Вы уверены, что хотите удалить этот чат? Все сообщения будут удалены.")
            .withActions(
                io.jmix.flowui.action.DialogAction(io.jmix.flowui.action.DialogAction.Type.YES)
                    .withText("Удалить")
                    .withVariant(io.jmix.flowui.kit.action.ActionVariant.DANGER)
                    .withHandler {
                        deleteChat(sessionId, session.get())
                    },
                io.jmix.flowui.action.DialogAction(io.jmix.flowui.action.DialogAction.Type.CANCEL)
                    .withText("Отмена")
            )
            .open()
    }

    private fun deleteChat(sessionId: Long, session: ChatSession) {
        // Удаляем все сообщения чата
        val messages = dataManager.load(ChatMessage::class.java)
            .query("select m from ChatMessage m where m.session = :session")
            .parameter("session", session)
            .list()

        messages.forEach { dataManager.remove(it) }

        // Удаляем сессию
        dataManager.remove(session)

        // Удаляем таб из UI
        val tab = chatTabsMap.remove(sessionId)
        messageListsMap.remove(sessionId)
        if (tab != null) {
            chatTabs.remove(tab)
        }
    }

    @Subscribe("newChatBtn")
    private fun onNewChatBtnClick(event: ClickEvent<JmixButton>) {
        val user = currentUser ?: return

        // Получаем список доступных пользователей для чата
        val availableUsers = chatHistory.getAvailableRecipients(user)

        // Создаём диалог выбора
        dialogs.createOptionDialog()
            .withHeader("Новый чат")
            .withText("Выберите тип чата")
            .withActions(
                io.jmix.flowui.action.DialogAction(io.jmix.flowui.action.DialogAction.Type.YES)
                    .withText("AI Assistant")
                    .withHandler {
                        createOrSelectLlmChat()
                    },
                io.jmix.flowui.action.DialogAction(io.jmix.flowui.action.DialogAction.Type.NO)
                    .withText("Пользователь")
                    .withHandler {
                        showUserSelectionDialog(availableUsers)
                    },
                io.jmix.flowui.action.DialogAction(io.jmix.flowui.action.DialogAction.Type.CANCEL)
                    .withText("Отмена")
            )
            .open()
    }

    private fun createOrSelectLlmChat() {
        val user = currentUser ?: return
        val llmChat = chatHistory.getOrCreateLlmChat(user)
        addChatTab(llmChat)
    }

    private fun showUserSelectionDialog(users: List<User>) {
        if (users.isEmpty()) {
            dialogs.createMessageDialog()
                .withHeader("Нет доступных пользователей")
                .withText("Нет других пользователей с доступом к чату")
                .open()
            return
        }

        // Создаём диалог с выбором пользователя
        val inputDialog = dialogs.createInputDialog(this)
            .withHeader("Выберите пользователя")
            .withParameters(
                io.jmix.flowui.app.inputdialog.InputParameter.entityParameter(
                    "recipient",
                    User::class.java
                )
                    .withLabel("Пользователь")
                    .withRequired(true)
            )
            .withActions(
                io.jmix.flowui.app.inputdialog.DialogActions.OK_CANCEL
            )
            .withCloseListener { closeEvent ->
                if (closeEvent.closedWith(io.jmix.flowui.app.inputdialog.DialogOutcome.OK)) {
                    val recipient = closeEvent.getValue<User>("recipient")
                    if (recipient != null) {
                        createUserChat(recipient)
                    }
                }
            }
            .open()
    }

    private fun createUserChat(recipient: User) {
        val user = currentUser ?: return
        val chat = chatHistory.getOrCreateUserChat(user, recipient)
        addChatTab(chat)
    }

    @Subscribe("AddEvent")
    fun onCancelButtonClick(event: ClickEvent<JmixButton>) {
        uiEventPublisher?.publishEventForUsers(
            AiCompanion(this),
            java.util.Collections.singleton((currentAuthentication.user as User).getUsername())
        )
    }

    private fun loadCommitStatistics() {
        val weekCount = countCommitsLastWeek()
        val monthCount = countCommitsCurrentMonth()

        weekCommitCount.text = weekCount.toString()
        monthCommitCount.text = monthCount.toString()
    }

    private fun countCommitsLastWeek(): Long {
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

    private fun countCommitsCurrentMonth(): Long {
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

    private fun mapToMessageListItem(chatMessage: ChatMessage, session: ChatSession): MessageListItem {
        val mli = MessageListItem(
            chatMessage.content,
            chatMessage.timestamp!!.toInstant(ZoneOffset.ofHours(3)),
            getDisplayName(chatMessage, session)
        )
        if (chatMessage.getRole() == MessageRole.ASSISTANT) {
            mli.userColorIndex = 1
        } else {
            mli.userColorIndex = 2
        }

        return mli
    }

    private fun getDisplayName(message: ChatMessage, session: ChatSession): String? {
        return when (message.getRole()) {
            MessageRole.USER -> {
                if (session.getChatType() == ChatType.USER) {
                    // Для user-to-user чатов показываем имя отправителя
                    session.user?.firstName ?: "Пользователь"
                } else {
                    session.user?.firstName
                }
            }
            MessageRole.ASSISTANT -> session.botName ?: "AI Assistant"
            MessageRole.SYSTEM -> "System"
            else -> "none"
        }
    }

    @EventListener
    private fun addNewMessage(event: AiCompanion) {
        val user = currentUser ?: return
        val llmSession = chatHistory.getOrCreateLlmChat(user)
        val sessionId = llmSession.id ?: return
        val messageList = messageListsMap[sessionId]
        if (messageList != null) {
            updateMessageListForSession(llmSession, messageList)
        }
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
