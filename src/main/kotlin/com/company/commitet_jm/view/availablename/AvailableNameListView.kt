package com.company.commitet_jm.view.availablename

import com.company.commitet_jm.entity.AvailableName
import com.company.commitet_jm.view.main.MainView
import com.vaadin.flow.router.Route
import io.jmix.flowui.view.*

/**
 * Список доступных имен файлов.
 *
 * Позволяет управлять списком допустимых имен обработок и отчетов для проектов.
 */
@Route(value = "available-names", layout = MainView::class)
@ViewController(id = "AvailableName.list")
@ViewDescriptor(path = "available-name-list-view.xml")
@LookupComponent("availableNamesDataGrid")
@DialogMode(width = "64em")
class AvailableNameListView : StandardListView<AvailableName>()
