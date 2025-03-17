package com.company.commitet_jm.view.commit

import com.company.commitet_jm.entity.Commit
import com.company.commitet_jm.entity.Project
import com.company.commitet_jm.view.main.MainView
import com.company.commitet_jm.view.project.ProjectListSelect
import com.vaadin.flow.component.ClickEvent
import com.vaadin.flow.router.Route
import io.jmix.core.DataManager
import io.jmix.flowui.DialogWindows
import io.jmix.flowui.action.entitypicker.EntityLookupAction
import io.jmix.flowui.kit.action.ActionPerformedEvent
import io.jmix.flowui.kit.component.button.JmixButton
import io.jmix.flowui.view.*
import io.jmix.flowui.view.builder.LookupWindowBuilderProcessor
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import java.util.function.Consumer


@Route(value = "commits/:id", layout = MainView::class)
@ViewController(id = "Commit_.detail")
@ViewDescriptor(path = "commit-detail-view.xml")
@EditedEntityContainer("commitDc")
class CommitDetailView : StandardDetailView<Commit>() {
    @Autowired
    private lateinit var dataManager: DataManager

    @field:ViewComponent("projectField.entityLookupAction")
    private lateinit var projectFieldEntityLookupAction: EntityLookupAction<Project>

    @Autowired
    private lateinit var dialogWindows: DialogWindows

    @Autowired
    private lateinit var lookupWindowBuilderProcessor: LookupWindowBuilderProcessor


    companion object {
        private  val log = LoggerFactory.getLogger(CommitDetailView::class.java)
    }

    @Subscribe(id = "saveAndCloseButton", subject = "clickListener")
    private fun onSaveAndCloseButtonClick(event: ClickEvent<JmixButton>) {
        log.trace("hello")

    }
}