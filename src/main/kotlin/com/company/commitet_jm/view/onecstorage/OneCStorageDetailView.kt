package com.company.commitet_jm.view.onecstorage

import com.company.commitet_jm.entity.OneCStorage
import com.company.commitet_jm.service.ones.OneCStorageService
import com.company.commitet_jm.view.main.MainView
import com.vaadin.flow.component.ClickEvent
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.router.Route
import io.jmix.flowui.kit.component.button.JmixButton
import io.jmix.flowui.view.*
import org.springframework.stereotype.Component

@Route(value = "one-c-storages/:id", layout = MainView::class)
@ViewController(id = "OneCStorage.detail")
@ViewDescriptor(path = "one-c-storage-detail-view.xml")
@EditedEntityContainer("oneCStorageDc")
class OneCStorageDetailView : StandardDetailView<OneCStorage>() {

    @ViewComponent
    lateinit var notCommandBox: VerticalLayout
    @ViewComponent
    lateinit var createStorageBox: VerticalLayout
    @ViewComponent
    lateinit var addUserStorageBox: VerticalLayout
    @ViewComponent
    lateinit var copyUserStorageBox: VerticalLayout
    @ViewComponent
    lateinit var historyStorageBox: VerticalLayout
    @ViewComponent
    lateinit var cmd_param: VerticalLayout
    @ViewComponent
    lateinit var executeCommandButton: Button
    private var selectedCommand: String=""

    @Subscribe(id = "createStorageButtonClick", subject = "clickListener")
    private fun onCreateStorageButtonClickClick(event: ClickEvent<JmixButton>) {

        changeVisibleLayout(createStorageBox)
//        selectedCommand =
//        notCommandBox.isVisible = false
//        createStorageBox.isVisible
//        val storage = OneCStorageService()
//        storage.createOneCStorage(editedEntity)
    }

    @Subscribe(id = "historyStorageButton", subject = "clickListener")
    private fun historyStorageButtonClick(event: ClickEvent<JmixButton>) {
        changeVisibleLayout(historyStorageBox)
    }

    @Subscribe(id = "addStorageUseButton", subject = "clickListener")
    private fun addStorageUseButtonClickClick(event: ClickEvent<JmixButton>) {
        changeVisibleLayout(addUserStorageBox)
    }

    @Subscribe(id = "copyUsersStorageButton", subject = "clickListener")
    private fun copyUsersStorageButtonClick(event: ClickEvent<JmixButton>) {
        changeVisibleLayout(copyUserStorageBox)
    }


    fun changeVisibleLayout(visCompoment: VerticalLayout){

        cmd_param.children.forEach { Component ->
            if (Component!=visCompoment){
                Component.isVisible = false
            }
        }
        visCompoment.isVisible = true
        selectedCommand = visCompoment.id.toString()
        executeCommandButton.isVisible = true

    }

}