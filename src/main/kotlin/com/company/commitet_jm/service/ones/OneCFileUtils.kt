package com.company.commitet_jm.service.ones

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Утилитарный класс для файловых операций, связанных с 1С
 */
@Component
class OneCFileUtils {

    companion object {
        private val log = LoggerFactory.getLogger(OneCFileUtils::class.java)
    }

    /**
     * Очищает директорию или создаёт её, если не существует.
     * @param path Путь к директории
     * @param deleteRoot Удалять ли корневую директорию (false - оставить пустую директорию)
     * @return true если операция успешна
     */
    fun clearOrCreateDirectory(path: Path, deleteRoot: Boolean = true): Boolean {
        return try {
            if (Files.exists(path)) {
                Files.walk(path)
                    .sorted(Comparator.reverseOrder())
                    .filter { deleteRoot || it != path }
                    .forEach { Files.deleteIfExists(it) }
            }
            if (!deleteRoot || !Files.exists(path)) {
                Files.createDirectories(path)
            }
            true
        } catch (e: SecurityException) {
            log.error("Ошибка безопасности при работе с директорией $path: ${e.message}")
            false
        } catch (e: IOException) {
            log.error("Ошибка при работе с директорией $path: ${e.message}")
            false
        }
    }

    /**
     * Очищает директорию или создаёт её, если не существует.
     * @param directoryPath Строковый путь к директории
     * @param deleteRoot Удалять ли корневую директорию
     * @return true если операция успешна
     */
    fun clearOrCreateDirectory(directoryPath: String, deleteRoot: Boolean = true): Boolean {
        return clearOrCreateDirectory(Paths.get(directoryPath), deleteRoot)
    }

    /**
     * Оставляет в директории только указанные файлы, переименовывает их и удаляет остальные
     * @param directory Целевая директория
     * @param keepFiles Список имён файлов для сохранения (регистрозависимый)
     * @param renameRule Функция для генерации нового имени файла на основе старого
     */
    fun filterAndRenameFiles(
        directory: File,
        keepFiles: Set<String>,
        renameRule: (String) -> String
    ) {
        if (!directory.isDirectory) return

        directory.listFiles()?.forEach { file ->
            when {
                file.isFile && file.name in keepFiles -> {
                    val newName = renameRule(file.name)
                    val newFile = File(directory, newName)
                    if (newName != file.name) {
                        if (newFile.exists()) newFile.delete()
                        file.renameTo(newFile)
                    }
                }
                else -> {
                    if (!file.isDirectory) {
                        file.delete()
                    }
                }
            }
        }
    }
}
