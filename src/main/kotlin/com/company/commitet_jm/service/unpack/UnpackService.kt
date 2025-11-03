package com.company.commitet_jm.service.unpack

import com.company.commitet_jm.service.unpack.model.V8File
import com.company.commitet_jm.service.unpack.utils.FileUtils
import java.io.IOException
import java.util.*
import java.util.zip.DataFormatException
import kotlin.math.max
import kotlin.math.min

/**
 * Сервис для распаковки файлов 1С:Предприятия
 *
 * Этот сервис отвечает за распаковку файлов 1С (.cf, .epf, .erf и др.) в
 * отдельные компоненты, такие как модули, формы, макеты и т.д.
 */
class UnpackService {
    private val inflateService: InflateService = InflateService()
    private val parseService: ParseService = ParseService()

    /**
     * Распаковать файл 1С
     * @param filePath путь к файлу 1С для распаковки
     * @return объект V8File с распакованной структурой
     * @throws IOException если произошла ошибка ввода-вывода
     */
    @Throws(IOException::class)
    fun unpack(filePath: String?): V8File {
        // Читаем файл в массив байтов
        val data: ByteArray = FileUtils.readFileToByteArray(filePath)
        return unpack(data)
    }

    /**
     * Распаковать файл 1С из массива байтов
     * @param data массив байтов файла 1С для распаковки
     * @return объект V8File с распакованной структурой
     * @throws IOException если произошла ошибка ввода-вывода
     */
    @Throws(IOException::class)
    fun unpack(data: ByteArray): V8File {
        if (data == null || data.size == 0) {
            throw IOException("Данные файла пусты")
        }


        // Проверяем, является ли файл файлом 1С V8
        if (!V8File.isV8File(data)) {
            throw IOException("Файл не является файлом 1С V8")
        }


        // Парсим файл
        val v8File: V8File = parseService.parse(data)


        // Распаковываем данные элементов
        unpackElements(v8File)

        return v8File
    }

    /**
     * Распаковать данные элементов
     * @param v8File объект V8File
     * @throws IOException если произошла ошибка ввода-вывода
     */
    @Throws(IOException::class)
    private fun unpackElements(v8File: V8File) {
        for (element in v8File.elements!!) {
            try {
                // Проверяем, являются ли данные сжатыми
                val data: ByteArray? = element.getData()
                if (data != null && data.size > 0) {
                    // Пытаемся распаковать данные
                    val uncompressedData: ByteArray = inflateService.inflate(data)


                    // Проверяем, является ли распакованный файл V8 файлом
                    if (V8File.isV8File(uncompressedData)) {
                        element.isV8File = true
                        // Рекурсивно распаковываем вложенный файл
                        val nestedFile: V8File = unpack(uncompressedData)
                        element.setUnpackedData(nestedFile)
                    } else {
                        // Просто устанавливаем распакованные данные
                        element.setData(uncompressedData)
                    }
                }
            } catch (e: DataFormatException) {
                // Если не удалось распаковать, оставляем данные как есть
                // Это может быть несжатый файл или файл с другим форматом
                element.isV8File = false
            }
        }
    }

    /**
     * Распаковать файл 1С в директорию
     * @param inputFilePath путь к файлу 1С для распаковки
     * @param outputDirPath путь к директории для распаковки
     * @throws IOException если произошла ошибка ввода-вывода
     */
    @Throws(IOException::class)
    fun unpackToDirectory(inputFilePath: String?, outputDirPath: String?) {
        // Создаем директорию, если она не существует
        FileUtils.createDirectory(outputDirPath)


        // Распаковываем файл
        val v8File: V8File = unpack(inputFilePath)


        // Если элементы распарсились — сохраняем как обычно
        if (v8File.elementCount > 0) {
            saveElementsToDirectory(v8File, outputDirPath)
            return
        }


        // Фолбэк: постранично извлечь блоки по текстовым заголовкам 1С
        val fileBytes: ByteArray = FileUtils.readFileToByteArray(inputFilePath)
        dumpBlocksFallback(fileBytes, outputDirPath)
    }

    /**
     * Сохранить элементы в директорию
     * @param v8File объект V8File
     * @param outputDirPath путь к директории для сохранения
     * @throws IOException если произошла ошибка ввода-вывода
     */
    @Throws(IOException::class)
    private fun saveElementsToDirectory(v8File: V8File?, outputDirPath: String?) {
        for (element in v8File!!.elements!!) {
            var elementName: String? = element.elementName
            if (elementName == null || elementName.isEmpty()) {
                elementName = "element_" + System.currentTimeMillis()
            }

            if (element.isV8File) {
                // Сохраняем вложенный файл как директорию
                val nestedDirPath = outputDirPath + "/" + elementName
                FileUtils.createDirectory(nestedDirPath)
                saveElementsToDirectory(element.getUnpackedData(), nestedDirPath)
            } else {
                // Сохраняем данные элемента как файл
                val elementFilePath = outputDirPath + "/" + elementName
                FileUtils.writeByteArrayToFile(elementFilePath, element.getData())
            }
        }
    }

