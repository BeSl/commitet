package com.company.commitet_jm.view.filecommit

import com.company.commitet_jm.entity.FileCommit
import com.company.commitet_jm.entity.TypesFiles
import com.company.commitet_jm.service.availablename.AvailableNameService
import com.company.commitet_jm.view.availablename.AvailableNameSelectionDialog
import com.company.commitet_jm.view.main.MainView
import com.vaadin.flow.router.Route
import io.jmix.flowui.Notifications
import io.jmix.flowui.component.select.JmixSelect
import io.jmix.flowui.component.textfield.TypedTextField
import io.jmix.flowui.component.upload.FileStorageUploadField
import io.jmix.flowui.kit.component.upload.event.FileUploadSucceededEvent
import io.jmix.flowui.view.*
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
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

    @ViewComponent
    private lateinit var codeField: TypedTextField<String>

    @Autowired
    private lateinit var availableNameService: AvailableNameService

    @Autowired
    private lateinit var availableNameDialog: AvailableNameSelectionDialog

    @Autowired
    private lateinit var notifications: Notifications

    private var validationInProgress = false

    @Subscribe("dataField")
    private fun onDataFieldFileUploadSucceeded(event: FileUploadSucceededEvent<FileStorageUploadField>) {
        log.info("File uploaded: {}", event.fileName)
        val sepFile = event.fileName.split(".").last().lowercase(Locale.getDefault())
        when (sepFile) {
            "epf" -> typeField.value = TypesFiles.DATAPROCESSOR
            "erf" -> typeField.value = TypesFiles.REPORT
            "bsl" -> typeField.value = TypesFiles.EXTERNAL_CODE
            "xml" -> typeField.value = TypesFiles.EXCHANGE_RULES
            else -> log.error("Type files not detected")
        }
        nameField.value = event.fileName
    }

    /**
     * Валидация имени файла перед сохранением.
     * Для обработок и отчетов проверяет наличие имени в списке доступных.
     */
    @Subscribe
    private fun onValidation(event: ValidationEvent) {
        // Избегаем циклической валидации
        if (validationInProgress) {
            return
        }

        val fileCommit = editedEntity
        val fileType = fileCommit.getType()
        val fileName = fileCommit.name?.trim()
        val project = fileCommit.commit?.project

        // Валидация только для обработок и отчетов
        if (fileType != TypesFiles.DATAPROCESSOR && fileType != TypesFiles.REPORT) {
            log.debug("Пропуск валидации для типа: {}", fileType)
            return
        }

        // Проверяем наличие проекта
        if (project == null) {
            log.warn("Проект не указан для FileCommit, пропуск валидации")
            return
        }

        // Проверяем наличие имени
        if (fileName.isNullOrBlank()) {
            log.warn("Имя файла не указано, пропуск валидации")
            return
        }

        // Извлекаем имя без расширения
        val nameWithoutExtension = fileName.substringBeforeLast(".")

        // Проверяем существование имени в списке доступных
        val nameExists = availableNameService.nameExists(project, fileType, nameWithoutExtension)

        if (!nameExists) {
            log.info("Имя '{}' не найдено в списке доступных для типа {}", nameWithoutExtension, fileType.id)

            // Добавляем ошибку валидации
            event.errors.add("Имя '$nameWithoutExtension' не найдено в списке доступных. Выберите существующее или создайте новое.")

            // Показываем диалог выбора/создания имени асинхронно
            com.vaadin.flow.component.UI.getCurrent().access {
                validationInProgress = true
                availableNameDialog.open(
                    project = project,
                    type = fileType,
                    currentName = nameWithoutExtension,
                    onSelect = { selectedName ->
                        log.info("Выбрано имя: {}", selectedName)

                        // Обновляем имя файла (с расширением)
                        val extension = fileName.substringAfterLast(".", "")
                        val newFileName = if (extension.isNotBlank()) {
                            "$selectedName.$extension"
                        } else {
                            selectedName
                        }

                        fileCommit.name = newFileName
                        nameField.value = newFileName

                        notifications.create("Имя файла обновлено: $newFileName")
                            .withType(Notifications.Type.SUCCESS)
                            .show()

                        validationInProgress = false

                        // Повторяем сохранение
                        close(StandardOutcome.SAVE)
                    }
                )
            }
        } else {
            log.debug("Имя '{}' найдено в списке доступных", nameWithoutExtension)
        }
    }

}