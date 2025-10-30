package com.company.commitet_jm.view.project;

import com.company.commitet_jm.sheduledJob.GitCloneTask;
import com.company.commitet_jm.entity.Project;
import com.company.commitet_jm.service.git.GitService;
import com.company.commitet_jm.view.main.MainView;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.HasValueAndElement;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import io.jmix.core.DataManager;
import io.jmix.core.FileStorageLocator;
import io.jmix.core.validation.group.UiCrossFieldChecks;
import io.jmix.flowui.Dialogs;
import io.jmix.flowui.action.SecuredBaseAction;
import io.jmix.flowui.component.UiComponentUtils;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.component.textfield.TypedTextField;
import io.jmix.flowui.component.validation.ValidationErrors;
import io.jmix.flowui.kit.action.Action;
import io.jmix.flowui.kit.action.ActionPerformedEvent;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.model.DataContext;
import io.jmix.flowui.model.InstanceContainer;
import io.jmix.flowui.model.InstanceLoader;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

@Route(value = "projects", layout = MainView.class)
@ViewController(id = "Project.list")
@ViewDescriptor(path = "project-list-view.xml")
@LookupComponent("projectsDataGrid")
@DialogMode(width = "64em")
public class ProjectListView extends StandardListView<Project> {
    @Autowired
    private FileStorageLocator fileStorageLocator;

    @Autowired
    private DataManager dataManager;

    @Autowired
    private GitService gitService;

    @ViewComponent
    private DataContext dataContext;

    @ViewComponent
    private CollectionContainer<Project> projectsDc;

    @ViewComponent
    private InstanceContainer<Project> projectDc;

    @ViewComponent
    private InstanceLoader<Project> projectDl;

    @ViewComponent
    private VerticalLayout listLayout;

    @ViewComponent
    private DataGrid<Project> projectsDataGrid;

    @ViewComponent
    private FormLayout form;

    @ViewComponent
    private HorizontalLayout detailActions;

    @ViewComponent
    private TypedTextField<String> urlRepoField;

    @ViewComponent
    private TypedTextField<String> localPathField;

    @ViewComponent
    private TypedTextField<String> defaultBranchField;

    @Autowired
    private Dialogs dialogs;

    @Subscribe
    public void onInit(InitEvent event) {
        projectsDataGrid.getActions().forEach(action -> {
            if (action instanceof SecuredBaseAction) {
                ((SecuredBaseAction) action).addEnabledRule(() -> listLayout.isEnabled());
            }
        });
    }

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        updateControls(false);
    }

    @Subscribe("projectsDataGrid.createAction")
    public void onProjectsDataGridCreateAction(ActionPerformedEvent event) {
        dataContext.clear();
        Project entity = dataContext.create(Project.class);
        projectDc.setItem(entity);
        updateControls(true);
    }

    @Subscribe("projectsDataGrid.editAction")
    public void onProjectsDataGridEditAction(ActionPerformedEvent event) {
        updateControls(true);
    }

    @Subscribe("saveButton")
    public void onSaveButtonClick(ClickEvent<JmixButton> event) {
        Project item = projectDc.getItem();
        ValidationErrors validationErrors = validateView(item);
        if (!validationErrors.isEmpty()) {
            ViewValidation viewValidation = getViewValidation();
            viewValidation.showValidationErrors(validationErrors);
            viewValidation.focusProblemComponent(validationErrors);
            return;
        }
        dataContext.save();
        projectsDc.replaceItem(item);
        updateControls(false);
    }

    @Subscribe("cloneGitButton")
    public void cloneGitButtonClick(ClickEvent<JmixButton> event) {
        GitCloneTask task = new GitCloneTask(dataManager, fileStorageLocator, gitService);
        task.setUrlRepo(urlRepoField.getValue());
        task.setLocalPath(localPathField.getValue());
        task.setDefaultBranch(defaultBranchField.getValue());

        dialogs.createBackgroundTaskDialog(task)
                .withHeader("Клонирование репозитория")
                .withText("Подождите, идет клонирование...")
                .open();
    }

    @Subscribe("cancelButton")
    public void onCancelButtonClick(ClickEvent<JmixButton> event) {
        dataContext.clear();
        projectDc.setItem(null);
        projectDl.load();
        updateControls(false);
    }

    @Subscribe(id = "projectsDc", target = Target.DATA_CONTAINER)
    public void onProjectsDcItemChange(InstanceContainer.ItemChangeEvent<Project> event) {
        Project entity = event.getItem();
        dataContext.clear();
        if (entity != null) {
            projectDl.setEntityId(entity.getId());
            projectDl.load();
        } else {
            projectDl.setEntityId(null);
            projectDc.setItem(null);
        }
        updateControls(false);
    }

    private ValidationErrors validateView(Project entity) {
        ViewValidation viewValidation = getViewValidation();
        ValidationErrors validationErrors = viewValidation.validateUiComponents(form);
        if (!validationErrors.isEmpty()) {
            return validationErrors;
        }
        validationErrors.addAll(viewValidation.validateBeanGroup(UiCrossFieldChecks.class, entity));
        return validationErrors;
    }

    private void updateControls(boolean editing) {
        UiComponentUtils.getComponents(form).forEach(component -> {
            if (component instanceof HasValueAndElement) {
                ((HasValueAndElement<?, ?>) component).setReadOnly(!editing);
            }
        });
        detailActions.setVisible(editing);
        listLayout.setEnabled(!editing);
        projectsDataGrid.getActions().forEach(Action::refreshState);
    }

    private ViewValidation getViewValidation() {
        return getApplicationContext().getBean(ViewValidation.class);
    }
}