package com.company.commitet_jm.service.availablename

import com.company.commitet_jm.entity.AvailableName
import com.company.commitet_jm.entity.Project
import com.company.commitet_jm.entity.TypesFiles
import io.jmix.core.DataManager
import io.jmix.core.SaveContext
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class AvailableNameService(
    private val dataManager: DataManager
) {

    data class UpdateResult(
        val created: Int,
        val updated: Int,
        val total: Int
    )

    data class AvailableNameDto(
        val name: String,
        val type: TypesFiles,
        val description: String? = null
    )

    /**
     * Массовое обновление (upsert) доступных имен для проекта
     */
    fun updateAvailableNames(project: Project, nameDtos: List<AvailableNameDto>): UpdateResult {
        val saveContext = SaveContext()
        var created = 0
        var updated = 0

        nameDtos.forEach { dto ->
            val existing = dataManager.load(AvailableName::class.java)
                .query("select e from AvailableName e where e.project = :project and e.type = :type and e.name = :name")
                .parameter("project", project)
                .parameter("type", dto.type.id)
                .parameter("name", dto.name)
                .optional()
                .orElse(null)

            if (existing != null) {
                // Update existing
                existing.description = dto.description
                existing.lastUpdated = LocalDateTime.now()
                saveContext.saving(existing)
                updated++
            } else {
                // Create new
                val newName = dataManager.create(AvailableName::class.java)
                newName.project = project
                newName.setType(dto.type)
                newName.name = dto.name
                newName.description = dto.description
                newName.lastUpdated = LocalDateTime.now()
                saveContext.saving(newName)
                created++
            }
        }

        dataManager.save(saveContext)
        return UpdateResult(created, updated, created + updated)
    }

    /**
     * Проверка существования имени
     */
    fun nameExists(project: Project, type: TypesFiles, name: String): Boolean {
        return dataManager.load(AvailableName::class.java)
            .query("select e from AvailableName e where e.project = :project and e.type = :type and e.name = :name")
            .parameter("project", project)
            .parameter("type", type.id)
            .parameter("name", name)
            .optional()
            .isPresent
    }

    /**
     * Получить список доступных имен по типу
     */
    fun getAvailableNames(project: Project, type: TypesFiles): List<AvailableName> {
        return dataManager.load(AvailableName::class.java)
            .query("select e from AvailableName e where e.project = :project and e.type = :type order by e.name")
            .parameter("project", project)
            .parameter("type", type.id)
            .list()
    }

    /**
     * Получить все доступные имена для проекта
     */
    fun getAllAvailableNames(project: Project): List<AvailableName> {
        return dataManager.load(AvailableName::class.java)
            .query("select e from AvailableName e where e.project = :project order by e.type, e.name")
            .parameter("project", project)
            .list()
    }

    /**
     * Создать новое доступное имя
     */
    fun createAvailableName(project: Project, type: TypesFiles, name: String, description: String? = null): AvailableName {
        val availableName = dataManager.create(AvailableName::class.java)
        availableName.project = project
        availableName.setType(type)
        availableName.name = name
        availableName.description = description
        availableName.lastUpdated = LocalDateTime.now()
        return dataManager.save(availableName)
    }
}
