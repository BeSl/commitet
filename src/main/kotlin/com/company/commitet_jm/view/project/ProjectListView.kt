package com.company.commitet_jm.view.project

import com.company.commitet_jm.sheduledJob.GitCloneTask
import com.company.commitet_jm.entity.Project
import com.company.commitet_jm.service.project.ProjectService
import com.company.commitet_jm.view.main.MainView
import com.vaadin.flow.component.ClickEvent
import com.vaadin.flow.component.HasValueAndElement
import com.vaadin.flow.component.formlayout.FormLayout
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.router.Route
import io.jmix.core.validation.group.UiCrossFieldChecks
import io.jmix.flowui.Dialogs
import io.jmix.flowui.Notifications
import io.jmix.flowui.action.SecuredBaseAction
import io.jmix.flowui.component.UiComponentUtils
import io.jmix.flowui.component.grid.DataGrid
import io.jmix.flowui.component.textfield.TypedTextField
import io.jmix.flowui.component.validation.ValidationErrors
import io.jmix.flowui.kit.action.Action
import io.jmix.flowui.kit.action.ActionPerformedEvent
import io.jmix.flowui.kit.component.button.JmixButton
import io.jmix.flowui.model.CollectionContainer
import io.jmix.flowui.model.DataContext
import io.jmix.flowui.model.InstanceContainer
import io.jmix.flowui.model.InstanceLoader
import io.jmix.flowui.view.*
import io.jmix.flowui.view.Target
import io.jmix.flowui.action.DialogAction
import org.springframework.beans.factory.annotation.Autowired

@Route(value = "projects", layout = MainView::class)
@ViewController(id = "Project.list")
@ViewDescriptor(path = "project-list-view.xml")
@LookupComponent("projectsDataGrid")
@DialogMode(width = "64em")
class ProjectListView : StandardListView<Project>() {

    @Autowired
    private lateinit var projectService: ProjectService

    @Autowired
    private lateinit var dialogs: Dialogs

    @Autowired
    private lateinit var notifications: Notifications

    @ViewComponent
    private lateinit var dataContext: DataContext

    @ViewComponent
    private lateinit var projectsDc: CollectionContainer<Project>

    @ViewComponent
    private lateinit var projectDc: InstanceContainer<Project>

    @ViewComponent
    private lateinit var projectDl: InstanceLoader<Project>

    @ViewComponent
    private lateinit var listLayout: VerticalLayout

    @ViewComponent
    private lateinit var projectsDataGrid: DataGrid<Project>

    @ViewComponent
    private lateinit var form: FormLayout

    @ViewComponent
    private lateinit var detailActions: HorizontalLayout

    @ViewComponent
    private lateinit var localPathField: TypedTextField<String>

    @Subscribe
    fun onInit(event: InitEvent) {
        projectsDataGrid.actions.forEach { action ->
            if (action is SecuredBaseAction) {
                action.addEnabledRule { listLayout.isEnabled }
            }
        }
    }

    @Subscribe
    fun onBeforeShow(event: BeforeShowEvent) {
        updateControls(false)
    }

    @Subscribe("projectsDataGrid.createAction")
    fun onProjectsDataGridCreateAction(event: ActionPerformedEvent) {
        dataContext.clear()
        val entity: Project = dataContext.create(Project::class.java) ?: return
        projectDc.setItem(entity)
        updateControls(true)
    }

    @Subscribe("projectsDataGrid.editAction")
    fun onProjectsDataGridEditAction(event: ActionPerformedEvent) {
        updateControls(true)
    }

    @Subscribe("saveButton")
    fun onSaveButtonClick(event: ClickEvent<JmixButton>) {
        val item = projectDc.item
        val validationErrors = validateView(item)
        if (!validationErrors.isEmpty) {
            val viewValidation = getViewValidation()
            viewValidation.showValidationErrors(validationErrors)
            viewValidation.focusProblemComponent(validationErrors)
            return
        }
        dataContext.save()
        projectsDc.replaceItem(item)
        updateControls(false)
    }

    @Subscribe("cloneGitButton")
    fun cloneGitButtonClick(event: ClickEvent<JmixButton>) {
        val project = projectDc.itemOrNull
        if (project == null) {
            notifications.create("Выберите проект").show()
            return
        }

        if (project.urlRepo.isNullOrBlank()) {
            notifications.create("Укажите URL репозитория").show()
            return
        }

        if (project.defaultBranch.isNullOrBlank()) {
            notifications.create("Укажите ветку по умолчанию").show()
            return
        }

        if (project.name.isNullOrBlank()) {
            notifications.create("Укажите имя проекта").show()
            return
        }

        // Сначала сохраняем проект если есть изменения
        if (dataContext.hasChanges()) {
            dataContext.save()
        }

        // Показываем предполагаемый путь
        val expectedPath = projectService.getProjectRepoPath(project)

        dialogs.createOptionDialog()
            .withHeader("Клонирование репозитория")
            .withText("Репозиторий будет склонирован в:\n$expectedPath\n\nПродолжить?")
            .withActions(
                DialogAction(DialogAction.Type.YES).withHandler {
                    startCloning(project)
                },
                DialogAction(DialogAction.Type.NO)
            )
            .open()
    }

    private fun startCloning(project: Project) {
        val task = object : GitCloneTask(projectService, project) {
            override fun done(result: String) {
                super.done(result)
                // Перезагружаем проект чтобы увидеть обновлённый localPath
                projectDl.load()
                notifications.create("Репозиторий успешно склонирован")
                    .withType(Notifications.Type.SUCCESS)
                    .show()
            }
        }

        dialogs.createBackgroundTaskDialog(task)
            .withHeader("Клонирование репозитория")
            .withText("Подождите, идет клонирование...")
            .withCancelAllowed(true)
            .open()
    }

    @Subscribe("cancelButton")
    fun onCancelButtonClick(event: ClickEvent<JmixButton>) {
        dataContext.clear()
        projectDc.setItem(null)
        projectDl.load()
        updateControls(false)
    }

    @Subscribe(id = "projectsDc", target = Target.DATA_CONTAINER)
    fun onProjectsDcItemChange(event: InstanceContainer.ItemChangeEvent<Project>) {
        val entity: Project? = event.item
        dataContext.clear()
        if (entity != null) {
            projectDl.entityId = entity.id
            projectDl.load()
        } else {
            projectDl.entityId = null
            projectDc.setItem(null)
        }
        updateControls(false)
    }

    private fun validateView(entity: Project): ValidationErrors {
        val viewValidation = getViewValidation()
        val validationErrors = viewValidation.validateUiComponents(form)
        if (!validationErrors.isEmpty) {
            return validationErrors
        }
        validationErrors.addAll(viewValidation.validateBeanGroup(UiCrossFieldChecks::class.java, entity))
        return validationErrors
    }

    private fun updateControls(editing: Boolean) {
        UiComponentUtils.getComponents(form).forEach { component ->
            if (component is HasValueAndElement<*, *>) {
                component.isReadOnly = !editing
            }
        }
        // localPath всегда readonly - заполняется автоматически при клонировании
        localPathField.isReadOnly = true

        detailActions.isVisible = editing
        listLayout.isEnabled = !editing
        projectsDataGrid.getActions().forEach(Action::refreshState)
    }

    private fun getViewValidation(): ViewValidation {
        return applicationContext.getBean(ViewValidation::class.java)
    }
}
