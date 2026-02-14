package com.company.commitet_jm.service.unpack


import com.company.commitet_jm.service.unpack.model.ElementAddress
import com.company.commitet_jm.service.unpack.model.FileHeader
import com.company.commitet_jm.service.unpack.model.V8Element
import com.company.commitet_jm.service.unpack.model.V8File
import com.company.commitet_jm.service.unpack.utils.FileUtils
import com.company.commitet_jm.service.unpack.utils.MemoryUtils.byteArrayToInt
import java.io.IOException
import kotlin.math.max
import kotlin.math.min

/**
 * Сервис для парсинга файлов 1С:Предприятия
 *
 * Этот сервис отвечает за парсинг файлов 1С и создание объектной модели.
 */
class ParseService {
    // private static final int CHUNK_SIZE = 1024;
    /**
     * Распарсить файл 1С и создать объектную модель
     * @param filePath путь к файлу 1С для парсинга
     * @return объект V8File с распарсенной структурой
     * @throws IOException если произошла ошибка ввода-вывода
     */
    @Throws(IOException::class)
    fun parse(filePath: String?): V8File {
        // Читаем файл в массив байтов
        val data: ByteArray = FileUtils.readFileToByteArray(filePath)
        return parse(data)
    }

    /**
     * Распарсить файл 1С из массива байтов и создать объектную модель
     * @param data массив байтов файла 1С для парсинга
     * @return объект V8File с распарсенной структурой
     * @throws IOException если произошла ошибка ввода-вывода
     */
    @Throws(IOException::class)
    fun parse(data: ByteArray): V8File {
        if (data.isEmpty()) {
            throw IOException("Данные файла пусты")
        }


        // Проверяем, является ли файл файлом 1С V8
        if (!V8File.isV8File(data)) {
            throw IOException("Файл не является файлом 1С V8")
        }

        val v8File = V8File()


        // Парсим заголовок файла
        parseFileHeader(data, v8File)


        // Парсим адреса элементов
        val elemAddrs = parseElementAddresses(data, v8File.header)
        v8File.setElemsAddrs(elemAddrs)


        // Парсим элементы
        val elements = parseElements(data, elemAddrs)
        v8File.setElements(elements)

        return v8File
    }

    /**
     * Распарсить заголовок файла
     * @param data данные файла
     * @param v8File объект V8File для заполнения
     * @throws IOException если произошла ошибка ввода-вывода
     */
    @Throws(IOException::class)
    private fun parseFileHeader(data: ByteArray, v8File: V8File) {
        if (data.size < FileHeader.size) {
            throw IOException("Недостаточно данных для заголовка файла")
        }

        val header = FileHeader()
        var offset = 0


        // Считываем поля заголовка
        header.nextPageAddr = byteArrayToInt(data, offset)
        offset += 4

        header.pageSize = byteArrayToInt(data, offset)
        offset += 4

        header.storageVersion = byteArrayToInt(data, offset)
        offset += 4

        header.reserved = byteArrayToInt(data, offset)

        v8File.header = header
    }

    /**
     * Распарсить адреса элементов
     * @param data данные файла
     * @param fileHeader заголовок файла
     * @return список адресов элементов
     * @throws IOException если произошла ошибка ввода-вывода
     */
    @Throws(IOException::class)
    private fun parseElementAddresses(data: ByteArray, fileHeader: FileHeader): MutableList<ElementAddress> {
        val elemAddrs: MutableList<ElementAddress> = ArrayList<ElementAddress>()


        // Адрес блока адресов элементов
        val blockAddr: Int = FileHeader.size


        // Читаем заголовок блока адресов
        // Заголовок 1С — текстовый: "dddddddd pppppppp 7fffffff[ ]\r\n"
        val hdr = scanAndParseHeader(data, blockAddr, min(data.size, blockAddr + 4096))
        if (hdr == null) {
            throw IOException("Не найден корректный заголовок блока адресов элементов")
        }
        // Реальный размер страницы и данных блока адресов
        // int pageSize = hdr.pageSize; // при необходимости используем далее для навигации по страницам
        val blockSize = hdr.dataSize


        // Начало данных сразу после строки заголовка
        val dataStartAddr = hdr.lineEnd


        // Читаем адреса элементов
        var addrOffset = dataStartAddr
        while (addrOffset + ElementAddress.size <= data.size &&
            addrOffset < dataStartAddr + blockSize
        ) {
            val elemAddr = ElementAddress()

            elemAddr.elemHeaderAddr = byteArrayToInt(data, addrOffset)
            addrOffset += 4

            elemAddr.elemDataAddr = byteArrayToInt(data, addrOffset)
            addrOffset += 4

            elemAddr.ffSignature = byteArrayToInt(data, addrOffset)
            addrOffset += 4


            // Проверяем сигнатуру
            if (elemAddr.ffSignature != FileHeader.FF_SIGNATURE) {
                break // Достигли конца списка адресов
            }

            elemAddrs.add(elemAddr)
        }

        return elemAddrs
    }

