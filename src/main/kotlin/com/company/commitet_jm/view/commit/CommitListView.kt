package com.company.commitet_jm.view.commit

import com.company.commitet_jm.entity.Commit
import com.company.commitet_jm.entity.StatusSheduler
import com.company.commitet_jm.entity.User
import com.company.commitet_jm.service.git.GitService
import com.company.commitet_jm.view.main.MainView
import com.vaadin.flow.component.ClickEvent
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.data.renderer.ComponentRenderer
import com.vaadin.flow.data.renderer.Renderer
import com.vaadin.flow.router.Route
import io.jmix.core.DataManager
import io.jmix.core.FileStorageLocator
import io.jmix.core.Messages
import io.jmix.core.security.CurrentAuthentication
import io.jmix.flowui.UiComponents
import io.jmix.flowui.kit.component.button.JmixButton
import io.jmix.flowui.model.CollectionContainer
import io.jmix.flowui.model.CollectionLoader
import io.jmix.flowui.view.*
import io.jmix.flowui.view.Target
import io.jmix.flowui.view.Supply
import org.springframework.beans.factory.annotation.Autowired


@Route(value = "commits", layout = MainView::class)
@ViewController(id = "Commit_.list")
@ViewDescriptor(path = "commit-list-view.xml")
@LookupComponent("commitsDataGrid")
@DialogMode(width = "64em")
class CommitListView : StandardListView<Commit>() {
    @Autowired
    protected var uiComponents: UiComponents? = null

    @Autowired
    protected var messages: Messages? = null

    @Autowired
    private lateinit var fileStorageLocator: FileStorageLocator

    @Autowired
    private lateinit var currentAuthentication: CurrentAuthentication

    @Autowired
    private lateinit var dataManager: DataManager

    @Autowired
    private lateinit var gitService: GitService

    @ViewComponent
    private val commitsDl: CollectionLoader<Commit>? = null

    @Subscribe(id = "commitsDc", target = Target.DATA_CONTAINER)
    private fun onCommitsDcCollectionChange(event: CollectionContainer.CollectionChangeEvent<Commit>) {

    }

    @Subscribe(id = "CreateCommitButton")
    private fun onCreateCommitButtonClick(event: ClickEvent<JmixButton>) {
        gitService.createCommit()
    }

    @Subscribe
    private fun onInit(event: InitEvent) {
        val cUser = currentAuthentication.user as User
        if (cUser?.isAdmin == true) {
            return
        }
            commitsDl?.setParameter("user", currentAuthentication.user as User)

    }

    @Supply(to = "commitsDataGrid.status", subject = "renderer")
    private fun commitsDataGridStatusRenderer(): Renderer<Commit> {
        return ComponentRenderer(this::createGradeComponent, this::gradeComponentUpdater)
    }


    fun createGradeComponent(): Span {
        val span: Span = uiComponents!!.create(Span::class.java)
        span.element.themeList.add("badge")

        return span
    }

    fun gradeComponentUpdater(span: Span, commit: Commit) {
        if (commit.getStatus() != null) {
            span.text = messages!!.getMessage(StatusSheduler::class.java, commit.getStatus().toString())

            when (commit.getStatus()) {
                StatusSheduler.NEW -> span.element.themeList.add("primary")
                StatusSheduler.PROCESSED -> span.element.themeList.add("contrast")
                StatusSheduler.ERROR -> span.element.themeList.add("error")
                StatusSheduler.COMPLETE -> span.element.themeList.add("success")
                null -> span.element.themeList.add("primary")
            }
        } else {
            span.text = "No data"
        }
    }

}