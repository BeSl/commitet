package com.company.commitet_jm.view.metadata

import com.company.commitet_jm.entity.ConfigMetadataItem
import com.company.commitet_jm.entity.MetadataType
import com.company.commitet_jm.entity.Project
import com.company.commitet_jm.service.metadata.ConfigMetadataService
import com.company.commitet_jm.view.main.MainView
import com.vaadin.flow.component.ClickEvent
import com.vaadin.flow.component.icon.Icon
import com.vaadin.flow.component.icon.VaadinIcon
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.data.renderer.ComponentRenderer
import com.vaadin.flow.router.Route
import io.jmix.flowui.component.combobox.EntityComboBox
import io.jmix.flowui.component.grid.TreeDataGrid
import io.jmix.flowui.kit.component.button.JmixButton
import io.jmix.flowui.model.CollectionLoader
import io.jmix.flowui.view.*

@Route(value = "config-metadata", layout = MainView::class)
@ViewController(id = "ConfigMetadataItem.tree")
@ViewDescriptor(path = "config-metadata-tree-view.xml")
class ConfigMetadataTreeView : StandardView() {

    @ViewComponent
    private lateinit var metadataDl: CollectionLoader<ConfigMetadataItem>

    @ViewComponent
    private lateinit var projectField: EntityComboBox<Project>

    @ViewComponent
    private lateinit var metadataTree: TreeDataGrid<ConfigMetadataItem>

    @ViewComponent
    private lateinit var projectsDl: CollectionLoader<Project>

    private val configMetadataService: ConfigMetadataService by lazy {
        applicationContext.getBean(ConfigMetadataService::class.java)
    }

    @Subscribe
    fun onInit(event: InitEvent) {
        projectsDl.load()
        setupTreeRenderer()
    }

    @Subscribe
    fun onReady(event: ReadyEvent) {
        // Выбираем первый проект по умолчанию
        val projects = projectsDl.container.items
        if (projects.isNotEmpty()) {
            projectField.value = projects.first()
        }
    }

    @Subscribe("projectField")
    fun onProjectFieldValueChange(event: com.vaadin.flow.component.AbstractField.ComponentValueChangeEvent<EntityComboBox<Project>, Project>) {
        val project = event.value
        if (project != null) {
            metadataDl.setParameter("project", project)
            metadataDl.load()
        }
    }

    @Subscribe("refreshBtn")
    fun onRefreshBtnClick(event: ClickEvent<JmixButton>) {
        if (projectField.value != null) {
            metadataDl.load()
        }
    }

    @Subscribe("expandAllBtn")
    fun onExpandAllBtnClick(event: ClickEvent<JmixButton>) {
        metadataTree.expandRecursively(metadataTree.genericDataView.items.toList(), Int.MAX_VALUE)
    }

    @Subscribe("collapseAllBtn")
    fun onCollapseAllBtnClick(event: ClickEvent<JmixButton>) {
        metadataTree.collapseRecursively(metadataTree.genericDataView.items.toList(), Int.MAX_VALUE)
    }

    private fun setupTreeRenderer() {
        // Кастомный рендерер для колонки name с иконкой
        metadataTree.getColumnByKey("name")?.setRenderer(
            ComponentRenderer { item: ConfigMetadataItem ->
                createNameCell(item)
            }
        )

        // Рендерер для типа метаданных с badge
        metadataTree.getColumnByKey("metadataType")?.setRenderer(
            ComponentRenderer { item: ConfigMetadataItem ->
                createTypeCell(item)
            }
        )
    }

    private fun createNameCell(item: ConfigMetadataItem): HorizontalLayout {
        val layout = HorizontalLayout()
        layout.isPadding = false
        layout.isSpacing = true
        layout.alignItems = com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.CENTER

        val icon = getIconForType(item.getMetadataType(), item.isCollection)
        icon.style.set("min-width", "20px")

        val nameSpan = Span(item.name ?: "")

        layout.add(icon, nameSpan)
        return layout
    }

    private fun createTypeCell(item: ConfigMetadataItem): Span {
        val span = Span()
        val metadataType = item.getMetadataType()

        if (metadataType != null) {
            span.text = getTypeDisplayName(metadataType)
            span.element.themeList.add("badge")
            span.element.themeList.add(getTypeBadgeTheme(metadataType))
        }

        return span
    }

