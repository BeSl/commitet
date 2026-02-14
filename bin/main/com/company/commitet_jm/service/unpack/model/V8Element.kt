package com.company.commitet_jm.service.unpack.model

import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import kotlin.math.min


/**
 * Представление элемента файла 1С:Предприятия
 *
 * Этот класс содержит информацию об отдельном элементе файла 1С, таком как
 * модуль, форма, макет и т.д.
 */
class V8Element {
    // Заголовок элемента
    private var header: ByteArray?

    /**
     * Получить размер заголовка элемента
     * @return размер заголовка элемента
     */
    /**
     * Установить размер заголовка элемента
     * @param headerSize размер заголовка элемента
     */
    // Размер заголовка элемента
    var headerSize: Int

    // Данные элемента
    private var data: ByteArray?

    /**
     * Получить размер данных элемента
     * @return размер данных элемента
     */
    /**
     * Установить размер данных элемента
     * @param dataSize размер данных элемента
     */
    // Размер данных элемента
    var dataSize: Int

    // Распакованные данные
    private var unpackedData: V8File?

    /**
     * Проверить, является ли файлом V8
     * @return true, если является файлом V8, иначе false
     */
    /**
     * Установить, является ли файлом V8
     * @param isV8File true, если является файлом V8, иначе false
     */
    // Является ли файлом V8
    var isV8File: Boolean

    /**
     * Проверить, требуется ли распаковка
     * @return true, если требуется распаковка, иначе false
     */
    /**
     * Установить, требуется ли распаковка
     * @param this.isNeedUnpack true, если требуется распаковка, иначе false
     */
    // Требуется ли распаковка
    var isNeedUnpack: Boolean

    /**
     * Конструктор по умолчанию
     */
    constructor() {
        this.header = ByteArray(0)
        this.headerSize = 0
        this.data = ByteArray(0)
        this.dataSize = 0
        this.unpackedData = V8File()
        this.isV8File = false
        this.isNeedUnpack = false
    }

    /**
     * Конструктор с параметрами
     * @param header заголовок элемента
     * @param data данные элемента
     */
    constructor(header: ByteArray?, data: ByteArray?) {
        this.header = if (header != null) header.copyOf(header.size) else ByteArray(0)
        this.headerSize = this.header!!.size
        this.data = if (data != null) data.copyOf(data.size) else ByteArray(0)
        this.dataSize = this.data!!.size
        this.unpackedData = V8File()
        this.isV8File = false
        this.isNeedUnpack = false
    }

    /**
     * Получить заголовок элемента
     * @return заголовок элемента
     */
    fun getHeader(): ByteArray? {
        return if (header != null) header!!.copyOf(header!!.size) else ByteArray(0)
    }

    /**
     * Установить заголовок элемента
     * @param header заголовок элемента
     */
    fun setHeader(header: ByteArray?) {
        this.header = if (header != null) header.copyOf(header.size) else ByteArray(0)
        this.headerSize = this.header!!.size
    }

    /**
     * Получить данные элемента
     * @return данные элемента
     */
    fun getData(): ByteArray? {
        return if (data != null) data!!.copyOf(data!!.size) else ByteArray(0)
    }

    /**
     * Установить данные элемента
     * @param data данные элемента
     */
    fun setData(data: ByteArray?) {
        this.data = if (data != null) data.copyOf(data.size) else ByteArray(0)
        this.dataSize = this.data!!.size
    }

    /**
     * Получить распакованные данные
     * @return распакованные данные
     */
    fun getUnpackedData(): V8File? {
        return unpackedData
    }

    /**
     * Установить распакованные данные
     * @param unpackedData распакованные данные
     */
    fun setUnpackedData(unpackedData: V8File?) {
        this.unpackedData = unpackedData
    }

    var elementName: String?
        /**
         * Получить имя элемента из заголовка
         * @return имя элемента
         */
        get() {
            if (header == null || header!!.size < headerBeginSize) {
                return ""
            }
            var pos = headerBeginSize
            val nameBytes = ByteArrayOutputStream()
            // Читаем UTF-16LE до двойного нулевого символа (0x0000 0x0000)
            var lastWasZeroWide = false
            while (pos + 1 < header!!.size) {
                val lo = header!![pos].toInt() and 0xFF
                val hi = header!![pos + 1].toInt() and 0xFF
                pos += 2
                if (lo == 0 && hi == 0) {
                    if (lastWasZeroWide) {
                        // встретили 0x0000 дважды подряд — терминатор
                        break
                    } else {
                        // первый нулевой wide, возможный конец — запомним и продолжим, чтобы подтвердить двойной ноль
                        lastWasZeroWide = true
                        continue
                    }
                }
                lastWasZeroWide = false
                nameBytes.write(lo)
                nameBytes.write(hi)
            }
            return String(nameBytes.toByteArray(), StandardCharsets.UTF_16LE).trim { it <= ' ' }
        }
        /**
         * Установить имя элемента в заголовке
         * @param name имя элемента
         */
        set(name) {
            var name = name
            if (name == null) {
                name = ""
            }


            // Вычисляем новый размер заголовка
            val headerBeginSize = headerBeginSize
            val newNameSize = headerBeginSize + name.length * 2 + 4 // +4 для завершающих нулей


            // Создаем новый заголовок
            val newHeader = ByteArray(newNameSize)


            // Копируем существующий заголовок или создаем новый
            if (header != null && header!!.size > 0) {
                val copyLength = min(headerBeginSize, header!!.size)
                System.arraycopy(header, 0, newHeader, 0, copyLength)
            }


            // Заполняем имя элемента
            for (i in 0..<name.length) {
                val pos = headerBeginSize + i * 2
                if (pos < newHeader.size) {
                    newHeader[pos] = name.get(i).code.toByte()
                }
                // Следующий байт остается 0 (Unicode)
                if (pos + 1 < newHeader.size) {
                    newHeader[pos + 1] = 0
                }
            }


            // Завершающие нули (4 байта)
            val endPos = headerBeginSize + name.length * 2
            var i = 0
            while (i < 4 && (endPos + i) < newHeader.size) {
                newHeader[endPos + i] = 0
                i++
            }

            this.header = newHeader
            this.headerSize = newHeader.size
        }

    companion object {
        val headerBeginSize: Int
            /**
             * Получить размер заголовка элемента
             * @return размер заголовка элемента в байтах
             */
            get() = 8 + 8 + 4 // date_creation (8) + date_modification (8) + res (4) = 20 байт
    }
}