package com.company.commitet_jm.service.unpack

import java.io.IOException
import java.util.zip.DataFormatException


/**
 * Сервис для распаковки (инфляции) сжатых данных
 *
 * Этот сервис отвечает за распаковку сжатых данных из файлов 1С.
 */
class InflateService {
    private val compressionService: CompressionService

    /**
     * Конструктор по умолчанию
     */
    init {
        this.compressionService = CompressionService()
    }

    /**
     * Распаковать (инфлатировать) сжатые данные
     * @param compressedData сжатые данные
     * @return распакованные данные
     * @throws IOException если произошла ошибка ввода-вывода
     * @throws DataFormatException если данные имеют неверный формат
     */
    @Throws(IOException::class, DataFormatException::class)
    fun inflate(compressedData: ByteArray?): ByteArray {
        return compressionService.decompress(compressedData)
    }

    /**
     * Распаковать (инфлатировать) сжатые данные с указанием размера выходных данных
     * @param compressedData сжатые данные
     * @param uncompressedSize размер выходных данных
     * @return распакованные данные
     * @throws IOException если произошла ошибка ввода-вывода
     * @throws DataFormatException если данные имеют неверный формат
     */
    @Throws(IOException::class, DataFormatException::class)
    fun inflate(compressedData: ByteArray?, uncompressedSize: Int): ByteArray {
        return compressionService.decompress(compressedData, uncompressedSize)
    }

    /**
     * Распаковать данные из файла в файл
     * @param inputFilePath путь к файлу со сжатыми данными
     * @param outputFilePath путь к файлу для записи распакованных данных
     * @throws IOException если произошла ошибка ввода-вывода
     * @throws DataFormatException если данные имеют неверный формат
     */
    @Throws(IOException::class, DataFormatException::class)
    fun inflateFile(inputFilePath: String?, outputFilePath: String?) {
        // Читаем сжатые данные из файла
        val compressedData: ByteArray? = com.company.commitet_jm.service.unpack.utils.FileUtils.readFileToByteArray(inputFilePath)


        // Распаковываем данные
        val uncompressedData = inflate(compressedData)


        // Записываем распакованные данные в файл
        com.company.commitet_jm.service.unpack.utils.FileUtils.writeByteArrayToFile(outputFilePath, uncompressedData)
    }
}