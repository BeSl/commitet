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
import java.util.*

@Service
open class ConfigMetadataService(
    private val dataManager: DataManager,
    private val fetchPlans: FetchPlans
) {
    private val log = LoggerFactory.getLogger(ConfigMetadataService::class.java)

    companion object {
        private const val BATCH_SIZE = 500
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
