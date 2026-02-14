package com.company.commitet_jm.service.ones

/**
 * Модели данных для работы с хранилищем 1С
 */

/**
 * Опции для формирования отчёта по истории хранилища
 */
data class HistoryStorageOptions(
    val versionStart: Int? = null,
    val versionEnd: Int? = null,
    val dateStart: String? = null,
    val dateEnd: String? = null,
    val reportFormat: ReportFormat = ReportFormat.TXT,
    val groupByObject: Boolean = false,
    val groupByComment: Boolean = false
)

/**
 * Формат отчёта
 */
enum class ReportFormat { TXT, MXL }

/**
 * Опции для добавления пользователя в хранилище
 */
data class UserStorageOptions(
    val restoreDeleted: Boolean = false,
    val extension: String? = null
)

/**
 * Опции для копирования пользователей между хранилищами
 */
data class CopyUsersOptions(
    val restoreDeleted: Boolean = false,
    val extension: String? = null
)

/**
 * Права пользователя в хранилище 1С
 */
enum class UserRights(val cliValue: String) {
    READ_ONLY("ReadOnly"),
    FULL_ACCESS("FullAccess"),
    VERSION_MANAGEMENT("VersionManagement")
}

/**
 * Исключение при операциях с хранилищем
 */
class StorageOperationException(message: String, cause: Throwable?) : RuntimeException(message, cause)
