package com.company.commitet_jm.view.projectexternalid

import com.company.commitet_jm.entity.ProjectExternalId
import com.company.commitet_jm.view.main.MainView
import com.vaadin.flow.router.Route
import io.jmix.flowui.view.*
import java.time.LocalDateTime

@Route(value = "projectexternalids/:id", layout = MainView::class)
@ViewController(id = "ProjectExternalId.detail")
@ViewDescriptor(path = "project-external-id-detail-view.xml")
@EditedEntityContainer("projectExternalIdDc")
class ProjectExternalIdDetailView : StandardDetailView<ProjectExternalId>() {

    @Subscribe
    fun onInitEntity(event: InitEntityEvent<ProjectExternalId>) {
        editedEntity.dateCreated = LocalDateTime.now()
    }
}
