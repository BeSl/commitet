package com.company.commitet_jm.view.filecommit

import com.company.commitet_jm.entity.FileCommit
import com.company.commitet_jm.entity.TypesFiles
import com.company.commitet_jm.view.main.MainView
import com.vaadin.flow.component.select.Select
import com.vaadin.flow.component.shared.HasClientValidation
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.router.Route
import io.jmix.flowui.component.select.JmixSelect
import io.jmix.flowui.component.textfield.TypedTextField
import io.jmix.flowui.component.upload.FileStorageUploadField
import io.jmix.flowui.kit.component.upload.event.FileUploadSucceededEvent
import io.jmix.flowui.view.*
import org.slf4j.LoggerFactory
import java.util.*

@Route(value = "file-commits/:id", layout = MainView::class)
@ViewController(id = "FileCommit.detail")
@ViewDescriptor(path = "file-commit-detail-view.xml")
@EditedEntityContainer("fileCommitDc")
class FileCommitDetailView : StandardDetailView<FileCommit>() {
    companion object {
        private  val log = LoggerFactory.getLogger(FileCommitDetailView::class.java)
    }

    @ViewComponent
    private lateinit var typeField: JmixSelect<TypesFiles>

    @ViewComponent
    private lateinit var nameField: TypedTextField<String>

    @Subscribe("dataField")
    private fun onDataFieldFileUploadSucceeded(event: FileUploadSucceededEvent<FileStorageUploadField>) {
        log.info("File uploaded: {}", event.fileName)
        val sepFile = event.fileName.split(".").last().lowercase(Locale.getDefault())
        when (sepFile) {
            "epf" -> typeField.value = TypesFiles.DATAPROCESSOR
            "erf" -> typeField.value = TypesFiles.REPORT
            "bsl" -> typeField.value = TypesFiles.EXTERNAL_CODE
            else -> log.error("Type files not detected")
        }
        nameField.value = event.fileName
    }

    @Subscribe("typeField")
    private fun onTypeFieldClientValidated(event: HasClientValidation.ClientValidatedEvent) {

    }
}