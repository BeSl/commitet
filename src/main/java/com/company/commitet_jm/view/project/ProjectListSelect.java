package com.company.commitet_jm.view.project;

import com.company.commitet_jm.entity.Project;
import com.company.commitet_jm.view.main.MainView;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.view.*;

@Route(value = "projectsSelect", layout = MainView.class)
@ViewController(id = "Project.listSelect")
@ViewDescriptor(path = "project-list-select.xml")
@LookupComponent("projectsDataGrid")
@DialogMode(width = "64em")
public class ProjectListSelect extends StandardListView<Project> {

}