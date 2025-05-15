package com.company.commitet_jm.view.onecstorage

import com.company.commitet_jm.entity.OneCStorage
import com.company.commitet_jm.service.ones.OneCStorageService
import com.company.commitet_jm.view.main.MainView
import com.vaadin.flow.component.ClickEvent
import com.vaadin.flow.router.Route
import io.jmix.flowui.kit.component.button.JmixButton
import io.jmix.flowui.view.*

@Route(value = "one-c-storages/:id", layout = MainView::class)
@ViewController(id = "OneCStorage.detail")
@ViewDescriptor(path = "one-c-storage-detail-view.xml")
@EditedEntityContainer("oneCStorageDc")
class OneCStorageDetailView : StandardDetailView<OneCStorage>() {
    @Subscribe(id = "createStorageButtonClick", subject = "clickListener")
    private fun onCreateStorageButtonClickClick(event: ClickEvent<JmixButton>) {

        val storage = OneCStorageService()
        storage.createOneCStorage(editedEntity)
    }
}