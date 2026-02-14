package com.company.commitet_jm.rest.dto

import java.util.UUID

/**
 * DTO для запроса создания коммита через REST API.
 *
 * @property userId UUID пользователя (устаревшее, используйте externalUserId).
 * @property projectId UUID проекта (используется, если не указан externalProjectId).
 * @property externalUserId Внешний ID пользователя (автора коммита). Если не указан, используется пользователь по умолчанию.
 * @property externalProjectId Внешний ID проекта. Если не указан, используется projectId.
 * @property taskNum Номер задачи (обязательное поле). Используется для создания имени ветки.
 * @property description Описание коммита (обязательное поле).
 * @property fixCommit Флаг, указывающий что это исправление (по умолчанию false).
 * @property files Список файлов для коммита.
 */
data class CommitCreateRequest(
    // Старые поля (оставляем для обратной совместимости)
    val userId: UUID? = null,
    val projectId: UUID? = null,

    // Новые поля для поиска по внешним ID
    val externalUserId: String? = null,
    val externalProjectId: String? = null,

    val taskNum: String,
    val description: String,
    val fixCommit: Boolean = false,
    val files: List<FileData> = emptyList()
)

/**
 * DTO для данных файла в запросе на создание коммита.
 *
 * @property name Имя файла (обязательное поле).
 * @property data Содержимое файла в формате Base64 (обязательное поле).
 * @property type Тип файла: REPORT, DATAPROCESSOR, SCHEDULEDJOBS, EXTERNAL_CODE, EXCHANGE_RULES.
 * @property code Код файла (опциональное). Используется для формирования имени файла в формате "Код_Имя".
 */
data class FileData(
    val name: String,
    val data: String,
    val type: String,
    val code: String? = null
)
