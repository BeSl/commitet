package com.company.commitet_jm.view.platform

import com.company.commitet_jm.entity.Platform
import com.company.commitet_jm.view.main.MainView
import com.vaadin.flow.router.Route
import io.jmix.flowui.view.EditedEntityContainer
import io.jmix.flowui.view.StandardDetailView
import io.jmix.flowui.view.ViewController
import io.jmix.flowui.view.ViewDescriptor

@Route(value = "platforms/:id", layout = MainView::class)
@ViewController(id = "Platform.detail")
@ViewDescriptor(path = "platform-detail-view.xml")
@EditedEntityContainer("platformDc")
class PlatformDetailView : StandardDetailView<Platform>() {
}