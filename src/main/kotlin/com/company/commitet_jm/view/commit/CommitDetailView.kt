package com.company.commitet_jm.view.commit

import com.company.commitet_jm.entity.Commit
import com.company.commitet_jm.entity.Project
import com.company.commitet_jm.entity.StatusSheduler
import com.company.commitet_jm.entity.User
import com.company.commitet_jm.view.main.MainView
import com.vaadin.flow.component.ClickEvent
import com.vaadin.flow.component.DetachEvent
import com.vaadin.flow.router.Route
import io.jmix.core.DataManager
import io.jmix.core.TimeSource
import io.jmix.core.security.CurrentAuthentication
import io.jmix.flowui.DialogWindows
import io.jmix.flowui.action.entitypicker.EntityLookupAction
import io.jmix.flowui.component.delegate.TextAreaFieldDelegate
import io.jmix.flowui.component.textarea.JmixTextArea
import io.jmix.flowui.component.textfield.TypedTextField
import io.jmix.flowui.kit.component.button.JmixButton
import io.jmix.flowui.view.*
import io.jmix.flowui.view.builder.LookupWindowBuilderProcessor
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import java.awt.TextArea
import java.time.LocalDateTime
import java.time.LocalDateTime.now
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

    @Subscribe(id = "saveAndCloseButton", subject = "clickListener")
    private fun onSaveAndCloseButtonClick(event: ClickEvent<JmixButton>) {
        log.info("save commit")
    }

}