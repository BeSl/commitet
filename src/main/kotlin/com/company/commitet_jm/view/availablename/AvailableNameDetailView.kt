package com.company.commitet_jm.view.availablename

import com.company.commitet_jm.entity.AvailableName
import com.company.commitet_jm.view.main.MainView
import com.vaadin.flow.router.Route
import io.jmix.flowui.view.*
import java.time.LocalDateTime

/**
 * Детальный view для доступного имени.
 */
@Route(value = "available-names/:id", layout = MainView::class)
@ViewController(id = "AvailableName.detail")
@ViewDescriptor(path = "available-name-detail-view.xml")
@EditedEntityContainer("availableNameDc")
class AvailableNameDetailView : StandardDetailView<AvailableName>() {

    @Subscribe
    private fun onBeforeSave(event: BeforeSaveEvent) {
        // Обновляем lastUpdated при сохранении
        editedEntity.lastUpdated = LocalDateTime.now()
    }
}
