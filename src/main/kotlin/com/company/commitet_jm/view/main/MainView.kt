package com.company.commitet_jm.view.main

import com.company.commitet_jm.entity.User
import com.vaadin.flow.component.html.H2
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.router.Route
import io.jmix.core.security.CurrentAuthentication
import io.jmix.flowui.app.main.StandardMainView
import io.jmix.flowui.view.Subscribe
import io.jmix.flowui.view.ViewComponent
import io.jmix.flowui.view.ViewController
import io.jmix.flowui.view.ViewDescriptor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.info.BuildProperties


@Route("")
@ViewController(id = "MainView")
@ViewDescriptor(path = "main-view.xml")
open class MainView : StandardMainView() {
    @Autowired
    private lateinit var currentAuthentication: CurrentAuthentication

    @ViewComponent
    private lateinit var welcomeMessage: H2

    @ViewComponent
    private lateinit var appVersion:Span

    @Autowired
    private val buildProperties: BuildProperties? = null

    @Subscribe
    private fun onInit(event: InitEvent) {
        val currentUser = currentAuthentication.user as User

        welcomeMessage.text = "${currentUser.firstName}, привет!!! "
        appVersion.text = "Версия сборки ${buildProperties?.version}"
    }
}
