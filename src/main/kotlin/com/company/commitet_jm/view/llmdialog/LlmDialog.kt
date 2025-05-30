package com.company.commitet_jm.view.llmdialog


import com.company.commitet_jm.view.main.MainView
import com.vaadin.flow.router.Route
import io.jmix.flowui.view.StandardView
import io.jmix.flowui.view.ViewController
import io.jmix.flowui.view.ViewDescriptor

@Route(value = "llm-dialog", layout = MainView::class)
@ViewController(id = "LlmDialog")
@ViewDescriptor(path = "llm-dialog.xml")
class LlmDialog : StandardView() {
}