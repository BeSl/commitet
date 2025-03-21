package com.company.commitet_jm.view.commit

import com.company.commitet_jm.entity.Commit
import com.company.commitet_jm.entity.Project
import com.company.commitet_jm.entity.StatusSheduler
import com.company.commitet_jm.entity.User
import com.company.commitet_jm.view.main.MainView
import com.vaadin.flow.component.ClickEvent
import com.vaadin.flow.router.Route
import io.jmix.core.DataManager
import io.jmix.core.security.CurrentAuthentication
import io.jmix.flowui.DialogWindows
import io.jmix.flowui.action.entitypicker.EntityLookupAction
import io.jmix.flowui.component.textfield.TypedTextField
import io.jmix.flowui.kit.component.button.JmixButton
import io.jmix.flowui.view.*
import io.jmix.flowui.view.builder.LookupWindowBuilderProcessor
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired


@Route(value = "commits/:id", layout = MainView::class)
@ViewController(id = "Commit_.detail")
@ViewDescriptor(path = "commit-detail-view.xml")
@EditedEntityContainer("commitDc")
class CommitDetailView : StandardDetailView<Commit>() {
    @Autowired
    private lateinit var currentAuthentication: CurrentAuthentication

    @Subscribe
    private fun onBeforeSave(event: BeforeSaveEvent) {
        println("test")

    }



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

    @Subscribe
    private fun onInitEntity(event: InitEntityEvent<Commit>) {

        var recommit = event.entity
        recommit.setStatus(StatusSheduler.NEW)
        recommit.author= currentAuthentication.getUser() as User

    }

    @Subscribe
    private fun onAfterSave(event: AfterSaveEvent) {
//        var commit = event.dataContext as Commit
//
//        if (commit.author == null) {
//            commit.author = currentAuthentication.getUser() as User
//        }
    }

    @Subscribe(id = "saveAndCloseButton", subject = "clickListener")
    private fun onSaveAndCloseButtonClick(event: ClickEvent<JmixButton>) {
        log.trace("hello")

    }
}