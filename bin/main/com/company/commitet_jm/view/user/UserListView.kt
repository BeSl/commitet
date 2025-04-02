package com.company.commitet_jm.view.user

import com.company.commitet_jm.entity.User
import com.company.commitet_jm.view.main.MainView
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.router.Route
import io.jmix.core.security.CurrentAuthentication
import io.jmix.flowui.kit.component.button.JmixButton
import io.jmix.flowui.model.CollectionContainer
import io.jmix.flowui.model.CollectionLoader
import io.jmix.flowui.view.*
import org.springframework.beans.factory.annotation.Autowired

@Route(value = "users", layout = MainView::class)
@ViewController(id = "User.list")
@ViewDescriptor(path = "user-list-view.xml")
@LookupComponent("usersDataGrid")
@DialogMode(width = "64em")
open class UserListView : StandardListView<User>() {
    @Autowired
    private lateinit var currentAuthentication: CurrentAuthentication

    @ViewComponent
    private lateinit var usersDc: CollectionContainer<User>

    @ViewComponent
    private lateinit var usersDl: CollectionLoader<User>

    @ViewComponent
    private lateinit var createButton: JmixButton

    @ViewComponent
    private lateinit var btnAdmin: HorizontalLayout

    @Subscribe
    private fun onInit(event: InitEvent) {
        val cUser = currentAuthentication.user as User
        if (cUser?.isAdmin == true) {
            return
        }
        usersDl?.setParameter("id", (currentAuthentication.user as User).id)
        btnAdmin.isVisible = false
    }
}
