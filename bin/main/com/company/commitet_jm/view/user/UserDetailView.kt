package com.company.commitet_jm.view.user

import com.company.commitet_jm.entity.User
import com.company.commitet_jm.entity.UserExternalId
import com.company.commitet_jm.view.main.MainView
import com.vaadin.flow.component.combobox.ComboBox
import com.vaadin.flow.component.notification.Notification
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.textfield.PasswordField
import com.vaadin.flow.router.Route
import io.jmix.core.EntityStates
import io.jmix.core.MessageTools
import io.jmix.core.security.CurrentAuthentication
import io.jmix.flowui.Notifications
import io.jmix.flowui.component.grid.DataGrid
import io.jmix.flowui.component.textfield.TypedTextField
import io.jmix.flowui.view.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.crypto.password.PasswordEncoder

import java.util.*

@Route(value = "users/:id", layout = MainView::class)
@ViewController(id = "User.detail")
@ViewDescriptor(path = "user-detail-view.xml")
@EditedEntityContainer("userDc")
open class UserDetailView : StandardDetailView<User>() {

    @ViewComponent
    private lateinit var usernameField: TypedTextField<String>

    @ViewComponent
    private lateinit var passwordField: PasswordField

    @ViewComponent
    private lateinit var confirmPasswordField: PasswordField

//    @ViewComponent
//    private lateinit var timeZoneField: ComboBox<String>

    @ViewComponent
    private lateinit var externalIdsDataGrid: DataGrid<UserExternalId>

    @ViewComponent
    private lateinit var externalIdsPanel: VerticalLayout

    @ViewComponent
    private lateinit var messageBundle: MessageBundle

    @Autowired
    private lateinit var messageTools: MessageTools

    @Autowired
    private lateinit var notifications: Notifications

    @Autowired
    private lateinit var entityStates: EntityStates

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    private lateinit var currentAuthentication: CurrentAuthentication

    @Subscribe
    fun onInit(event: InitEvent) {
//        timeZoneField.setItems(listOf(*TimeZone.getAvailableIDs()))
    }

    @Subscribe
    fun onInitEntity(event: InitEntityEvent<User>) {
        usernameField.isReadOnly = false
        passwordField.isVisible = true
        confirmPasswordField.isVisible = true
    }

    @Subscribe
    fun onReady(event: ReadyEvent) {
        if (entityStates.isNew(editedEntity)) {
            usernameField.focus()
        }

        val cUser = currentAuthentication.user as User
        // Показываем панель внешних ID только администраторам
        externalIdsPanel.isVisible = cUser.isAdmin == true
    }

    @Subscribe
    fun onValidation(event: ValidationEvent) {
        if (entityStates.isNew(editedEntity)
                && !Objects.equals(passwordField.value, confirmPasswordField.value)) {
            event.errors.add(messageBundle.getMessage("passwordsDoNotMatch"))
        }
    }

    @Subscribe
    fun onBeforeSave(event: BeforeSaveEvent) {
        if (entityStates.isNew(editedEntity)) {
            editedEntity.password = passwordEncoder.encode(passwordField.value)

            val entityCaption = messageTools.getEntityCaption(editedEntityContainer.entityMetaClass)
            notifications.create(messageBundle.formatMessage("noAssignedRolesNotification", entityCaption))
                .withType(Notifications.Type.WARNING)
                .withPosition(Notification.Position.TOP_END)
                .show()
        }
    }
}