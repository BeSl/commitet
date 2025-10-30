package com.company.commitet_jm.view.user;

import com.company.commitet_jm.entity.User;
import com.company.commitet_jm.view.main.MainView;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.Route;
import io.jmix.core.security.CurrentAuthentication;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.model.CollectionLoader;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

@Route(value = "users", layout = MainView.class)
@ViewController(id = "User.list")
@ViewDescriptor(path = "user-list-view.xml")
@LookupComponent("usersDataGrid")
@DialogMode(width = "64em")
public class UserListView extends StandardListView<User> {
    @Autowired
    private CurrentAuthentication currentAuthentication;

    @ViewComponent
    private CollectionContainer<User> usersDc;

    @ViewComponent
    private CollectionLoader<User> usersDl;

    @ViewComponent
    private JmixButton createButton;

    @ViewComponent
    private HorizontalLayout btnAdmin;

    @Subscribe
    private void onInit(InitEvent event) {
        User cUser = (User) currentAuthentication.getUser();
        if (cUser != null && cUser.getIsAdmin() == true) {
            return;
        }
        usersDl.setParameter("id", ((User) currentAuthentication.getUser()).getId());
        btnAdmin.setVisible(false);
    }
}