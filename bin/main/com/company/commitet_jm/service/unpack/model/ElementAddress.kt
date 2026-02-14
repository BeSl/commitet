package com.company.commitet_jm.service.unpack.model

/**
 * Представление адреса элемента файла 1С:Предприятия
 *
 * Этот класс содержит информацию о адресе элемента в файле 1С, включая
 * смещение в файле и размер данных.
 */
class ElementAddress {
    /**
     * Получить адрес заголовка элемента
     * @return адрес заголовка элемента
     */
    /**
     * Установить адрес заголовка элемента
     * @param elemHeaderAddr адрес заголовка элемента
     */
    // Адрес заголовка элемента
    var elemHeaderAddr: Int

    /**
     * Получить адрес данных элемента
     * @return адрес данных элемента
     */
    /**
     * Установить адрес данных элемента
     * @param elemDataAddr адрес данных элемента
     */
    // Адрес данных элемента
    var elemDataAddr: Int

    /**
     * Получить сигнатуру
     * @return сигнатура (всегда 0x7fffffff)
     */
    /**
     * Установить сигнатуру
     * @param ffSignature сигнатура (всегда 0x7fffffff)
     */
    // Сигнатура 0x7fffffff
    var ffSignature: Int

    /**
     * Конструктор по умолчанию
     */
    constructor() {
        this.elemHeaderAddr = 0
        this.elemDataAddr = 0
        this.ffSignature = FileHeader.FF_SIGNATURE
    }

    /**
     * Конструктор с параметрами
     * @param elemHeaderAddr адрес заголовка элемента
     * @param elemDataAddr адрес данных элемента
     * @param ffSignature сигнатура (всегда 0x7fffffff)
     */
    constructor(elemHeaderAddr: Int, elemDataAddr: Int, ffSignature: Int) {
        this.elemHeaderAddr = elemHeaderAddr
        this.elemDataAddr = elemDataAddr
        this.ffSignature = ffSignature
    }

    companion object {
        val size: Int
            /**
             * Получить размер структуры адреса элемента
             * @return размер структуры адреса элемента в байтах
             */
            get() = 4 + 4 + 4 // 12 байт
    }
}