package com.company.commitet_jm.view.availablename

import com.company.commitet_jm.entity.AvailableName
import com.company.commitet_jm.entity.Project
import com.company.commitet_jm.entity.TypesFiles
import com.company.commitet_jm.service.availablename.AvailableNameService
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.combobox.ComboBox
import com.vaadin.flow.component.dialog.Dialog
import com.vaadin.flow.component.html.H3
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.textfield.TextArea
import com.vaadin.flow.component.textfield.TextField
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * Диалог для выбора или создания доступного имени файла.
 *
 * Используется для валидации имен обработок и отчетов при несоответствии
 * введенного имени списку доступных имен в проекте.
 */
@Component
class AvailableNameSelectionDialog(
    private val availableNameService: AvailableNameService
) {
    companion object {
        private val log = LoggerFactory.getLogger(AvailableNameSelectionDialog::class.java)
    }

    /**
     * Открывает диалог выбора/создания доступного имени.
     *
     * @param project Проект, для которого выбирается имя
     * @param type Тип файла (DATAPROCESSOR или REPORT)
     * @param currentName Текущее имя файла (заполняется по умолчанию)
     * @param onSelect Callback, вызываемый при выборе/создании имени
     */
    fun open(
        project: Project,
        type: TypesFiles,
        currentName: String,
        onSelect: (String) -> Unit
    ) {
        log.info("Открытие диалога выбора имени: project={}, type={}, currentName={}",
            project.name, type.id, currentName)

        val dialog = Dialog()
        dialog.width = "600px"

        // Загружаем доступные имена
        val availableNames = availableNameService.getAvailableNames(project, type)
        log.debug("Загружено {} доступных имен", availableNames.size)

        // Заголовок
        val header = H3("Выбор или создание имени")

        // ComboBox для выбора существующего имени
        val existingNameCombo = ComboBox<AvailableName>("Выбрать существующее имя")
        existingNameCombo.setItems(availableNames)
        existingNameCombo.setItemLabelGenerator { it.name ?: "" }
        existingNameCombo.placeholder = "Выберите из списка..."
        existingNameCombo.width = "100%"
        existingNameCombo.isClearButtonVisible = true

        // TextField для нового имени
        val newNameField = TextField("Или создать новое имя")
        newNameField.value = currentName
        newNameField.width = "100%"
        newNameField.placeholder = "Введите новое имя..."

        // TextArea для описания
        val descriptionArea = TextArea("Описание (опционально)")
        descriptionArea.width = "100%"
        descriptionArea.height = "80px"
        descriptionArea.placeholder = "Краткое описание назначения..."

        // Логика переключения между выбором и созданием
        existingNameCombo.addValueChangeListener { event ->
            if (event.value != null) {
                newNameField.value = event.value.name ?: ""
                descriptionArea.value = event.value.description ?: ""
                newNameField.isEnabled = false
            } else {
                newNameField.isEnabled = true
            }
        }

        newNameField.addValueChangeListener { event ->
            if (event.value.isNotBlank() && event.isFromClient) {
                existingNameCombo.clear()
                existingNameCombo.isEnabled = false
            } else if (event.value.isBlank()) {
                existingNameCombo.isEnabled = true
            }
        }

        // Кнопки
        val confirmButton = Button("Выбрать / Создать") { _ ->
            val selectedName = when {
                existingNameCombo.value != null -> {
                    log.info("Выбрано существующее имя: {}", existingNameCombo.value.name)
                    existingNameCombo.value.name.toString()
                }
                newNameField.value.isNotBlank() -> {
                    val newName = newNameField.value.trim()
                    val description = descriptionArea.value?.trim()

                    log.info("Создание нового имени: name={}, type={}", newName, type.id)

                    try {
                        availableNameService.createAvailableName(
                            project = project,
                            type = type,
                            name = newName,
                            description = description
                        )
                        log.info("Новое имя успешно создано")
                    } catch (e: Exception) {
                        log.error("Ошибка при создании нового имени: {}", e.message, e)
                    }

                    newName
                }
                else -> {
                    log.warn("Не выбрано ни существующее имя, ни введено новое")
                    return@Button
                }
            }

            onSelect(selectedName)
            dialog.close()
        }
        confirmButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY)

        val cancelButton = Button("Отмена") { _ ->
            log.info("Диалог отменен пользователем")
            dialog.close()
        }

        val buttonsLayout = HorizontalLayout(confirmButton, cancelButton)
        buttonsLayout.justifyContentMode = FlexComponent.JustifyContentMode.END
        buttonsLayout.width = "100%"

        // Компоновка
        val layout = VerticalLayout(
            header,
            existingNameCombo,
            newNameField,
            descriptionArea,
            buttonsLayout
        )
        layout.isPadding = true
        layout.isSpacing = true
        layout.width = "100%"

        dialog.add(layout)
        dialog.open()
    }
}
