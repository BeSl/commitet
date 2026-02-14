package com.company.commitet_jm.view.userexternalid

import com.company.commitet_jm.entity.UserExternalId
import com.company.commitet_jm.view.main.MainView
import com.vaadin.flow.router.Route
import io.jmix.flowui.view.*
import java.time.LocalDateTime

@Route(value = "userexternalids/:id", layout = MainView::class)
@ViewController(id = "UserExternalId.detail")
@ViewDescriptor(path = "user-external-id-detail-view.xml")
@EditedEntityContainer("userExternalIdDc")
class UserExternalIdDetailView : StandardDetailView<UserExternalId>() {

    @Subscribe
    fun onInitEntity(event: InitEntityEvent<UserExternalId>) {
        editedEntity.dateCreated = LocalDateTime.now()
    }
}
