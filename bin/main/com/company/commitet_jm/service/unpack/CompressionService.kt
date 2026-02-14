package com.company.commitet_jm.service.unpack

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.zip.DataFormatException
import java.util.zip.Deflater
import java.util.zip.Inflater
import kotlin.math.max
import kotlin.math.min


/**
 * Сервис для работы со сжатием данных в файлах 1С:Предприятия
 *
 * Этот сервис объединяет функции сжатия и распаковки данных.
 */
class CompressionService {
    /**
     * Сжать данные с использованием алгоритма Deflate
     * @param data данные для сжатия
     * @return сжатые данные
     * @throws IOException если произошла ошибка ввода-вывода
     */
    @Throws(IOException::class)
    fun compress(data: ByteArray?): ByteArray? {
        if (data == null) {
            return ByteArray(0)
        }


        // Создаем сжиматель
        val deflater = Deflater(Deflater.BEST_COMPRESSION)
        deflater.setInput(data)
        deflater.finish()


        // Буфер для сжатых данных
        val outputStream = ByteArrayOutputStream(data.size)

        val buffer = ByteArray(1024)
        while (!deflater.finished()) {
            val count = deflater.deflate(buffer)
            outputStream.write(buffer, 0, count)
        }


        // Закрываем ресурсы
        deflater.end()
        outputStream.close()

        return outputStream.toByteArray()
    }

    /**
     * Распаковать данные с использованием алгоритма Inflate
     * @param compressedData сжатые данные
     * @return распакованные данные
     * @throws IOException если произошла ошибка ввода-вывода
     * @throws DataFormatException если данные имеют неверный формат
     */
    @Throws(IOException::class, DataFormatException::class)
    fun decompress(compressedData: ByteArray?): ByteArray {
        if (compressedData == null || compressedData.size == 0) {
            return ByteArray(0)
        }


        // Создаем распаковщик (nowrap=true для raw DEFLATE, как в 1C)
        val inflater = Inflater(true)
        inflater.setInput(compressedData)


        // Буфер для распакованных данных
        val initialSize = max(1024, min(1 shl 20, compressedData.size * 2))
        val outputStream = ByteArrayOutputStream(initialSize)

        val buffer = ByteArray(8192)
        var noProgressIters = 0
        while (!inflater.finished()) {
            val count = inflater.inflate(buffer)
            if (count > 0) {
                outputStream.write(buffer, 0, count)
                noProgressIters = 0
                continue
            }
            // Нет прогресса
            if (inflater.needsDictionary()) {
                inflater.end()
                outputStream.close()
                throw DataFormatException("Inflater requires a preset dictionary")
            }
            if (inflater.needsInput()) {
                // Вход исчерпан, но finished=false => данные обрезаны/повреждены
                inflater.end()
                outputStream.close()
                throw DataFormatException("Truncated or invalid deflate stream (needs input before finished)")
            }
            if (++noProgressIters > 5) {
                inflater.end()
                outputStream.close()
                throw DataFormatException("Inflater made no progress")
            }
        }


        // Закрываем ресурсы
        inflater.end()
        outputStream.close()

        return outputStream.toByteArray()
    }

    /**
     * Распаковать данные с указанием размера выходных данных
     * @param compressedData сжатые данные
     * @param uncompressedSize размер выходных данных
     * @return распакованные данные
     * @throws IOException если произошла ошибка ввода-вывода
     * @throws DataFormatException если данные имеют неверный формат
     */
    @Throws(IOException::class, DataFormatException::class)
    fun decompress(compressedData: ByteArray?, uncompressedSize: Int): ByteArray {
        if (compressedData == null || compressedData.size == 0) {
            return ByteArray(0)
        }


        // Создаем распаковщик (nowrap=true для raw DEFLATE, как в 1C)
        val inflater = Inflater(true)
        inflater.setInput(compressedData)


        // Буфер для распакованных данных
        val initialSize =
            if (uncompressedSize > 0) uncompressedSize else max(1024, min(1 shl 20, compressedData.size * 2))
        val outputStream = ByteArrayOutputStream(initialSize)

        val buffer = ByteArray(8192)
        var noProgressIters = 0
        while (!inflater.finished()) {
            val count = inflater.inflate(buffer)
            if (count > 0) {
                outputStream.write(buffer, 0, count)
                noProgressIters = 0
                continue
            }
            if (inflater.needsDictionary()) {
                inflater.end()
                outputStream.close()
                throw DataFormatException("Inflater requires a preset dictionary")
            }
            if (inflater.needsInput()) {
                inflater.end()
                outputStream.close()
                throw DataFormatException("Truncated or invalid deflate stream (needs input before finished)")
            }
            if (++noProgressIters > 5) {
                inflater.end()
                outputStream.close()
                throw DataFormatException("Inflater made no progress")
            }
        }


        // Закрываем ресурсы
        inflater.end()
        outputStream.close()

        return outputStream.toByteArray()
    }

    /**
     * Сжать данные с использованием алгоритма Deflate с указанием уровня сжатия
     * @param data данные для сжатия
     * @param compressionLevel уровень сжатия (0-9)
     * @return сжатые данные
     * @throws IOException если произошла ошибка ввода-вывода
     */
    @Throws(IOException::class)
    fun compress(data: ByteArray?, compressionLevel: Int): ByteArray? {
        var compressionLevel = compressionLevel
        if (data == null) {
            return ByteArray(0)
        }


        // Проверяем уровень сжатия
        if (compressionLevel < 0 || compressionLevel > 9) {
            compressionLevel = Deflater.BEST_COMPRESSION
        }


        // Создаем сжиматель
        val deflater = Deflater(compressionLevel)
        deflater.setInput(data)
        deflater.finish()


        // Буфер для сжатых данных
        val outputStream = ByteArrayOutputStream(data.size)

        val buffer = ByteArray(1024)
        while (!deflater.finished()) {
            val count = deflater.deflate(buffer)
            outputStream.write(buffer, 0, count)
        }


        // Закрываем ресурсы
        deflater.end()
        outputStream.close()

        return outputStream.toByteArray()
    }
}