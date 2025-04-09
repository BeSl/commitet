package com.company.commitet_jm.view.project

import com.company.commitet_jm.app.GitCloneTask
import com.company.commitet_jm.entity.Project
import com.company.commitet_jm.view.main.MainView
import com.vaadin.flow.component.ClickEvent
import com.vaadin.flow.component.HasValueAndElement
import com.vaadin.flow.component.formlayout.FormLayout
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.router.Route
import io.jmix.core.DataManager
import io.jmix.core.FileStorageLocator
import io.jmix.core.validation.group.UiCrossFieldChecks
import io.jmix.flowui.Dialogs
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
import org.springframework.beans.factory.annotation.Autowired

@Route(value = "projects", layout = MainView::class)
@ViewController(id = "Project.list")
@ViewDescriptor(path = "project-list-view.xml")
@LookupComponent("projectsDataGrid")
@DialogMode(width = "64em")
class ProjectListView : StandardListView<Project>() {
    @Autowired
    private lateinit var fileStorageLocator: FileStorageLocator

    @Autowired
    private lateinit var dataManager: DataManager

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
    private lateinit var urlRepoField: TypedTextField<Any>

    @ViewComponent
    private lateinit var localPathField: TypedTextField<String>

    @ViewComponent
    private lateinit var defaultBranchField: TypedTextField<String>

    @Autowired
    private lateinit var dialogs: Dialogs

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
        val entity: Project = dataContext.create(Project::class.java)
        projectDc.item = entity
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

        val task = GitCloneTask(
            dataManager = dataManager,
            fileStorageLocator = fileStorageLocator
        ).apply {
            urlRepo = urlRepoField.value
            localPath = localPathField.value
            defaultBranch = defaultBranchField.value

        }

        dialogs.createBackgroundTaskDialog(task)
            .withHeader("Клонирование репозитория")
            .withText("Подождите, идет клонирование...")
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
        detailActions.isVisible = editing
        listLayout.isEnabled = !editing
        projectsDataGrid.getActions().forEach(Action::refreshState);
    }

    private fun getViewValidation(): ViewValidation {
        return applicationContext.getBean(ViewValidation::class.java)
    }
}