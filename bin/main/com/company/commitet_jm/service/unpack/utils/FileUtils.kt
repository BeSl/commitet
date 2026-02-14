package com.company.commitet_jm.service.unpack.utils

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths


/**
 * Утилиты для работы с файлами
 *
 * Этот класс содержит вспомогательные методы для работы с файлами,
 * такие как чтение, запись и другие операции.
 */
object FileUtils {
    /**
     * Прочитать файл в массив байтов
     * @param filePath путь к файлу для чтения
     * @return массив байтов файла
     * @throws IOException если произошла ошибка ввода-вывода
     */
    @Throws(IOException::class)
    fun readFileToByteArray(filePath: String?): ByteArray {
        if (filePath == null || filePath.isEmpty()) {
            throw IOException("Путь к файлу не может быть пустым")
        }

        val path = Paths.get(filePath)
        if (!Files.exists(path)) {
            throw IOException("Файл не существует: " + filePath)
        }

        return Files.readAllBytes(path)
    }

    /**
     * Записать массив байтов в файл
     * @param filePath путь к файлу для записи
     * @param data массив байтов для записи
     * @throws IOException если произошла ошибка ввода-вывода
     */
    @Throws(IOException::class)
    fun writeByteArrayToFile(filePath: String?, data: ByteArray?) {
        var data = data
        if (filePath == null || filePath.isEmpty()) {
            throw IOException("Путь к файлу не может быть пустым")
        }

        if (data == null) {
            data = ByteArray(0)
        }

        val path = Paths.get(filePath)
        val parent = path.getParent()


        // Создаем директории, если они не существуют
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent)
        }

        Files.write(path, data)
    }

    /**
     * Проверить, существует ли файл
     * @param filePath путь к файлу для проверки
     * @return true, если файл существует, иначе false
     */
    fun fileExists(filePath: String?): Boolean {
        if (filePath == null || filePath.isEmpty()) {
            return false
        }

        return Files.exists(Paths.get(filePath))
    }

    /**
     * Проверить, существует ли директория
     * @param dirPath путь к директории для проверки
     * @return true, если директория существует, иначе false
     */
    fun directoryExists(dirPath: String?): Boolean {
        if (dirPath == null || dirPath.isEmpty()) {
            return false
        }

        return Files.exists(Paths.get(dirPath)) && Files.isDirectory(Paths.get(dirPath))
    }

    /**
     * Создать директорию
     * @param dirPath путь к директории для создания
     * @throws IOException если произошла ошибка ввода-вывода
     */
    @Throws(IOException::class)
    fun createDirectory(dirPath: String?) {
        if (dirPath == null || dirPath.isEmpty()) {
            throw IOException("Путь к директории не может быть пустым")
        }

        val path = Paths.get(dirPath)
        if (!Files.exists(path)) {
            Files.createDirectories(path)
        }
    }

    /**
     * Получить размер файла
     * @param filePath путь к файлу
     * @return размер файла в байтах
     * @throws IOException если произошла ошибка ввода-вывода
     */
    @Throws(IOException::class)
    fun getFileSize(filePath: String): Long {
        if (filePath == null || filePath.isEmpty()) {
            throw IOException("Путь к файлу не может быть пустым")
        }

        val path = Paths.get(filePath)
        if (!Files.exists(path)) {
            throw IOException("Файл не существует: " + filePath)
        }

        return Files.size(path)
    }

    /**
     * Удалить файл
     * @param filePath путь к файлу для удаления
     * @throws IOException если произошла ошибка ввода-вывода
     */
    @Throws(IOException::class)
    fun deleteFile(filePath: String) {
        if (filePath == null || filePath.isEmpty()) {
            throw IOException("Путь к файлу не может быть пустым")
        }

        val path = Paths.get(filePath)
        if (Files.exists(path)) {
            Files.delete(path)
        }
    }

    /**
     * Получить имя файла без пути
     * @param filePath путь к файлу
     * @return имя файла
     */
    fun getFileName(filePath: String?): String {
        if (filePath == null || filePath.isEmpty()) {
            return ""
        }

        val path = Paths.get(filePath)
        return path.getFileName().toString()
    }

    /**
     * Получить расширение файла
     * @param filePath путь к файлу
     * @return расширение файла
     */
    fun getFileExtension(filePath: String?): String {
        if (filePath == null || filePath.isEmpty()) {
            return ""
        }

        val fileName = getFileName(filePath)
        val dotIndex = fileName.lastIndexOf('.')
        if (dotIndex > 0 && dotIndex < fileName.length - 1) {
            return fileName.substring(dotIndex + 1)
        }

        return ""
    }
}