    // --- Fallback block extractor for simple cases (text headers + raw data) ---
    @Throws(IOException::class)
    private fun dumpBlocksFallback(data: ByteArray, outDir: String?) {
        var pos = 0
        val limit = data.size
        var blockIndex = 0
        while (pos < limit) {
            val headerPos: Int = findHeader(data, pos, min(limit, pos + 1000000))
            if (headerPos < 0) break
            val hdr: HeaderParseResult? = parseHeader(data, headerPos)
            if (hdr == null) {
                pos = headerPos + 1
                continue
            }
            val dataStart = hdr.lineEnd
            val dataEnd = min(limit, dataStart + hdr.dataSize)
            // validate expected next header position to reduce false positives inside payload
            val expectedNext = dataStart + hdr.dataSize
            if (expectedNext + 27 <= limit) {
                val probe: Int = findHeader(data, expectedNext, expectedNext + 28)
                if (probe != expectedNext) {
                    pos = headerPos + 1
                    continue
                }
            }
            if (dataStart > limit) break
            val block = ByteArray(max(0, dataEnd - dataStart))
            if (block.size > 0) System.arraycopy(data, dataStart, block, 0, block.size)
            val baseName = String.format("block_%04d", blockIndex++)
            FileUtils.writeByteArrayToFile(outDir + "/" + baseName + ".bin", block)
            // Попробуем распаковать как raw DEFLATE
            try {
                val und: ByteArray? = inflateService.inflate(block)
                if (und != null && und.size > 0) {
                    FileUtils.writeByteArrayToFile(outDir + "/" + baseName + ".und", und)
                }
            } catch (ignore: Exception) {
                // оставляем как есть
            }
            pos = dataEnd
        }
    }

    private class HeaderParseResult {
        var dataSize: Int = 0

        @Suppress("unused")
        var pageSize: Int = 0

        @Suppress("unused")
        var nextPage: Int = 0
        var lineEnd: Int = 0 // позиция после CRLF
    }

    private fun findHeader(data: ByteArray, from: Int, to: Int): Int {
        val end = min(to, data.size)
        var i = max(0, from)
        while (i + 27 < end) {
            if (isHex8(data, i) && isSpace(data, i + 8) && isHex8(data, i + 9) && isSpace(data, i + 17)) {
                val pos = i + 18
                if (matches7fffffff(data, pos)) {
                    var crlf = pos + 8
                    if (crlf < data.size && isSpace(data, crlf)) crlf++
                    if (crlf + 1 < data.size && data[crlf] == '\r'.code.toByte() && data[crlf + 1] == '\n'.code.toByte()) {
                        return i
                    }
                }
            }
            i++
        }
        return -1
    }

    private fun matches7fffffff(data: ByteArray, pos: Int): Boolean {
        if (pos + 8 > data.size) return false
        val s = "7fffffff"
        for (i in 0..7) {
            if (Char(data[pos + i].toUShort()).lowercaseChar() != s.get(i)) return false
        }
        return true
    }

    private fun parseHeader(data: ByteArray, offset: Int): HeaderParseResult? {
        try {
            val h1: String = readAscii(data, offset, 8)
            val h2: String = readAscii(data, offset + 9, 8)
            val h3: String = readAscii(data, offset + 18, 8)
            var pos = offset + 26
            if (pos < data.size && isSpace(data, pos)) pos++
            if (!(pos + 1 < data.size && data[pos] == '\r'.code.toByte() && data[pos + 1] == '\n'.code.toByte())) return null
            val r = HeaderParseResult()
            r.dataSize = h1.toInt(16)
            r.pageSize = h2.toInt(16)
            r.nextPage = h3.toInt(16)
            r.lineEnd = pos + 2
            if ("7fffffff" != h3) return null
            if (r.pageSize < 256 || r.pageSize > (1 shl 20) || (r.pageSize % 256) != 0) return null
            if (r.dataSize < 0 || r.dataSize > r.pageSize) return null
            return r
        } catch (e: Exception) {
            return null
        }
    }

    private fun isHex8(data: ByteArray, pos: Int): Boolean {
        if (pos + 8 > data.size) return false
        for (i in 0..7) {
            val ch = Char(data[pos + i].toUShort()).lowercaseChar()
            if (!((ch >= '0' && ch <= '9') || (ch >= 'a' && ch <= 'f'))) return false
        }
        return true
    }

    private fun isSpace(data: ByteArray, pos: Int): Boolean {
        return pos < data.size && data[pos].toInt() == 0x20
    }

    private fun readAscii(data: ByteArray, pos: Int, len: Int): String {
        val sb = StringBuilder(len)
        var i = 0
        while (i < len && pos + i < data.size) {
            sb.append(Char(data[pos + i].toUShort()))
            i++
        }
        return sb.toString().trim { it <= ' ' }.lowercase(Locale.getDefault())
    }
}