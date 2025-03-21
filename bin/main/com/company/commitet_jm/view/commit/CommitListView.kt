package com.company.commitet_jm.view.commit

import com.company.commitet_jm.entity.Commit
import com.company.commitet_jm.service.GitWorker
import com.company.commitet_jm.view.main.MainView
import com.vaadin.flow.component.ClickEvent
import com.vaadin.flow.router.Route
import io.jmix.core.DataManager
import io.jmix.flowui.kit.component.button.JmixButton
import io.jmix.flowui.model.CollectionContainer
import io.jmix.flowui.view.*
import io.jmix.flowui.view.Target
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.beans.factory.annotation.Autowired


@Route(value = "commits", layout = MainView::class)
@ViewController(id = "Commit_.list")
@ViewDescriptor(path = "commit-list-view.xml")
@LookupComponent("commitsDataGrid")
@DialogMode(width = "64em")
class CommitListView : StandardListView<Commit>() {
    @Autowired
    private lateinit var dataManager: DataManager

    @Subscribe(id = "commitsDc", target = Target.DATA_CONTAINER)
    private fun onCommitsDcCollectionChange(event: CollectionContainer.CollectionChangeEvent<Commit>) {

    }

    @Subscribe(id = "CreateCommitButton")
    private fun onCreateCommitButtonClick(event: ClickEvent<JmixButton>) {
        val gitWorker = GitWorker(
            dataManager = dataManager
        )
        gitWorker.CreateCommit()
    }


}