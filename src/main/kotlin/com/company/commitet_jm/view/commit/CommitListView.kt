package com.company.commitet_jm.view.commit

import com.company.commitet_jm.entity.Commit
import com.company.commitet_jm.view.main.MainView
import com.vaadin.flow.router.Route
import io.jmix.flowui.model.CollectionContainer
import io.jmix.flowui.view.*
import io.jmix.flowui.view.Target


@Route(value = "commits", layout = MainView::class)
@ViewController(id = "Commit_.list")
@ViewDescriptor(path = "commit-list-view.xml")
@LookupComponent("commitsDataGrid")
@DialogMode(width = "64em")
class CommitListView : StandardListView<Commit>() {
    @Subscribe(id = "commitsDc", target = Target.DATA_CONTAINER)
    private fun onCommitsDcCollectionChange(event: CollectionContainer.CollectionChangeEvent<Commit>) {

    }
}