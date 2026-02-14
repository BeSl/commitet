package com.company.commitet_jm.rest.dto

import java.util.UUID

/**
 * DTO для ответа на запрос создания коммита.
 *
 * @property success Флаг успешности операции.
 * @property commitId UUID созданного коммита (если успешно).
 * @property message Сообщение о результате операции.
 * @property authorId UUID назначенного автора коммита.
 * @property authorUsername Username назначенного автора.
 */
data class CommitCreateResponse(
    val success: Boolean,
    val commitId: UUID? = null,
    val message: String,
    val authorId: UUID? = null,
    val authorUsername: String? = null
)

/**
 * DTO для ответа с ошибкой.
 *
 * @property success Всегда false для ошибок.
 * @property error Код ошибки.
 * @property message Описание ошибки.
 * @property details Дополнительные детали ошибки (опционально).
 */
data class ErrorResponse(
    val success: Boolean = false,
    val error: String,
    val message: String,
    val details: Map<String, Any>? = null
)
