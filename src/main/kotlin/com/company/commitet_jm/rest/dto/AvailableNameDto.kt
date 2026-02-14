package com.company.commitet_jm.rest.dto

import com.company.commitet_jm.entity.TypesFiles
import java.time.LocalDateTime

/**
 * DTO для передачи информации о доступном имени
 */
data class AvailableNameDto(
    val name: String,
    val type: String, // TypesFiles.id
    val description: String? = null
)

/**
 * Запрос на массовое обновление доступных имен
 */
data class AvailableNamesUpdateRequest(
    val names: List<AvailableNameDto>
)

/**
 * Ответ на запрос обновления доступных имен
 */
data class AvailableNamesUpdateResponse(
    val success: Boolean,
    val created: Int,
    val updated: Int,
    val total: Int,
    val message: String? = null,
    val errors: List<String>? = null
)

/**
 * Информация о доступном имени (для списков)
 */
data class AvailableNameInfo(
    val id: String,
    val name: String,
    val type: String,
    val description: String? = null,
    val lastUpdated: LocalDateTime? = null
)

/**
 * Ответ со списком доступных имен
 */
data class AvailableNamesListResponse(
    val success: Boolean,
    val names: List<AvailableNameInfo>,
    val total: Int
)
