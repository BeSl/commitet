package com.company.commitet_jm.view.appsettings;

import com.company.commitet_jm.entity.AppSettings;
import com.company.commitet_jm.view.main.MainView;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.view.*;

@Route(value = "app-settingses", layout = MainView.class)
@ViewController("AppSettings.list")
@ViewDescriptor("app-settings-list-view.xml")
@LookupComponent("appSettingsesDataGrid")
@DialogMode(width = "64em")
public class AppSettingsListView extends StandardListView<AppSettings> {
}