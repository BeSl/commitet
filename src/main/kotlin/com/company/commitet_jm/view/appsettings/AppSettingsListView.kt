package com.company.commitet_jm.view.appsettings

import com.company.commitet_jm.entity.AppSettings
import com.company.commitet_jm.view.main.MainView
import com.vaadin.flow.router.Route
import io.jmix.flowui.view.*


@Route(value = "app-settingses", layout = MainView::class)
@ViewController(id = "AppSettings.list")
@ViewDescriptor(path = "app-settings-list-view.xml")
@LookupComponent("appSettingsesDataGrid")
@DialogMode(width = "64em")
class AppSettingsListView : StandardListView<AppSettings>() {
}