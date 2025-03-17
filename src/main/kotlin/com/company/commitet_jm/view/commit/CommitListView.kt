package com.company.commitet_jm.view.commit

import com.company.commitet_jm.entity.Commit
import com.company.commitet_jm.view.main.MainView
import com.vaadin.flow.router.Route
import io.jmix.flowui.view.*


@Route(value = "commits", layout = MainView::class)
@ViewController(id = "Commit_.list")
@ViewDescriptor(path = "commit-list-view.xml")
@LookupComponent("commitsDataGrid")
@DialogMode(width = "64em")
class CommitListView : StandardListView<Commit>() {
}