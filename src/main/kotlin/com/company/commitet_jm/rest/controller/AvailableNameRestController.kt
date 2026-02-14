package com.company.commitet_jm.rest.controller

import com.company.commitet_jm.entity.Project
import com.company.commitet_jm.entity.TypesFiles
import com.company.commitet_jm.rest.dto.*
import com.company.commitet_jm.service.availablename.AvailableNameService
import io.jmix.core.DataManager
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

/**
 * REST контроллер для управления доступными именами файлов.
 *
 * Управляет списком допустимых имен обработок и отчетов для проектов.
 */
@RestController
@RequestMapping("/api/available-names")
class AvailableNameRestController(
    private val availableNameService: AvailableNameService,
    private val dataManager: DataManager
) {
    companion object {
        private val log = LoggerFactory.getLogger(AvailableNameRestController::class.java)
    }

    /**
     * Массовое обновление списка доступных имен для проекта.
     *
     * POST /api/available-names/{projectId}
     *
     * Обновляет список доступных имен файлов (upsert).
     * Если имя уже существует - обновляет его, если нет - создает новое.
     *
     * @param projectId UUID проекта.
     * @param request Список доступных имен.
     * @return Результат обновления.
     */
    @PostMapping(
        value = ["/{projectId}"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun updateAvailableNames(
        @PathVariable projectId: UUID,
        @RequestBody request: AvailableNamesUpdateRequest
    ): ResponseEntity<Any> {
        log.info("POST /api/available-names/{} - обновление списка доступных имен (count={})", projectId, request.names.size)

        return try {
            // Загружаем проект
            val project = dataManager.load(Project::class.java)
                .id(projectId)
                .optional()
                .orElse(null)

            if (project == null) {
                log.warn("Проект не найден: {}", projectId)
                return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(ErrorResponse(
                        error = "PROJECT_NOT_FOUND",
                        message = "Проект с ID $projectId не найден"
                    ))
            }

            // Валидация
            val validationErrors = mutableListOf<String>()
            request.names.forEachIndexed { index, dto ->
                if (dto.name.isBlank()) {
                    validationErrors.add("Элемент $index: имя не может быть пустым")
                }
                if (TypesFiles.fromId(dto.type) == null) {
                    validationErrors.add("Элемент $index: неверный тип '${dto.type}'")
                }
            }

            if (validationErrors.isNotEmpty()) {
                return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(AvailableNamesUpdateResponse(
                        success = false,
                        created = 0,
                        updated = 0,
                        total = 0,
                        message = "Ошибка валидации",
                        errors = validationErrors
                    ))
            }

            // Преобразуем DTO в сервисный формат
            val serviceDtos = request.names.map { dto ->
                AvailableNameService.AvailableNameDto(
                    name = dto.name,
                    type = TypesFiles.fromId(dto.type)!!,
                    description = dto.description
                )
            }

            // Обновляем
            val result = availableNameService.updateAvailableNames(project, serviceDtos)

            log.info("Обновление завершено: создано={}, обновлено={}, всего={}", result.created, result.updated, result.total)

            ResponseEntity.ok(AvailableNamesUpdateResponse(
                success = true,
                created = result.created,
                updated = result.updated,
                total = result.total,
                message = "Список доступных имен успешно обновлен"
            ))

        } catch (e: Exception) {
            log.error("Ошибка при обновлении списка доступных имен: {}", e.message, e)
            ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(AvailableNamesUpdateResponse(
                    success = false,
                    created = 0,
                    updated = 0,
                    total = 0,
                    message = "Внутренняя ошибка сервера",
                    errors = listOf(e.message ?: "Unknown error")
                ))
        }
    }

    /**
     * Получение списка доступных имен для проекта.
     *
     * GET /api/available-names/{projectId}?type=DATAPROCESSOR
     *
     * @param projectId UUID проекта.
     * @param type Тип файлов (опционально). Если не указан, возвращаются все типы.
     * @return Список доступных имен.
     */
    @GetMapping(
        value = ["/{projectId}"],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun getAvailableNames(
        @PathVariable projectId: UUID,
        @RequestParam(required = false) type: String?
    ): ResponseEntity<Any> {
        log.info("GET /api/available-names/{} - получение списка (type={})", projectId, type ?: "all")

        return try {
            // Загружаем проект
            val project = dataManager.load(Project::class.java)
                .id(projectId)
                .optional()
                .orElse(null)

            if (project == null) {
                log.warn("Проект не найден: {}", projectId)
                return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(ErrorResponse(
                        error = "PROJECT_NOT_FOUND",
                        message = "Проект с ID $projectId не найден"
                    ))
            }

            // Загружаем список
            val availableNames = if (type != null) {
                val fileType = TypesFiles.fromId(type)
                if (fileType == null) {
                    return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body(ErrorResponse(
                            error = "INVALID_TYPE",
                            message = "Неверный тип файла: $type"
                        ))
                }
                availableNameService.getAvailableNames(project, fileType)
            } else {
                availableNameService.getAllAvailableNames(project)
            }

            // Преобразуем в DTO
            val namesDto = availableNames.map { name ->
                AvailableNameInfo(
                    id = name.id.toString(),
                    name = name.name.toString(),
                    type = name.getType()?.id ?: "",
                    description = name.description,
                    lastUpdated = name.lastUpdated
                )
            }

            ResponseEntity.ok(AvailableNamesListResponse(
                success = true,
                names = namesDto,
                total = namesDto.size
            ))

        } catch (e: Exception) {
            log.error("Ошибка при получении списка доступных имен: {}", e.message, e)
            ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse(
                    error = "INTERNAL_ERROR",
                    message = "Внутренняя ошибка сервера",
                    details = mapOf("exception" to (e.message ?: "Unknown error"))
                ))
        }
    }
}
