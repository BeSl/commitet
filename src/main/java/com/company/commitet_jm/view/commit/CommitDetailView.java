package com.company.commitet_jm.view.commit;

import com.company.commitet_jm.entity.*;
import com.company.commitet_jm.service.git.GitService;
import com.company.commitet_jm.view.main.MainView;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.Route;
import io.jmix.core.DataManager;
import io.jmix.core.FileStorageLocator;
import io.jmix.core.TimeSource;
import io.jmix.core.security.CurrentAuthentication;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.component.textarea.JmixTextArea;
import io.jmix.flowui.component.textfield.TypedTextField;
import io.jmix.flowui.component.valuepicker.EntityPicker;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.view.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

@Route(value = "commits/:id", layout = MainView.class)
@ViewController("Commit_.detail")
@ViewDescriptor("commit-detail-view.xml")
@EditedEntityContainer("commitDc")
public class CommitDetailView extends StandardDetailView<Commit> {

    private static final Logger log = LoggerFactory.getLogger(CommitDetailView.class);

    @Autowired
    private TimeSource timeSource;

    @Autowired
    private CurrentAuthentication currentAuthentication;

    @Autowired
    private DataManager dataManager;

    @Autowired
    private GitService gitService;

    @ViewComponent
    private JmixTextArea errorInfoField;

    @ViewComponent
    private TypedTextField<String> statusField;

    @ViewComponent
    private JmixTextArea descriptionField;

    @ViewComponent
    private TypedTextField<String> taskNumField;

    @ViewComponent
    private EntityPicker<Project> projectField;

    @ViewComponent
    private DataGrid<FileCommit> filesDataGrid;

    @ViewComponent
    private HorizontalLayout buttonsPanel;

    @ViewComponent
    private Button clearStatusCommit;

    @ViewComponent
    private Button startAnalyzeButton;

    @ViewComponent
    private Button uploadFilesButton;

    @ViewComponent
    private HorizontalLayout urlBranchBox;

    @Autowired
    private FileStorageLocator fileStorageLocator;

    @Subscribe
    private void onInitEntity(InitEntityEvent<Commit> event) {
        errorInfoField.setVisible(false);
        Commit recommit = event.getEntity();

        recommit.setDateCreated(timeSource.now().toLocalDateTime());
        recommit.setStatus(StatusSheduler.NEW);
        recommit.setAuthor((User) currentAuthentication.getUser());
    }

    @Subscribe
    private void onInit(InitEvent event) {
    }

    @Subscribe(id = "saveAndCloseButton", subject = "clickListener")
    private void onSaveAndCloseButtonClick(ClickEvent<JmixButton> event) {
        log.info("save commit");
    }

    @Subscribe
    private void onReady(ReadyEvent event) {
        initHtmlContent(editedEntity.getUrlBranch() != null ? editedEntity.getUrlBranch() : "");
        User cUser = (User) currentAuthentication.getUser();
        if (cUser.getIsAdmin() == Boolean.TRUE) {
            clearStatusCommit.setVisible(true);
            startAnalyzeButton.setVisible(true);
            uploadFilesButton.setVisible(true);
            return;
        }
        if (statusField.getValue() != null && 
            (statusField.getValue().toLowerCase().equals("new") ||
             statusField.getValue().toLowerCase().equals("новый"))) {
            return;
        }

        descriptionField.setEnabled(false);
        taskNumField.setEnabled(false);
        projectField.setEnabled(false);
        filesDataGrid.setEnabled(false);
        buttonsPanel.setVisible(false);
    }

    protected void initHtmlContent(String branchLink) {
        if (branchLink.isEmpty()) return;

        Div div = new Div();
        div.add(new H3("Ссылка на ветку:"));
        div.add(new Anchor(branchLink, branchLink));
        urlBranchBox.add(div);
    }

    @Subscribe(id = "clearStatusCommit", subject = "clickListener")
    private void onClearStatusCommitClick(ClickEvent<JmixButton> event) {
        editedEntity.setStatus(StatusSheduler.NEW);
        dataManager.save(editedEntity);
    }

    @Subscribe(id = "startAnalyzeButton", subject = "clickListener")
    private void onStartAnalyzeButtonCommitClick(ClickEvent<JmixButton> event) {
    }

    @Subscribe(id = "uploadFilesButton", subject = "clickListener")
    private void onUploadFilesButtonCommitClick(ClickEvent<JmixButton> event) {
        gitService.createCommit();
    }
}