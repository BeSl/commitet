package com.company.commitet_jm.service.metadata

import com.company.commitet_jm.entity.ConfigMetadataItem
import com.company.commitet_jm.entity.MetadataType
import com.company.commitet_jm.entity.Project
import io.jmix.core.DataManager
import io.jmix.core.FetchPlan
import io.jmix.core.FetchPlans
import io.jmix.core.SaveContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.*

@Service
open class ConfigMetadataService(
    private val dataManager: DataManager,
    private val fetchPlans: FetchPlans
) {
    private val log = LoggerFactory.getLogger(ConfigMetadataService::class.java)

    companion object {
        private const val BATCH_SIZE = 500

        /**
         * Маппинг названий каталогов выгрузки на типы метаданных
         */
        private val FOLDER_TO_TYPE = mapOf(
            "Catalogs" to MetadataType.CATALOG,
            "Documents" to MetadataType.DOCUMENT,
            "Enums" to MetadataType.ENUM,
            "Reports" to MetadataType.REPORT,
            "DataProcessors" to MetadataType.DATA_PROCESSOR,
            "InformationRegisters" to MetadataType.INFORMATION_REGISTER,
            "AccumulationRegisters" to MetadataType.ACCUMULATION_REGISTER,
            "AccountingRegisters" to MetadataType.ACCOUNTING_REGISTER,
            "CalculationRegisters" to MetadataType.CALCULATION_REGISTER,
            "BusinessProcesses" to MetadataType.BUSINESS_PROCESS,
            "Tasks" to MetadataType.TASK,
            "Constants" to MetadataType.CONSTANT,
            "ExchangePlans" to MetadataType.EXCHANGE_PLAN,
            "ChartsOfAccounts" to MetadataType.CHART_OF_ACCOUNTS,
            "ChartsOfCalculationTypes" to MetadataType.CHART_OF_CALCULATION_TYPES,
            "ChartsOfCharacteristicTypes" to MetadataType.CHART_OF_CHARACTERISTIC_TYPES,
            "CommonModules" to MetadataType.COMMON_MODULE,
            "SessionParameters" to MetadataType.SESSION_PARAMETER,
            "Roles" to MetadataType.ROLE,
            "CommonForms" to MetadataType.COMMON_FORM,
            "CommonCommands" to MetadataType.COMMON_COMMAND,
            "CommonTemplates" to MetadataType.COMMON_TEMPLATE,
            "Subsystems" to MetadataType.SUBSYSTEM,
            "StyleItems" to MetadataType.STYLE_ITEM,
            "Languages" to MetadataType.LANGUAGE,
            "WebServices" to MetadataType.WEB_SERVICE,
            "HTTPServices" to MetadataType.HTTP_SERVICE,
            "Sequences" to MetadataType.SEQUENCE,
            "ScheduledJobs" to MetadataType.SCHEDULED_JOB,
            "FunctionalOptions" to MetadataType.FUNCTIONAL_OPTION,
            "FunctionalOptionsParameters" to MetadataType.FUNCTIONAL_OPTIONS_PARAMETER,
            "DefinedTypes" to MetadataType.DEFINED_TYPE,
            "CommonAttributes" to MetadataType.COMMON_ATTRIBUTE,
            "EventSubscriptions" to MetadataType.EVENT_SUBSCRIPTION,
            "ExternalDataSources" to MetadataType.EXTERNAL_DATA_SOURCE
        )

        /**
         * Внутренние каталоги объектов метаданных
         */
        private val INNER_FOLDER_TO_TYPE = mapOf(
            "Forms" to MetadataType.FORM,
            "Templates" to MetadataType.TEMPLATE,
            "Commands" to MetadataType.COMMAND,
            "Attributes" to MetadataType.ATTRIBUTE,
            "TabularSections" to MetadataType.TABULAR_SECTION
        )

        /**
         * Каталоги, которые нужно пропускать
         */
        private val SKIP_FOLDERS = setOf("Ext", "Help")
    }

    /**
     * Импорт конфигурации метаданных с поддержкой Upsert.
     * Если объект с таким externalId + project уже существует - обновляем,
     * иначе создаём новый.
     */
    @Transactional
    open fun importConfig(project: Project, metadataDtoList: List<MetadataDTO>): ImportResult {
        log.info("Starting metadata import for project: ${project.name}, items count: ${metadataDtoList.size}")

        // Загружаем все существующие элементы для этого проекта
        val existingItems = loadExistingItems(project)
        val existingByExternalId = existingItems.associateBy { it.externalId }

        val itemsToSave = mutableListOf<ConfigMetadataItem>()
        val createdByExternalId = mutableMapOf<String, ConfigMetadataItem>()
        var createdCount = 0
        var updatedCount = 0

        // Рекурсивно обрабатываем дерево DTO
        fun processDto(dto: MetadataDTO, parentItem: ConfigMetadataItem?) {
            val existingItem = existingByExternalId[dto.externalId]
            val item: ConfigMetadataItem

            if (existingItem != null) {
                // Update existing
                item = existingItem
                item.name = dto.name
                item.setMetadataType(MetadataType.fromId(dto.metadataType))
                item.isCollection = dto.isCollection
                item.sortOrder = dto.sortOrder
                item.parent = parentItem
                item.fullPath = buildFullPath(parentItem, dto.name)
                updatedCount++
            } else {
                // Create new
                item = dataManager.create(ConfigMetadataItem::class.java)
                item.externalId = dto.externalId
                item.name = dto.name
                item.setMetadataType(MetadataType.fromId(dto.metadataType))
                item.isCollection = dto.isCollection
                item.sortOrder = dto.sortOrder
                item.parent = parentItem
                item.project = project
                item.fullPath = buildFullPath(parentItem, dto.name)
                createdCount++
            }

            createdByExternalId[dto.externalId] = item
            itemsToSave.add(item)

            // Обрабатываем детей рекурсивно
            dto.children.forEach { childDto ->
                processDto(childDto, item)
            }
        }

        // Обрабатываем корневые элементы
        metadataDtoList.forEach { dto ->
            val parentItem = dto.parentExternalId?.let {
                existingByExternalId[it] ?: createdByExternalId[it]
            }
            processDto(dto, parentItem)
        }

        // Сохраняем batch-ами для производительности
        saveBatch(itemsToSave)

        log.info("Import completed: created=$createdCount, updated=$updatedCount, total=${itemsToSave.size}")

        return ImportResult(
            created = createdCount,
            updated = updatedCount,
            total = itemsToSave.size
        )
    }

    /**
     * Импорт метаданных из файловой структуры выгрузки конфигуратора 1С.
     * Сканирует каталог src проекта и строит дерево метаданных.
     *
     * @param project проект
     * @param srcPath путь к каталогу src (если null, используется project.localPath/src)
     */
    @Transactional
    open fun importFromFileSystem(project: Project, srcPath: Path? = null): ImportResult {
        val basePath = srcPath ?: Path.of(project.localPath ?: "", "src")

        if (!Files.exists(basePath) || !Files.isDirectory(basePath)) {
            log.error("Source directory does not exist: $basePath")
            return ImportResult(0, 0, 0, listOf("Каталог не найден: $basePath"))
        }

        log.info("Starting metadata import from file system for project: ${project.name}, path: $basePath")

        // Очищаем существующие метаданные
        clearMetadata(project)

        val itemsToSave = mutableListOf<ConfigMetadataItem>()
        var sortOrder = 0

        // Создаём корневой элемент конфигурации
        val configFile = basePath.resolve("Configuration.xml")
        val configName = if (Files.exists(configFile)) {
            extractNameFromXml(configFile) ?: "Конфигурация"
        } else {
            "Конфигурация"
        }

        val rootItem = dataManager.create(ConfigMetadataItem::class.java).apply {
            externalId = UUID.randomUUID().toString()
            name = configName
            setMetadataType(MetadataType.ROOT)
            isCollection = false
            this.sortOrder = sortOrder++
            this.project = project
            fullPath = configName
        }
        itemsToSave.add(rootItem)

        // Сканируем каталоги с типами метаданных
        Files.list(basePath).use { stream ->
            stream.filter { Files.isDirectory(it) }
                .filter { FOLDER_TO_TYPE.containsKey(it.fileName.toString()) }
                .sorted(Comparator.comparing { it.fileName.toString() })
                .forEach { typeFolder ->
                    val folderName = typeFolder.fileName.toString()
                    val metadataType = FOLDER_TO_TYPE[folderName]!!

                    // Создаём коллекцию (группу) для типа метаданных
                    val collectionItem = dataManager.create(ConfigMetadataItem::class.java).apply {
                        externalId = UUID.randomUUID().toString()
                        name = getCollectionDisplayName(folderName)
                        setMetadataType(MetadataType.COLLECTION)
                        isCollection = true
                        this.sortOrder = sortOrder++
                        parent = rootItem
                        this.project = project
                        fullPath = "${rootItem.fullPath}.$name"
                    }
                    itemsToSave.add(collectionItem)

                    // Сканируем объекты метаданных внутри каталога
                    scanMetadataObjects(typeFolder, metadataType, collectionItem, project, itemsToSave)
                }
        }

        // Сохраняем всё
        saveBatch(itemsToSave)

        log.info("File system import completed: total=${itemsToSave.size}")
        return ImportResult(itemsToSave.size, 0, itemsToSave.size)
    }

    /**
     * Сканирует объекты метаданных в каталоге типа (например, Catalogs)
     */
    private fun scanMetadataObjects(
        typeFolder: Path,
        metadataType: MetadataType,
        parentItem: ConfigMetadataItem,
        project: Project,
        itemsToSave: MutableList<ConfigMetadataItem>
    ) {
        var sortOrder = 0

        Files.list(typeFolder).use { stream ->
            stream.filter { Files.isDirectory(it) }
                .sorted(Comparator.comparing { it.fileName.toString() })
                .forEach { objectFolder ->
                    val objectName = objectFolder.fileName.toString()

                    // Создаём элемент метаданных
                    val objectItem = dataManager.create(ConfigMetadataItem::class.java).apply {
                        externalId = UUID.randomUUID().toString()
                        name = objectName
                        setMetadataType(metadataType)
                        isCollection = false
                        this.sortOrder = sortOrder++
                        parent = parentItem
                        this.project = project
                        fullPath = "${parentItem.fullPath}.$objectName"
                    }
                    itemsToSave.add(objectItem)

                    // Сканируем внутренние элементы (формы, макеты, команды и т.д.)
                    scanInnerElements(objectFolder, objectItem, project, itemsToSave)
                }
        }
    }

    /**
     * Сканирует внутренние элементы объекта метаданных (формы, макеты, реквизиты и т.д.)
     */
    private fun scanInnerElements(
        objectFolder: Path,
        parentItem: ConfigMetadataItem,
        project: Project,
        itemsToSave: MutableList<ConfigMetadataItem>
    ) {
        Files.list(objectFolder).use { stream ->
            stream.filter { Files.isDirectory(it) }
                .filter { !SKIP_FOLDERS.contains(it.fileName.toString()) }
                .forEach { innerFolder ->
                    val folderName = innerFolder.fileName.toString()
                    val innerType = INNER_FOLDER_TO_TYPE[folderName]

                    if (innerType != null) {
                        // Это известный тип внутренних элементов
                        var sortOrder = 0
                        Files.list(innerFolder).use { innerStream ->
                            innerStream.filter { Files.isDirectory(it) }
                                .sorted(Comparator.comparing { it.fileName.toString() })
                                .forEach { elementFolder ->
                                    val elementName = elementFolder.fileName.toString()
                                    val elementItem = dataManager.create(ConfigMetadataItem::class.java).apply {
                                        externalId = UUID.randomUUID().toString()
                                        name = elementName
                                        setMetadataType(innerType)
                                        isCollection = false
                                        this.sortOrder = sortOrder++
                                        parent = parentItem
                                        this.project = project
                                        fullPath = "${parentItem.fullPath}.$elementName"
                                    }
                                    itemsToSave.add(elementItem)
                                }
                        }
                    }
                }
        }
    }

    /**
     * Извлекает имя конфигурации из Configuration.xml
     */
    private fun extractNameFromXml(xmlPath: Path): String? {
        return try {
            val content = Files.readString(xmlPath)
            // Ищем тег <Name> или атрибут name
            val nameRegex = Regex("<Name>([^<]+)</Name>", RegexOption.IGNORE_CASE)
            nameRegex.find(content)?.groupValues?.get(1)
        } catch (e: Exception) {
            log.warn("Failed to extract name from XML: ${e.message}")
            null
        }
    }

    /**
     * Возвращает отображаемое название коллекции на русском
     */
    private fun getCollectionDisplayName(folderName: String): String {
        return when (folderName) {
            "Catalogs" -> "Справочники"
            "Documents" -> "Документы"
            "Enums" -> "Перечисления"
            "Reports" -> "Отчёты"
            "DataProcessors" -> "Обработки"
            "InformationRegisters" -> "Регистры сведений"
            "AccumulationRegisters" -> "Регистры накопления"
            "AccountingRegisters" -> "Регистры бухгалтерии"
            "CalculationRegisters" -> "Регистры расчёта"
            "BusinessProcesses" -> "Бизнес-процессы"
            "Tasks" -> "Задачи"
            "Constants" -> "Константы"
            "ExchangePlans" -> "Планы обмена"
            "ChartsOfAccounts" -> "Планы счетов"
            "ChartsOfCalculationTypes" -> "Планы видов расчёта"
            "ChartsOfCharacteristicTypes" -> "Планы видов характеристик"
            "CommonModules" -> "Общие модули"
            "SessionParameters" -> "Параметры сеанса"
            "Roles" -> "Роли"
            "CommonForms" -> "Общие формы"
            "CommonCommands" -> "Общие команды"
            "CommonTemplates" -> "Общие макеты"
            "Subsystems" -> "Подсистемы"
            "StyleItems" -> "Элементы стиля"
            "Languages" -> "Языки"
            "WebServices" -> "Web-сервисы"
            "HTTPServices" -> "HTTP-сервисы"
            "Sequences" -> "Последовательности"
            "ScheduledJobs" -> "Регламентные задания"
            "FunctionalOptions" -> "Функциональные опции"
            "FunctionalOptionsParameters" -> "Параметры функциональных опций"
            "DefinedTypes" -> "Определяемые типы"
            "CommonAttributes" -> "Общие реквизиты"
            "EventSubscriptions" -> "Подписки на события"
            "ExternalDataSources" -> "Внешние источники данных"
            else -> folderName
        }
    }

    /**
     * Полная очистка дерева метаданных для проекта.
     * Использует каскадное удаление через FK.
     */
    @Transactional
    open fun clearMetadata(project: Project) {
        log.info("Clearing metadata for project: ${project.name}")

        // Загружаем только корневые элементы (без родителей)
        // FK с CASCADE удалит детей автоматически
        val rootItems = dataManager.load(ConfigMetadataItem::class.java)
            .query("select e from ConfigMetadataItem e where e.project = :project and e.parent is null")
            .parameter("project", project)
            .list()

        if (rootItems.isNotEmpty()) {
            val saveContext = SaveContext()
            rootItems.forEach { saveContext.removing(it) }
            dataManager.save(saveContext)
            log.info("Deleted ${rootItems.size} root items (with cascading children)")
        }
    }

    /**
     * Получение дерева метаданных для проекта с оптимизированным Fetch Plan.
     */
    fun getMetadataTree(project: Project): List<ConfigMetadataItem> {
        val fetchPlan = fetchPlans.builder(ConfigMetadataItem::class.java)
            .addFetchPlan(FetchPlan.BASE)
            .add("parent", FetchPlan.LOCAL)
            .add("project", FetchPlan.LOCAL)
            .build()

        return dataManager.load(ConfigMetadataItem::class.java)
            .query("select e from ConfigMetadataItem e where e.project = :project order by e.sortOrder, e.name")
            .parameter("project", project)
            .fetchPlan(fetchPlan)
            .list()
    }

    /**
     * Получение корневых элементов дерева (для lazy loading в UI).
     */
    fun getRootItems(project: Project): List<ConfigMetadataItem> {
        return dataManager.load(ConfigMetadataItem::class.java)
            .query("select e from ConfigMetadataItem e where e.project = :project and e.parent is null order by e.sortOrder, e.name")
            .parameter("project", project)
            .list()
    }

    /**
     * Получение дочерних элементов для lazy loading.
     */
    fun getChildren(parent: ConfigMetadataItem): List<ConfigMetadataItem> {
        return dataManager.load(ConfigMetadataItem::class.java)
            .query("select e from ConfigMetadataItem e where e.parent = :parent order by e.sortOrder, e.name")
            .parameter("parent", parent)
            .list()
    }

    /**
     * Поиск элемента по полному пути.
     */
    fun findByFullPath(project: Project, fullPath: String): ConfigMetadataItem? {
        return dataManager.load(ConfigMetadataItem::class.java)
            .query("select e from ConfigMetadataItem e where e.project = :project and e.fullPath = :fullPath")
            .parameter("project", project)
            .parameter("fullPath", fullPath)
            .optional()
            .orElse(null)
    }

    /**
     * Поиск элементов по типу метаданных.
     */
    fun findByMetadataType(project: Project, metadataType: MetadataType): List<ConfigMetadataItem> {
        return dataManager.load(ConfigMetadataItem::class.java)
            .query("select e from ConfigMetadataItem e where e.project = :project and e.metadataType = :type order by e.sortOrder, e.name")
            .parameter("project", project)
            .parameter("type", metadataType.id)
            .list()
    }

    private fun loadExistingItems(project: Project): List<ConfigMetadataItem> {
        return dataManager.load(ConfigMetadataItem::class.java)
            .query("select e from ConfigMetadataItem e where e.project = :project")
            .parameter("project", project)
            .list()
    }

    private fun buildFullPath(parent: ConfigMetadataItem?, name: String?): String {
        return if (parent?.fullPath != null) {
            "${parent.fullPath}.$name"
        } else {
            name ?: ""
        }
    }

    private fun saveBatch(items: List<ConfigMetadataItem>) {
        if (items.isEmpty()) return

        // Для больших объёмов разбиваем на batch-и
        items.chunked(BATCH_SIZE).forEach { batch ->
            val saveContext = SaveContext()
            saveContext.setDiscardSaved(true) // Не возвращать сохранённые объекты для экономии памяти
            batch.forEach { saveContext.saving(it) }
            dataManager.save(saveContext)
            log.debug("Saved batch of ${batch.size} items")
        }
    }
}
