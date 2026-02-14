package com.company.commitet_jm.view.commit

import com.company.commitet_jm.entity.*
import com.company.commitet_jm.service.git.CommitDiffInfo
import com.company.commitet_jm.service.git.DiffChangeType
import com.company.commitet_jm.service.git.DiffEntry
import com.company.commitet_jm.service.git.GitService
import com.company.commitet_jm.service.ones.OneRunner
import com.company.commitet_jm.view.main.MainView
import com.vaadin.flow.component.ClickEvent
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.grid.Grid
import com.vaadin.flow.component.html.*
import com.vaadin.flow.component.icon.Icon
import com.vaadin.flow.component.icon.VaadinIcon
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.data.renderer.ComponentRenderer
import com.vaadin.flow.router.Route
import io.jmix.core.DataManager
import io.jmix.core.FileStorageLocator
import io.jmix.core.TimeSource
import io.jmix.core.security.CurrentAuthentication
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import io.jmix.flowui.Dialogs
import io.jmix.flowui.component.details.JmixDetails
import io.jmix.flowui.component.grid.DataGrid
import io.jmix.flowui.component.textarea.JmixTextArea
import io.jmix.flowui.component.textfield.TypedTextField
import io.jmix.flowui.component.valuepicker.EntityPicker
import io.jmix.flowui.kit.component.button.JmixButton
import io.jmix.flowui.view.*
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@Route(value = "commits/:id", layout = MainView::class)
@ViewController(id = "Commit_.detail")
@ViewDescriptor(path = "commit-detail-view.xml")
@EditedEntityContainer("commitDc")
class CommitDetailView : StandardDetailView<Commit>() {
    @Autowired
    private lateinit var oneRunner: OneRunner

    @Autowired
    private lateinit var timeSource: TimeSource

    @Autowired
    private lateinit var currentAuthentication: CurrentAuthentication

    @Autowired
    private lateinit var dataManager: DataManager

    @Autowired
    private lateinit var gitService: GitService

    @Autowired
    private lateinit var dialogs: Dialogs

    @ViewComponent
    private lateinit var errorInfoField: JmixTextArea

    @ViewComponent
    private lateinit var statusField: TypedTextField<Any>

    @ViewComponent
    private lateinit var descriptionField: JmixTextArea

    @ViewComponent
    private lateinit var taskNumField: TypedTextField<Any>

    @ViewComponent
    private lateinit var projectField: EntityPicker<Any>

    @ViewComponent
    private lateinit var filesDataGrid: DataGrid<FileCommit>

    @ViewComponent
    private lateinit var buttonsPanel: HorizontalLayout

    @ViewComponent
    private lateinit var clearStatusCommit: Button

    @ViewComponent
    private lateinit var startAnalyzeButton: Button

    @ViewComponent
    private lateinit var uploadFilesButton: Button

    @ViewComponent
    private lateinit var urlBranchBox: HorizontalLayout

    @ViewComponent
    private lateinit var diffContainer: VerticalLayout

    @ViewComponent
    private lateinit var diffStatsLabel: Span

    @ViewComponent
    private lateinit var rawDiffArea: JmixTextArea

    @ViewComponent
    private lateinit var rawDiffDetails: JmixDetails

    @ViewComponent
    private lateinit var showRawDiffButton: Button

    private lateinit var diffEntriesGrid: Grid<DiffEntry>

    @Autowired
    private lateinit var fileStorageLocator: FileStorageLocator

    companion object {
        private val log = LoggerFactory.getLogger(CommitDetailView::class.java)
    }

    @Subscribe
    private fun onInitEntity(event: InitEntityEvent<Commit>) {
        errorInfoField.isVisible = false
        val recommit = event.entity
        recommit.dateCreated = timeSource.now().toLocalDateTime()
        recommit.setStatus(StatusSheduler.NEW)
        recommit.author = currentAuthentication.getUser() as User
    }

    @Subscribe
    private fun onInit(event: InitEvent) {
        setupDiffGrid()
    }

    @Subscribe(id = "saveAndCloseButton", subject = "clickListener")
    private fun onSaveAndCloseButtonClick(event: ClickEvent<JmixButton>) {
        log.info("save commit")
    }

    @Subscribe
    private fun onReady(event: ReadyEvent) {
        initHtmlContent(branchLink = editedEntity.urlBranch ?: "")
        loadDiffData()

        val cUser = currentAuthentication.user as User
        if (cUser.isAdmin == true) {
            clearStatusCommit.isVisible = true
            startAnalyzeButton.isVisible = true
            uploadFilesButton.isVisible = true
            return
        }
        if (statusField.value.toString().lowercase() == "new" ||
            statusField.value.toString().lowercase() == "новый"
        ) {
            return
        }

        descriptionField.isEnabled = false
        taskNumField.isEnabled = false
        projectField.isEnabled = false
        filesDataGrid.isEnabled = false
        buttonsPanel.isVisible = false
    }