    private fun getIconForType(type: MetadataType?, isCollection: Boolean?): Icon {
        if (isCollection == true) {
            return Icon(VaadinIcon.FOLDER_O)
        }

        return when (type) {
            MetadataType.ROOT -> Icon(VaadinIcon.COG)
            MetadataType.CATALOG -> Icon(VaadinIcon.DATABASE)
            MetadataType.DOCUMENT -> Icon(VaadinIcon.FILE_TEXT_O)
            MetadataType.ENUM -> Icon(VaadinIcon.LIST)
            MetadataType.REPORT -> Icon(VaadinIcon.CHART)
            MetadataType.DATA_PROCESSOR -> Icon(VaadinIcon.COGS)
            MetadataType.INFORMATION_REGISTER -> Icon(VaadinIcon.TABLE)
            MetadataType.ACCUMULATION_REGISTER -> Icon(VaadinIcon.TRENDING_UP)
            MetadataType.ACCOUNTING_REGISTER -> Icon(VaadinIcon.BOOK_DOLLAR)
            MetadataType.CALCULATION_REGISTER -> Icon(VaadinIcon.CALC)
            MetadataType.BUSINESS_PROCESS -> Icon(VaadinIcon.SITEMAP)
            MetadataType.TASK -> Icon(VaadinIcon.CHECK_SQUARE_O)
            MetadataType.CONSTANT -> Icon(VaadinIcon.BOOKMARK)
            MetadataType.EXCHANGE_PLAN -> Icon(VaadinIcon.EXCHANGE)
            MetadataType.CHART_OF_ACCOUNTS -> Icon(VaadinIcon.TREE_TABLE)
            MetadataType.CHART_OF_CALCULATION_TYPES -> Icon(VaadinIcon.CALC_BOOK)
            MetadataType.CHART_OF_CHARACTERISTIC_TYPES -> Icon(VaadinIcon.TAGS)
            MetadataType.ATTRIBUTE -> Icon(VaadinIcon.MINUS)
            MetadataType.TABULAR_SECTION -> Icon(VaadinIcon.GRID_SMALL)
            MetadataType.FORM -> Icon(VaadinIcon.BROWSER)
            MetadataType.TEMPLATE -> Icon(VaadinIcon.FILE_CODE)
            MetadataType.COMMAND -> Icon(VaadinIcon.TERMINAL)
            MetadataType.COMMON_MODULE -> Icon(VaadinIcon.CODE)
            MetadataType.SESSION_PARAMETER -> Icon(VaadinIcon.KEY)
            MetadataType.ROLE -> Icon(VaadinIcon.USER_CHECK)
            MetadataType.COMMON_FORM -> Icon(VaadinIcon.BROWSER)
            MetadataType.COMMON_COMMAND -> Icon(VaadinIcon.TERMINAL)
            MetadataType.COMMON_TEMPLATE -> Icon(VaadinIcon.FILE_CODE)
            MetadataType.SUBSYSTEM -> Icon(VaadinIcon.CUBES)
            MetadataType.STYLE_ITEM -> Icon(VaadinIcon.PAINT_ROLL)
            MetadataType.LANGUAGE -> Icon(VaadinIcon.GLOBE)
            MetadataType.COLLECTION -> Icon(VaadinIcon.FOLDER)
            MetadataType.WEB_SERVICE -> Icon(VaadinIcon.CLOUD)
            MetadataType.HTTP_SERVICE -> Icon(VaadinIcon.CONNECT)
            MetadataType.SEQUENCE -> Icon(VaadinIcon.ARROW_RIGHT)
            MetadataType.SCHEDULED_JOB -> Icon(VaadinIcon.CLOCK)
            MetadataType.FUNCTIONAL_OPTION -> Icon(VaadinIcon.OPTIONS)
            MetadataType.FUNCTIONAL_OPTIONS_PARAMETER -> Icon(VaadinIcon.SLIDERS)
            MetadataType.DEFINED_TYPE -> Icon(VaadinIcon.ASTERISK)
            MetadataType.COMMON_ATTRIBUTE -> Icon(VaadinIcon.TAG)
            MetadataType.EVENT_SUBSCRIPTION -> Icon(VaadinIcon.BELL)
            MetadataType.EXTERNAL_DATA_SOURCE -> Icon(VaadinIcon.EXTERNAL_LINK)
            null -> Icon(VaadinIcon.QUESTION)
        }
    }

