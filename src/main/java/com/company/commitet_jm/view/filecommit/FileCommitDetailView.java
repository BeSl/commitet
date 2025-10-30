package com.company.commitet_jm.view.filecommit;

import com.company.commitet_jm.entity.FileCommit;
import com.company.commitet_jm.entity.TypesFiles;
import com.company.commitet_jm.view.main.MainView;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.component.select.JmixSelect;
import io.jmix.flowui.component.textfield.TypedTextField;
import io.jmix.flowui.component.upload.FileStorageUploadField;
import io.jmix.flowui.kit.component.upload.event.FileUploadSucceededEvent;
import io.jmix.flowui.view.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Locale;

@Route(value = "file-commits/:id", layout = MainView.class)
@ViewController("FileCommit.detail")
@ViewDescriptor("file-commit-detail-view.xml")
@EditedEntityContainer("fileCommitDc")
public class FileCommitDetailView extends StandardDetailView<FileCommit> {
    private static final Logger log = LoggerFactory.getLogger(FileCommitDetailView.class);

    @ViewComponent
    private JmixSelect<TypesFiles> typeField;

    @ViewComponent
    private TypedTextField<String> nameField;

    @Subscribe("dataField")
    private void onDataFieldFileUploadSucceeded(FileUploadSucceededEvent<FileStorageUploadField> event) {
        log.info("File uploaded: {}", event.getFileName());
        String sepFile = event.getFileName().substring(event.getFileName().lastIndexOf('.') + 1).toLowerCase(Locale.getDefault());
        switch (sepFile) {
            case "epf":
                typeField.setValue(TypesFiles.DATAPROCESSOR);
                break;
            case "erf":
                typeField.setValue(TypesFiles.REPORT);
                break;
            case "bsl":
                typeField.setValue(TypesFiles.EXTERNAL_CODE);
                break;
            case "xml":
                typeField.setValue(TypesFiles.EXCHANGE_RULES);
                break;
            default:
                log.error("Type files not detected");
                break;
        }
        nameField.setValue(event.getFileName());
    }
}