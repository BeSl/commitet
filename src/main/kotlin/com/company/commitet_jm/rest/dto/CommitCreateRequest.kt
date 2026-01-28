package com.company.commitet_jm.rest.dto

import java.util.UUID

/**
 * DTO для запроса создания коммита через REST API.
 *
 * @property userId UUID пользователя (автора коммита). Если не указан или пользователь не найден,
 *                  будет использован пользователь по умолчанию.
 * @property projectId UUID проекта (обязательное поле).
 * @property taskNum Номер задачи (обязательное поле). Используется для создания имени ветки.
 * @property description Описание коммита (обязательное поле).
 * @property fixCommit Флаг, указывающий что это исправление (по умолчанию false).
 * @property files Список файлов для коммита.
 */
data class CommitCreateRequest(
    val userId: UUID? = null,
    val projectId: UUID,
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
 */
data class FileData(
    val name: String,
    val data: String,
    val type: String
)
