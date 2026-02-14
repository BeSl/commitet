package com.company.commitet_jm.service.unpack.model

/**
 * Представление заголовка файла 1С:Предприятия
 *
 * Этот класс содержит информацию о заголовке файла 1С, включая версию формата,
 * количество элементов и другую метаинформацию.
 */
class FileHeader {
    /**
     * Получить следующий адрес страницы
     * @return следующий адрес страницы
     */
    /**
     * Установить следующий адрес страницы
     * @param nextPageAddr следующий адрес страницы
     */
    // Следующий адрес страницы
    var nextPageAddr: Int

    /**
     * Получить размер страницы
     * @return размер страницы
     */
    /**
     * Установить размер страницы
     * @param pageSize размер страницы
     */
    // Размер страницы
    var pageSize: Int

    /**
     * Получить версию хранилища
     * @return версия хранилища
     */
    /**
     * Установить версию хранилища
     * @param storageVersion версия хранилища
     */
    // Версия хранилища
    var storageVersion: Int

    /**
     * Получить зарезервированное значение
     * @return зарезервированное значение
     */
    /**
     * Установить зарезервированное значение
     * @param reserved зарезервированное значение
     */
    // Зарезервировано (всегда 0x00000000)
    var reserved: Int

    /**
     * Конструктор по умолчанию
     */
    constructor() {
        this.nextPageAddr = FF_SIGNATURE
        this.pageSize = DEFAULT_PAGE_SIZE
        this.storageVersion = 0
        this.reserved = 0
    }

    /**
     * Конструктор с параметрами
     * @param nextPageAddr следующий адрес страницы
     * @param pageSize размер страницы
     * @param storageVersion версия хранилища
     * @param reserved зарезервированное значение
     */
    constructor(nextPageAddr: Int, pageSize: Int, storageVersion: Int, reserved: Int) {
        this.nextPageAddr = nextPageAddr
        this.pageSize = pageSize
        this.storageVersion = storageVersion
        this.reserved = reserved
    }

    companion object {
        // Размер страницы по умолчанию
        const val DEFAULT_PAGE_SIZE: Int = 512

        // Сигнатура 0x7fffffff
        const val FF_SIGNATURE: Int = 0x7fffffff

        val size: Int
            /**
             * Получить размер заголовка файла
             * @return размер заголовка файла в байтах
             */
            get() = 4 + 4 + 4 + 4 // 16 байт
    }
}