    private fun setupDiffGrid() {
        // Создаём грид программно, так как DiffEntry не JPA-entity
        diffEntriesGrid = Grid(DiffEntry::class.java, false)
        diffEntriesGrid.setWidthFull()
        diffEntriesGrid.minHeight = "250px"

        // Колонка каталога
        diffEntriesGrid.addColumn { it.directory.ifEmpty { "(корень)" } }
            .setHeader("Каталог")
            .setWidth("200px")
            .setResizable(true)

        // Колонка имени файла с иконкой
        diffEntriesGrid.addColumn(ComponentRenderer { entry ->
            HorizontalLayout().apply {
                isPadding = false
                isSpacing = true
                alignItems = com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.CENTER

                val icon = when (entry.changeType) {
                    DiffChangeType.ADDED -> Icon(VaadinIcon.PLUS).apply { color = "green" }
                    DiffChangeType.MODIFIED -> Icon(VaadinIcon.EDIT).apply { color = "blue" }
                    DiffChangeType.DELETED -> Icon(VaadinIcon.TRASH).apply { color = "red" }
                    DiffChangeType.RENAMED -> Icon(VaadinIcon.ARROW_RIGHT).apply { color = "orange" }
                    DiffChangeType.COPIED -> Icon(VaadinIcon.COPY).apply { color = "purple" }
                }
                icon.setSize("16px")

                add(icon, Span(entry.fileName))
            }
        }).setHeader("Файл").setFlexGrow(1).setResizable(true)

        // Колонка типа изменения
        diffEntriesGrid.addColumn { it.changeType.displayName }
            .setHeader("Тип")
            .setWidth("120px")
            .setResizable(true)

        // Колонка статистики
        diffEntriesGrid.addColumn(ComponentRenderer { entry ->
            HorizontalLayout().apply {
                isPadding = false
                isSpacing = true
                if (entry.additions > 0) {
                    add(Span("+${entry.additions}").apply { style.set("color", "green") })
                }
                if (entry.deletions > 0) {
                    add(Span("-${entry.deletions}").apply { style.set("color", "red") })
                }
            }
        }).setHeader("Изменения").setWidth("100px").setResizable(true)

        // Обработчик клика для просмотра diff файла
        diffEntriesGrid.addItemClickListener { event ->
            showFileDiff(event.item)
        }

        // Добавляем грид в контейнер после diffStatsLabel (индекс 1)
        diffContainer.addComponentAtIndex(1, diffEntriesGrid)
    }

    private fun loadDiffData() {
        // DIFF_DATA был удален из модели, функция больше не используется
        val diffInfo = CommitDiffInfo.fromJson(null)

        if (diffInfo == null || diffInfo.entries.isEmpty()) {
            diffStatsLabel.text = "Нет данных об изменениях"
            rawDiffDetails.isVisible = false
            return
        }

        // Статистика
        diffStatsLabel.text = "Изменено файлов: ${diffInfo.totalFiles}, " +
                "+${diffInfo.totalAdditions} / -${diffInfo.totalDeletions} строк"

        // Загружаем данные в грид
        diffEntriesGrid.setItems(diffInfo.entries.sortedWith(
            compareBy({ it.directory }, { it.fileName })
        ))

        // Raw diff
        rawDiffArea.value = diffInfo.rawDiff ?: "Нет данных"
    }

    private fun showFileDiff(entry: DiffEntry) {
        if (entry.diffContent.isNullOrBlank()) {
            return
        }

        dialogs.createMessageDialog()
            .withHeader("Diff: ${entry.path}")
            .withContent(
                Pre(entry.diffContent).apply {
                    style.set("white-space", "pre-wrap")
                    style.set("word-wrap", "break-word")
                    style.set("font-family", "monospace")
                    style.set("font-size", "12px")
                    style.set("max-height", "500px")
                    style.set("overflow", "auto")
                    style.set("background-color", "#f5f5f5")
                    style.set("padding", "10px")
                    style.set("border-radius", "4px")
                }
            )
            .withWidth("80%")
            .withHeight("600px")
            .open()
    }

    @Subscribe(id = "showRawDiffButton", subject = "clickListener")
    private fun onShowRawDiffButtonClick(event: ClickEvent<Button>) {
        // DIFF_DATA был удален из модели
        val diffInfo = CommitDiffInfo.fromJson(null)
        val rawDiff = diffInfo?.rawDiff ?: "Нет данных"

        dialogs.createMessageDialog()
            .withHeader("Полный diff")
            .withContent(
                Pre(rawDiff).apply {
                    style.set("white-space", "pre-wrap")
                    style.set("word-wrap", "break-word")
                    style.set("font-family", "monospace")
                    style.set("font-size", "12px")
                    style.set("max-height", "600px")
                    style.set("overflow", "auto")
                    style.set("background-color", "#f5f5f5")
                    style.set("padding", "10px")
                }
            )
            .withWidth("90%")
            .withHeight("700px")
            .open()
    }

    protected fun initHtmlContent(branchLink: String) {
        if (branchLink.isEmpty()) return

        val div = Div()
        div.add(H3("Ссылка на ветку:"))
        div.add(Anchor(branchLink, branchLink))
        urlBranchBox.add(div)
    }

    @Subscribe(id = "clearStatusCommit", subject = "clickListener")
    private fun onClearStatusCommitClick(event: ClickEvent<JmixButton>) {
        editedEntity.setStatus(StatusSheduler.NEW)
        dataManager.save(editedEntity)
    }

    @Subscribe(id = "startAnalyzeButton", subject = "clickListener")
    private fun onStartAnalyzeButtonCommitClick(event: ClickEvent<JmixButton>) {
    }

    @Subscribe(id = "uploadFilesButton", subject = "clickListener")
    private fun onUploadFilesButtonCommitClick(event: ClickEvent<JmixButton>) {
        gitService.createCommit()
    }
}
