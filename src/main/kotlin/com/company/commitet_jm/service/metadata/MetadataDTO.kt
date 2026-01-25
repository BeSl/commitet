package com.company.commitet_jm.service.metadata

/**
 * DTO для импорта метаданных конфигурации 1С.
 *
 * Пример JSON:
 * ```json
 * {
 *   "externalId": "550e8400-e29b-41d4-a716-446655440000",
 *   "name": "Справочники",
 *   "metadataType": "collection",
 *   "isCollection": true,
 *   "sortOrder": 10,
 *   "parentExternalId": null,
 *   "children": [
 *     {
 *       "externalId": "660e8400-e29b-41d4-a716-446655440001",
 *       "name": "Номенклатура",
 *       "metadataType": "catalog",
 *       "isCollection": false,
 *       "sortOrder": 1,
 *       "parentExternalId": "550e8400-e29b-41d4-a716-446655440000",
 *       "children": [
 *         {
 *           "externalId": "770e8400-e29b-41d4-a716-446655440002",
 *           "name": "Артикул",
 *           "metadataType": "attribute",
 *           "isCollection": false,
 *           "sortOrder": 1,
 *           "parentExternalId": "660e8400-e29b-41d4-a716-446655440001"
 *         }
 *       ]
 *     }
 *   ]
 * }
 * ```
 */
data class MetadataDTO(
    val externalId: String,
    val name: String,
    val metadataType: String,
    val isCollection: Boolean = false,
    val sortOrder: Int = 0,
    val parentExternalId: String? = null,
    val children: List<MetadataDTO> = emptyList()
)

/**
 * Результат импорта метаданных.
 */
data class ImportResult(
    val created: Int,
    val updated: Int,
    val total: Int,
    val errors: List<String> = emptyList()
)
