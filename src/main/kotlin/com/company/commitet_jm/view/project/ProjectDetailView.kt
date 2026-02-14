package com.company.commitet_jm.view.project

import com.company.commitet_jm.entity.Project
import com.company.commitet_jm.entity.ProjectExternalId
import com.company.commitet_jm.entity.User
import com.company.commitet_jm.service.project.ProjectService
import com.company.commitet_jm.sheduledJob.GitCloneTask
import com.company.commitet_jm.view.main.MainView
import com.vaadin.flow.component.ClickEvent
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.router.Route
import io.jmix.core.security.CurrentAuthentication
import io.jmix.flowui.Dialogs
import io.jmix.flowui.Notifications
import io.jmix.flowui.action.DialogAction
import io.jmix.flowui.component.grid.DataGrid
import io.jmix.flowui.kit.component.button.JmixButton
import io.jmix.flowui.view.*
import org.springframework.beans.factory.annotation.Autowired

@Route(value = "projects/:id", layout = MainView::class)
@ViewController(id = "Project.detail")
@ViewDescriptor(path = "project-detail-view.xml")
@EditedEntityContainer("projectDc")
class ProjectDetailView : StandardDetailView<Project>() {

    @Autowired
    private lateinit var currentAuthentication: CurrentAuthentication

    @Autowired
    private lateinit var projectService: ProjectService

    @Autowired
    private lateinit var dialogs: Dialogs

    @Autowired
    private lateinit var notifications: Notifications

    @ViewComponent
    private lateinit var externalIdsPanel: VerticalLayout

    @ViewComponent
    private lateinit var externalIdsDataGrid: DataGrid<ProjectExternalId>

    @Subscribe
    fun onReady(event: ReadyEvent) {
        val cUser = currentAuthentication.user as User
        // Показываем панель внешних ID только администраторам
        externalIdsPanel.isVisible = cUser.isAdmin == true
    }

    @Subscribe("cloneGitButton")
    fun cloneGitButtonClick(event: ClickEvent<JmixButton>) {
        val project = editedEntity

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
}