    private fun getTypeDisplayName(type: MetadataType): String {
        return when (type) {
            MetadataType.ROOT -> "Конфигурация"
            MetadataType.CATALOG -> "Справочник"
            MetadataType.DOCUMENT -> "Документ"
            MetadataType.ENUM -> "Перечисление"
            MetadataType.REPORT -> "Отчет"
            MetadataType.DATA_PROCESSOR -> "Обработка"
            MetadataType.INFORMATION_REGISTER -> "Рег. сведений"
            MetadataType.ACCUMULATION_REGISTER -> "Рег. накопления"
            MetadataType.ACCOUNTING_REGISTER -> "Рег. бухгалтерии"
            MetadataType.CALCULATION_REGISTER -> "Рег. расчета"
            MetadataType.BUSINESS_PROCESS -> "Бизнес-процесс"
            MetadataType.TASK -> "Задача"
            MetadataType.CONSTANT -> "Константа"
            MetadataType.EXCHANGE_PLAN -> "План обмена"
            MetadataType.CHART_OF_ACCOUNTS -> "План счетов"
            MetadataType.CHART_OF_CALCULATION_TYPES -> "План видов расчета"
            MetadataType.CHART_OF_CHARACTERISTIC_TYPES -> "ПВХ"
            MetadataType.ATTRIBUTE -> "Реквизит"
            MetadataType.TABULAR_SECTION -> "Табл. часть"
            MetadataType.FORM -> "Форма"
            MetadataType.TEMPLATE -> "Макет"
            MetadataType.COMMAND -> "Команда"
            MetadataType.COMMON_MODULE -> "Общий модуль"
            MetadataType.SESSION_PARAMETER -> "Парам. сеанса"
            MetadataType.ROLE -> "Роль"
            MetadataType.COMMON_FORM -> "Общая форма"
            MetadataType.COMMON_COMMAND -> "Общая команда"
            MetadataType.COMMON_TEMPLATE -> "Общий макет"
            MetadataType.SUBSYSTEM -> "Подсистема"
            MetadataType.STYLE_ITEM -> "Элемент стиля"
            MetadataType.LANGUAGE -> "Язык"
            MetadataType.COLLECTION -> "Коллекция"
            MetadataType.WEB_SERVICE -> "Web-сервис"
            MetadataType.HTTP_SERVICE -> "HTTP-сервис"
            MetadataType.SEQUENCE -> "Последовательность"
            MetadataType.SCHEDULED_JOB -> "Регл. задание"
            MetadataType.FUNCTIONAL_OPTION -> "Функц. опция"
            MetadataType.FUNCTIONAL_OPTIONS_PARAMETER -> "Парам. функц. опций"
            MetadataType.DEFINED_TYPE -> "Опред. тип"
            MetadataType.COMMON_ATTRIBUTE -> "Общий реквизит"
            MetadataType.EVENT_SUBSCRIPTION -> "Подписка на событие"
            MetadataType.EXTERNAL_DATA_SOURCE -> "Внешний источник"
        }
    }

    private fun getTypeBadgeTheme(type: MetadataType): String {
        return when (type) {
            MetadataType.CATALOG, MetadataType.DOCUMENT -> "primary"
            MetadataType.REPORT, MetadataType.DATA_PROCESSOR -> "success"
            MetadataType.INFORMATION_REGISTER, MetadataType.ACCUMULATION_REGISTER,
            MetadataType.ACCOUNTING_REGISTER, MetadataType.CALCULATION_REGISTER -> "contrast"
            MetadataType.COMMON_MODULE, MetadataType.ROLE -> "warning"
            MetadataType.ATTRIBUTE, MetadataType.TABULAR_SECTION -> ""
            else -> ""
        }
    }
}