    // --- Вспомогательные методы разбора текстового заголовка блока ---
    private class Header {
        var dataSize: Int = 0

        @Suppress("unused")
        var pageSize: Int = 0

        @Suppress("unused")
        var nextPage: Int = 0
        var lineEnd: Int = 0 // позиция после CRLF
    }

    private fun scanAndParseHeader(data: ByteArray, from: Int, to: Int): Header? {
        val end = min(data.size, max(from, to))
        var i = max(0, from)
        while (i + 27 < end) {
            if (isHex8(data, i) && isSpace(data, i + 8) && isHex8(data, i + 9) && isSpace(data, i + 17)) {
                val pos = i + 18
                if (pos + 8 <= data.size && matches7fffffff(data, pos)) {
                    var crlf = pos + 8
                    if (crlf < data.size && isSpace(data, crlf)) crlf++
                    if (crlf + 1 < data.size && data[crlf] == '\r'.code.toByte() && data[crlf + 1] == '\n'.code.toByte()) {
                        return parseHeaderAt(data, i, crlf + 2)
                    }
                }
            }
            i++
        }
        return null
    }

    private fun parseHeaderAt(data: ByteArray, offset: Int, lineEnd: Int): Header? {
        try {
            val d = parseHex8(data, offset)
            val p = parseHex8(data, offset + 9)
            val n = parseHex8(data, offset + 18)
            val h = Header()
            h.dataSize = d
            h.pageSize = p
            h.nextPage = n
            h.lineEnd = lineEnd
            return h
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

    private fun matches7fffffff(data: ByteArray, pos: Int): Boolean {
        if (pos + 8 > data.size) return false
        val s = "7fffffff"
        for (i in 0..7) {
            if (Char(data[pos + i].toUShort()).lowercaseChar() != s.get(i)) return false
        }
        return true
    }

    private fun parseHex8(data: ByteArray, pos: Int): Int {
        var `val` = 0
        for (i in 0..7) {
            val c = Char(data[pos + i].toUShort()).lowercaseChar().code
            val d: Int
            if (c >= '0'.code && c <= '9'.code) d = c - '0'.code
            else if (c >= 'a'.code && c <= 'f'.code) d = 10 + (c - 'a'.code)
            else throw IllegalArgumentException()
            `val` = (`val` shl 4) or d
        }
        return `val`
    }

    /**
     * Распарсить элементы файла
     * @param data данные файла
     * @param elemAddrs адреса элементов
     * @return список элементов
     * @throws IOException если произошла ошибка ввода-вывода
     */
    @Throws(IOException::class)
    private fun parseElements(data: ByteArray, elemAddrs: MutableList<ElementAddress>): MutableList<V8Element?> {
        val elements: MutableList<V8Element?> = ArrayList<V8Element?>()

        for (addr in elemAddrs) {
            val element = V8Element()


            // Читаем заголовок элемента по алгоритму V8: ReadBlockData
            val headerAddr = addr.elemHeaderAddr
            if (headerAddr >= 0 && headerAddr < data.size) {
                val headerData: ByteArray? = com.company.commitet_jm.service.unpack.utils.V8BlockReader.readBlockData(data, headerAddr)
                element.setHeader(headerData)
            }


            // Читаем данные элемента
            val dataAddr = addr.elemDataAddr
            if (dataAddr == FileHeader.FF_SIGNATURE) {
                element.setData(ByteArray(0))
            } else if (dataAddr >= 0 && dataAddr < data.size) {
                val elemData: ByteArray? = com.company.commitet_jm.service.unpack.utils.V8BlockReader.readBlockData(data, dataAddr)
                element.setData(elemData)
            }

            elements.add(element)
        }

        return elements
    }
}