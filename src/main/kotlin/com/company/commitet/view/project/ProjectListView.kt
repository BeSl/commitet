package com.company.commitet.view.project

import com.company.commitet.entity.Project
import com.company.commitet.view.main.MainView
import com.vaadin.flow.router.Route
import io.jmix.flowui.view.*


@Route(value = "projects", layout = MainView::class)
@ViewController(id = "Project.list")
@ViewDescriptor(path = "project-list-view.xml")
@LookupComponent("projectsDataGrid")
@DialogMode(width = "64em")
class ProjectListView : StandardListView<Project>() {
}