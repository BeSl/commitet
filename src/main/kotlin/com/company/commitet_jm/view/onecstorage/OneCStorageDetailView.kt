package com.company.commitet_jm.view.onecstorage

import com.company.commitet_jm.component.CommandFunction
import com.company.commitet_jm.entity.OneCStorage
import com.company.commitet_jm.service.ones.OneCStorageService
import com.company.commitet_jm.view.main.MainView
import com.vaadin.flow.component.ClickEvent
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.combobox.ComboBox
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.data.binder.ValidationException
import com.vaadin.flow.router.Route
import io.jmix.flowui.kit.component.button.JmixButton
import io.jmix.flowui.view.*
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate


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

    @ViewComponent
    lateinit var userRightsField: ComboBox<String>

    @ViewComponent
    lateinit var reportFormatField: ComboBox<String>

    private var selectedCommand: CommandFunction? = null

    @Autowired
    private lateinit var storage: OneCStorageService

    @Subscribe
    private fun onInit(event: InitEvent) {
        // Инициализация полей
        userRightsField.setItems("READ_ONLY", "FULL_ACCESS", "VERSION_MANAGEMENT")
        reportFormatField.setItems("TXT", "MXL")
    }



    @Subscribe(id = "createStorageButtonClick", subject = "clickListener")
    private fun onCreateStorageButtonClickClick(event: ClickEvent<JmixButton>) {

        changeVisibleLayout(createStorageBox){
            storage.createOneCStorage(editedEntity)
        }

    }

    @Subscribe(id = "historyStorageButton", subject = "clickListener")
    private fun historyStorageButtonClick(event: ClickEvent<JmixButton>) {
        changeVisibleLayout(historyStorageBox){
            historyStorage()
        }

    }

    @Subscribe(id = "addStorageUseButton", subject = "clickListener")
    private fun addStorageUseButtonClickClick(event: ClickEvent<JmixButton>) {
        changeVisibleLayout(addUserStorageBox){
            addUserStorage()
        }
    }

    @Subscribe(id = "copyUsersStorageButton", subject = "clickListener")
    private fun copyUsersStorageButtonClick(event: ClickEvent<JmixButton>) {
        changeVisibleLayout(copyUserStorageBox){
            copyUsersStorage()
        }
    }


    fun changeVisibleLayout(visCompoment: VerticalLayout, cmd : CommandFunction){

        cmd_param.children.forEach { Component ->
            if (Component!=visCompoment){
                Component.isVisible = false
            }
        }
        visCompoment.isVisible = true
        selectedCommand = cmd
        executeCommandButton.isVisible = true

    }

    @Subscribe(id = "executeCommandButton", subject = "clickListener")
    private fun onExecuteCommandButtonClick(event: ClickEvent<JmixButton>) {
        try {
            selectedCommand?.invoke()
        } catch (e: ValidationException) {
//            showNotification(e.message, NotificationVariant.LUMO_ERROR)
        } catch (e: Exception) {
//            showNotification("Ошибка выполнения: ${e.message}", NotificationVariant.LUMO_ERROR)
        }
    }

    fun historyStorage(){

    }
    fun addUserStorage(){

    }
    fun copyUsersStorage(){

    }

}

// Вспомогательные классы
data class HistoryOptions(
    val startVersion: Int? = null,
    val endVersion: Int? = null,
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val format: String = "TXT"
)

enum class UserRights {
    READ_ONLY,
    FULL_ACCESS,
    VERSION_MANAGEMENT
}