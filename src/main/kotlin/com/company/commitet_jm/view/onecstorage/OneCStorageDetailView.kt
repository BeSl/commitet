package com.company.commitet_jm.view.onecstorage

import com.company.commitet_jm.entity.OneCStorage
import com.company.commitet_jm.view.main.MainView
import com.vaadin.flow.router.Route
import io.jmix.flowui.view.EditedEntityContainer
import io.jmix.flowui.view.StandardDetailView
import io.jmix.flowui.view.ViewController
import io.jmix.flowui.view.ViewDescriptor

@Route(value = "one-c-storages/:id", layout = MainView::class)
@ViewController(id = "OneCStorage.detail")
@ViewDescriptor(path = "one-c-storage-detail-view.xml")
@EditedEntityContainer("oneCStorageDc")
class OneCStorageDetailView : StandardDetailView<OneCStorage>() {
}