package com.company.commitet_jm.view.commit

import com.company.commitet_jm.entity.*
import com.company.commitet_jm.view.main.MainView
import com.vaadin.flow.component.ClickEvent
import com.vaadin.flow.component.Html
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.html.*
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.router.Route
import io.jmix.core.DataManager
import io.jmix.core.TimeSource
import io.jmix.core.security.CurrentAuthentication
import io.jmix.flowui.DialogWindows
import io.jmix.flowui.action.entitypicker.EntityLookupAction
import io.jmix.flowui.component.grid.DataGrid
import io.jmix.flowui.component.textarea.JmixTextArea
import io.jmix.flowui.component.textfield.TypedTextField
import io.jmix.flowui.component.valuepicker.EntityPicker
import io.jmix.flowui.kit.component.button.JmixButton
import io.jmix.flowui.view.*
import io.jmix.flowui.view.builder.LookupWindowBuilderProcessor
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import java.util.*


@Route(value = "commits/:id", layout = MainView::class)
@ViewController(id = "Commit_.detail")
@ViewDescriptor(path = "commit-detail-view.xml")
@EditedEntityContainer("commitDc")
class CommitDetailView : StandardDetailView<Commit>() {

    @Autowired
    private lateinit var timeSource: TimeSource

    @Autowired
    private lateinit var currentAuthentication: CurrentAuthentication

    @Autowired
    private lateinit var dataManager: DataManager

    @field:ViewComponent("projectField.entityLookupAction")
    private lateinit var projectFieldEntityLookupAction: EntityLookupAction<Project>

    @Autowired
    private lateinit var dialogWindows: DialogWindows

    @Autowired
    private lateinit var lookupWindowBuilderProcessor: LookupWindowBuilderProcessor

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
    private lateinit var urlBranchBox: HorizontalLayout

    companion object {
        private  val log = LoggerFactory.getLogger(CommitDetailView::class.java)
    }

    @Subscribe
    private fun onInitEntity(event: InitEntityEvent<Commit>) {

        errorInfoField.isVisible = false
        var recommit = event.entity

        recommit.setStatus(StatusSheduler.NEW)
        recommit.author= currentAuthentication.getUser() as User

    }

    @Subscribe
    private fun onInit(event: InitEvent) {
    }

    @Subscribe(id = "saveAndCloseButton", subject = "clickListener")
    private fun onSaveAndCloseButtonClick(event: ClickEvent<JmixButton>) {
        log.info("save commit")
    }

    @Subscribe
    private fun onReady(event: ReadyEvent) {
        initHtmlContent(editedEntity.urlBranch?:"")
        val cuser = currentAuthentication.getUser() as User
        if (cuser.isAdmin == true){
            clearStatusCommit.isVisible = true
            return
        }
        if (statusField.value.toString().lowercase() == "new" ||
            statusField.value.toString().lowercase() == "новый") {
            return
        }

        descriptionField.isEnabled = false
        taskNumField.isEnabled  =  false
        projectField.isEnabled = false
        filesDataGrid.isEnabled = false
        buttonsPanel.isVisible = false
    }

    protected fun initHtmlContent(branchLink: String) {
        if (branchLink.isEmpty()) return

        val div: Div = Div()
        div.add(H3("Ссылка на ветку:"))
        div.add(Anchor(branchLink, branchLink))
        urlBranchBox.add(div)
    }

    @Subscribe(id = "clearStatusCommit", subject = "clickListener")
    private fun onClearStatusCommitClick(event: ClickEvent<JmixButton>) {
        editedEntity.setStatus(StatusSheduler.NEW)
        dataManager.save(editedEntity)
    }




}