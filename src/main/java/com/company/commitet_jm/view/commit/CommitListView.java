package com.company.commitet_jm.view.commit;

import com.company.commitet_jm.entity.Commit;
import com.company.commitet_jm.entity.StatusSheduler;
import com.company.commitet_jm.entity.User;
import com.company.commitet_jm.service.git.GitService;
import com.company.commitet_jm.view.main.MainView;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.router.Route;
import io.jmix.core.DataManager;
import io.jmix.core.FileStorageLocator;
import io.jmix.core.Messages;
import io.jmix.core.security.CurrentAuthentication;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.model.CollectionLoader;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

@Route(value = "commits", layout = MainView.class)
@ViewController("Commit_.list")
@ViewDescriptor("commit-list-view.xml")
@LookupComponent("commitsDataGrid")
@DialogMode(width = "64em")
public class CommitListView extends StandardListView<Commit> {
    @Autowired
    protected UiComponents uiComponents;

    @Autowired
    protected Messages messages;

    @Autowired
    private FileStorageLocator fileStorageLocator;

    @Autowired
    private CurrentAuthentication currentAuthentication;

    @Autowired
    private DataManager dataManager;

    @Autowired
    private GitService gitService;

    @ViewComponent
    private CollectionLoader<Commit> commitsDl;

    @Subscribe(id = "commitsDc", target = Target.DATA_CONTAINER)
    private void onCommitsDcCollectionChange(CollectionContainer.CollectionChangeEvent<Commit> event) {
    }

    @Subscribe(id = "CreateCommitButton")
    private void onCreateCommitButtonClick(ClickEvent<JmixButton> event) {
        gitService.createCommit();
    }

    @Subscribe
    private void onInit(InitEvent event) {
        User cUser = (User) currentAuthentication.getUser();
        if (cUser.getIsAdmin() == Boolean.TRUE) {
            return;
        }
        if (commitsDl != null) {
            commitsDl.setParameter("user", currentAuthentication.getUser());
        }
    }

    @Supply(to = "commitsDataGrid.status", subject = "renderer")
    private Renderer<Commit> commitsDataGridStatusRenderer() {
        return new ComponentRenderer<>(this::createGradeComponent, this::gradeComponentUpdater);
    }

    public Span createGradeComponent() {
        Span span = uiComponents.create(Span.class);
        span.getElement().getThemeList().add("badge");

        return span;
    }

    public void gradeComponentUpdater(Span span, Commit commit) {
        if (commit.getStatusEnum() != null) {
            span.setText(messages.getMessage(StatusSheduler.class, commit.getStatusEnum().toString()));

            switch (commit.getStatusEnum()) {
                case NEW:
                    span.getElement().getThemeList().add("primary");
                    break;
                case PROCESSED:
                    span.getElement().getThemeList().add("contrast");
                    break;
                case ERROR:
                    span.getElement().getThemeList().add("error");
                    break;
                case COMPLETE:
                    span.getElement().getThemeList().add("success");
                    break;
                default:
                    span.getElement().getThemeList().add("primary");
                    break;
            }
        } else {
            span.setText("No data");
        }
    }
}