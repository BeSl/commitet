package com.company.commitet_jm.view.onecstorage

import com.company.commitet_jm.entity.OneCStorage
import com.company.commitet_jm.view.main.MainView
import com.vaadin.flow.router.Route
import io.jmix.flowui.view.*


@Route(value = "one-c-storages", layout = MainView::class)
@ViewController(id = "OneCStorage.list")
@ViewDescriptor(path = "one-c-storage-list-view.xml")
@LookupComponent("oneCStoragesDataGrid")
@DialogMode(width = "64em")
class OneCStorageListView : StandardListView<OneCStorage>() {
}