package com.company.commitet_jm.service.unpack.model

import kotlin.math.min


/**
 * Представление файла 1С:Предприятия (cf, epf, erf и др.)
 *
 * Этот класс содержит информацию о структуре файла 1С, включая заголовок файла,
 * элементы и блоки данных.
 */
class V8File {

    /**
     * Установить заголовок файла
     * @param this.header заголовок файла
     */
    // Заголовок файла
    var header: FileHeader

    /**
     * Получить адреса элементов
     * @return список адресов элементов
     */
    // Адреса элементов
    var elemsAddrs: MutableList<ElementAddress> = ArrayList()
        private set

    /**
     * Получить элементы файла
     * @return список элементов файла
     */
    // Элементы файла
    var elements: MutableList<V8Element> = ArrayList()
        private set


    /**
     * Проверить, упакованы ли данные
     * @return true, если данные упакованы, иначе false
     */
    /**
     * Установить, упакованы ли данные
     * @param isDataPacked true, если данные упакованы, иначе false
     */
    // Упакованы ли данные
    var isDataPacked: Boolean

    /**
     * Конструктор по умолчанию
     */
    constructor() {
        this.header = FileHeader()
        this.elemsAddrs = ArrayList<ElementAddress>()
        this.elements = ArrayList<V8Element>()
        this.isDataPacked = true
    }

    /**
     * Конструктор копирования
     * @param src исходный объект V8File
     */
    constructor(src: V8File) {
        this.header = FileHeader(
            src.header.nextPageAddr,
            src.header.pageSize,
            src.header.storageVersion,
            src.header.reserved
        )

        this.elemsAddrs = ArrayList<ElementAddress>()
        for (addr in src.elemsAddrs) {
            this.elemsAddrs.add(
                ElementAddress(
                    addr.elemHeaderAddr,
                    addr.elemDataAddr,
                    addr.ffSignature
                )
            )
        }

        this.elements = ArrayList<V8Element>()
        for (elem in src.elements!!) {
            // Глубокое копирование элемента
            val newElem = V8Element()
            newElem.setHeader(elem.getHeader())
            newElem.setData(elem.getData())
            newElem.isV8File = elem.isV8File
            newElem.isNeedUnpack = elem.isNeedUnpack
            // Копируем распакованные данные
            newElem.setUnpackedData(V8File(elem.getUnpackedData()!!))
            this.elements!!.add(newElem)
        }

        this.isDataPacked = src.isDataPacked
    }

    /**
     * Установить адреса элементов
     * @param elemsAddrs список адресов элементов
     */
    fun setElemsAddrs(elemsAddrs: MutableList<ElementAddress>) {
        //this.elemsAddrs = (if (elemsAddrs != null) elemsAddrs else ArrayList<ElementAddress>()) as MutableList<ElementAddress>
        this.elemsAddrs = elemsAddrs
    }

    /**
     * Установить элементы файла
     * @param elems список элементов файла
     */
    fun setElements(elems: MutableList<V8Element?>?) {
        this.elements = (if (elems != null) elems.filterNotNull() else ArrayList<V8Element>()) as MutableList<V8Element>
    }


    /**
     * Добавить элемент в файл
     * @param element элемент для добавления
     */
    fun addElement(element: V8Element?) {
        if (element != null) {
            elements!!.add(element)
        }
    }

    /**
     * Удалить элемент из файла
     * @param element элемент для удаления
     * @return true, если элемент был удален, иначе false
     */
    fun removeElement(element: V8Element?): Boolean {
        return elements!!.remove(element!!)
    }

    val elementCount: Int
        /**
         * Получить количество элементов в файле
         * @return количество элементов в файле
         */
        get() = if (this.elements != null) elements!!.size else 0

    /**
     * Очистить элементы файла
     */
    fun clearElements() {
        elements!!.clear()
        elemsAddrs.clear()
    }

    companion object {
        /**
         * Проверить, является ли файл файлом 1С V8
         * @param fileData данные файла
         * @return true, если файл является файлом 1С V8, иначе false
         */
        fun isV8File(fileData: ByteArray?): Boolean {
            if (fileData == null || fileData.size < 32) {
                return false
            }


            // В файлах 1С заголовок блока — это строка из трех 8-символьных HEX чисел,
            // разделенных пробелами, заканчивающаяся CRLF. Она может быть не в нуле.
            val scanLimit = min(fileData.size, 2048)
            var i = 0
            while (i + 27 < scanLimit) {
                // Проверим шаблон: 8 hex, space, 8 hex, space, 7fffffff, optional space, CR/LF
                if (isHexBlockHeaderAt(fileData, i)) {
                    return true
                }
                i++
            }
            return false
        }

        private fun isHexBlockHeaderAt(data: ByteArray, offset: Int): Boolean {
            // Формат: hhhhhhhh[ ]hhhhhhhh[ ]7fffffff[ ][\r][\n]
            var pos = offset
            if (!isHex8(data, pos)) return false
            pos += 8
            if (!isSpace(data, pos)) return false
            pos += 1
            if (!isHex8(data, pos)) return false
            pos += 8
            if (!isSpace(data, pos)) return false
            pos += 1
            // 7fffffff
            if (pos + 8 > data.size) return false
            for (k in 0..7) {
                val c = data[pos + k]
                val ch = Char(c.toUShort())
                val exp = "7fffffff".get(k)
                if (ch.lowercaseChar() != exp) return false
            }
            pos += 8
            // optional space
            if (pos < data.size && isSpace(data, pos)) pos += 1
            // CR LF
            if (pos + 2 > data.size) return false
            return data[pos] == BlockHeader.EOL_0D.code.toByte() && data[pos + 1] == BlockHeader.EOL_0A.code.toByte()
        }

        private fun isHex8(data: ByteArray, pos: Int): Boolean {
            if (pos + 8 > data.size) return false
            for (i in 0..7) {
                val c = data[pos + i]
                val ch = Char(c.toUShort()).lowercaseChar()
                if (!((ch >= '0' && ch <= '9') || (ch >= 'a' && ch <= 'f'))) return false
            }
            return true
        }

        private fun isSpace(data: ByteArray, pos: Int): Boolean {
            return pos < data.size && data[pos] == BlockHeader.SPACE.code.toByte()
        }
    }
}