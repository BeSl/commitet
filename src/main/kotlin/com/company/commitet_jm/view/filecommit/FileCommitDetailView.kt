package com.company.commitet_jm.view.filecommit

import com.company.commitet_jm.entity.FileCommit
import com.company.commitet_jm.view.main.MainView
import com.vaadin.flow.router.Route
import io.jmix.flowui.view.EditedEntityContainer
import io.jmix.flowui.view.StandardDetailView
import io.jmix.flowui.view.ViewController
import io.jmix.flowui.view.ViewDescriptor

@Route(value = "file-commits/:id", layout = MainView::class)
@ViewController(id = "FileCommit.detail")
@ViewDescriptor(path = "file-commit-detail-view.xml")
@EditedEntityContainer("fileCommitDc")
class FileCommitDetailView : StandardDetailView<FileCommit>() {
}