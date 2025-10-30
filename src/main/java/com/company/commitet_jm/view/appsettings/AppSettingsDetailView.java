package com.company.commitet_jm.view.appsettings;

import com.company.commitet_jm.entity.AppSettings;
import com.company.commitet_jm.view.main.MainView;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.view.EditedEntityContainer;
import io.jmix.flowui.view.StandardDetailView;
import io.jmix.flowui.view.ViewController;
import io.jmix.flowui.view.ViewDescriptor;

@Route(value = "app-settingses/:id", layout = MainView.class)
@ViewController("AppSettings.detail")
@ViewDescriptor("app-settings-detail-view.xml")
@EditedEntityContainer("appSettingsDc")
public class AppSettingsDetailView extends StandardDetailView<AppSettings> {
}