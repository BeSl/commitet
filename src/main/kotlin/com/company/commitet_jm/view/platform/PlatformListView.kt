package com.company.commitet_jm.view.platform

import com.company.commitet_jm.entity.Platform
import com.company.commitet_jm.view.main.MainView
import com.vaadin.flow.router.Route
import io.jmix.flowui.view.*


@Route(value = "platforms", layout = MainView::class)
@ViewController(id = "Platform.list")
@ViewDescriptor(path = "platform-list-view.xml")
@LookupComponent("platformsDataGrid")
@DialogMode(width = "64em")
class PlatformListView : StandardListView<Platform>() {
}