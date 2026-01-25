package com.company.commitet_jm.service.git

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper

/**
 * Тип изменения файла
 */
enum class DiffChangeType(val displayName: String, val icon: String) {
    ADDED("Добавлен", "PLUS"),
    MODIFIED("Изменён", "EDIT"),
    DELETED("Удалён", "TRASH"),
    RENAMED("Переименован", "ARROW_RIGHT"),
    COPIED("Скопирован", "COPY")
}

/**
 * Информация об изменении одного файла
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class DiffEntry(
    val path: String,
    val changeType: DiffChangeType,
    val oldPath: String? = null,
    val additions: Int = 0,
    val deletions: Int = 0,
    val diffContent: String? = null
) {
    /**
     * Получить имя файла (без пути)
     */
    val fileName: String
        get() = path.substringAfterLast("/")

    /**
     * Получить каталог файла
     */
    val directory: String
        get() = if (path.contains("/")) path.substringBeforeLast("/") else ""

    /**
     * Краткая статистика изменений
     */
    val stats: String
        get() = "+$additions / -$deletions"
}

/**
 * Узел дерева изменений (для отображения с группировкой по каталогам)
 */
data class DiffTreeNode(
    val name: String,
    val fullPath: String,
    val isDirectory: Boolean,
    val changeType: DiffChangeType? = null,
    val additions: Int = 0,
    val deletions: Int = 0,
    val diffContent: String? = null,
    val children: MutableList<DiffTreeNode> = mutableListOf()
) {
    val stats: String
        get() = if (additions > 0 || deletions > 0) "+$additions / -$deletions" else ""
}

/**
 * Полная информация о diff коммита
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class CommitDiffInfo(
    val entries: List<DiffEntry> = emptyList(),
    val totalAdditions: Int = 0,
    val totalDeletions: Int = 0,
    val totalFiles: Int = 0,
    val rawDiff: String? = null
) {
    companion object {
        private val objectMapper = ObjectMapper().apply {
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        }

        fun fromJson(json: String?): CommitDiffInfo? {
            if (json.isNullOrBlank()) return null
            return try {
                objectMapper.readValue(json, CommitDiffInfo::class.java)
            } catch (e: Exception) {
                null
            }
        }

        fun empty() = CommitDiffInfo()
    }

    fun toJson(): String = objectMapper.writeValueAsString(this)

    /**
     * Построить дерево изменений с группировкой по каталогам
     */
    fun buildTree(): List<DiffTreeNode> {
        val root = mutableMapOf<String, DiffTreeNode>()

        for (entry in entries) {
            val parts = entry.path.split("/")
            var currentPath = ""
            var currentMap = root

            for (i in parts.indices) {
                val part = parts[i]
                currentPath = if (currentPath.isEmpty()) part else "$currentPath/$part"
                val isFile = i == parts.lastIndex

                if (!currentMap.containsKey(part)) {
                    val node = if (isFile) {
                        DiffTreeNode(
                            name = part,
                            fullPath = currentPath,
                            isDirectory = false,
                            changeType = entry.changeType,
                            additions = entry.additions,
                            deletions = entry.deletions,
                            diffContent = entry.diffContent
                        )
                    } else {
                        DiffTreeNode(
                            name = part,
                            fullPath = currentPath,
                            isDirectory = true
                        )
                    }
                    currentMap[part] = node
                }

                if (!isFile) {
                    val dirNode = currentMap[part]!!
                    // Создаём map для children если нужно
                    @Suppress("UNCHECKED_CAST")
                    val childrenMap = dirNode.children.associateBy { it.name }.toMutableMap()
                    currentMap = childrenMap as MutableMap<String, DiffTreeNode>
                    // Обновляем children из map
                    dirNode.children.clear()
                    dirNode.children.addAll(childrenMap.values)
                }
            }
        }

        return root.values.toList().sortedWith(compareBy({ !it.isDirectory }, { it.name }))
    }

    /**
     * Получить список каталогов с файлами
     */
    fun getDirectoryGroups(): Map<String, List<DiffEntry>> {
        return entries.groupBy { it.directory.ifEmpty { "(корень)" } }
            .toSortedMap()
    }
}
