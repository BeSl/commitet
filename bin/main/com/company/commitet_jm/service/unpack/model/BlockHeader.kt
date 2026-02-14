package com.company.commitet_jm.service.unpack.model

/**
 * Представление заголовка блока данных файла 1С:Предприятия
 *
 * Этот класс содержит информацию о заголовке блока данных в файле 1С,
 * включая информацию о сжатии и размере данных.
 */
class BlockHeader {
    /**
     * Получить символ окончания строки 0x0D
     * @return символ окончания строки 0x0D
     */
    /**
     * Установить символ окончания строки 0x0D
     * @param eol0D символ окончания строки 0x0D
     */
    // Символ окончания строки 0x0D
    var eol0D: Char

    /**
     * Получить символ окончания строки 0x0A
     * @return символ окончания строки 0x0A
     */
    /**
     * Установить символ окончания строки 0x0A
     * @param eol0A символ окончания строки 0x0A
     */
    // Символ окончания строки 0x0A
    var eol0A: Char

    /**
     * Получить размер данных в шестнадцатеричном формате
     * @return размер данных в шестнадцатеричном формате
     */
    /**
     * Установить размер данных в шестнадцатеричном формате
     * @param dataSizeHex размер данных в шестнадцатеричном формате
     */
    // Размер данных в шестнадцатеричном формате (8 байт)
    var dataSizeHex: String

    /**
     * Получить пробел 1
     * @return пробел 1
     */
    /**
     * Установить пробел 1
     * @param space1 пробел 1
     */
    // Пробел
    var space1: Char

    /**
     * Получить размер страницы в шестнадцатеричном формате
     * @return размер страницы в шестнадцатеричном формате
     */
    /**
     * Установить размер страницы в шестнадцатеричном формате
     * @param pageSizeHex размер страницы в шестнадцатеричном формате
     */
    // Размер страницы в шестнадцатеричном формате (8 байт)
    var pageSizeHex: String

    /**
     * Получить пробел 2
     * @return пробел 2
     */
    /**
     * Установить пробел 2
     * @param space2 пробел 2
     */
    // Пробел
    var space2: Char

    /**
     * Получить адрес следующей страницы в шестнадцатеричном формате
     * @return адрес следующей страницы в шестнадцатеричном формате
     */
    /**
     * Установить адрес следующей страницы в шестнадцатеричном формате
     * @param nextPageAddrHex адрес следующей страницы в шестнадцатеричном формате
     */
    // Адрес следующей страницы в шестнадцатеричном формате (8 байт)
    var nextPageAddrHex: String

    /**
     * Получить пробел 3
     * @return пробел 3
     */
    /**
     * Установить пробел 3
     * @param space3 пробел 3
     */
    // Пробел
    var space3: Char

    /**
     * Получить символ окончания строки 2 0x0D
     * @return символ окончания строки 2 0x0D
     */
    /**
     * Установить символ окончания строки 2 0x0D
     * @param eol20D символ окончания строки 2 0x0D
     */
    // Символ окончания строки 2 0x0D
    var eol20D: Char

    /**
     * Получить символ окончания строки 2 0x0A
     * @return символ окончания строки 2 0x0A
     */
    /**
     * Установить символ окончания строки 2 0x0A
     * @param eol20A символ окончания строки 2 0x0A
     */
    // Символ окончания строки 2 0x0A
    var eol20A: Char

    /**
     * Конструктор по умолчанию
     */
    constructor() {
        this.eol0D = EOL_0D
        this.eol0A = EOL_0A
        this.dataSizeHex = "00000000"
        this.space1 = SPACE
        this.pageSizeHex = "00000200" // 512 в шестнадцатеричном
        this.space2 = SPACE
        this.nextPageAddrHex = "7fffffff"
        this.space3 = SPACE
        this.eol20D = EOL2_0D
        this.eol20A = EOL2_0A
    }

    /**
     * Конструктор с параметрами
     * @param dataSize размер данных
     * @param pageSize размер страницы
     * @param nextPageAddr адрес следующей страницы
     */
    constructor(dataSize: Int, pageSize: Int, nextPageAddr: Int) {
        this.eol0D = EOL_0D
        this.eol0A = EOL_0A
        this.dataSizeHex = String.format("%08x", dataSize)
        this.space1 = SPACE
        this.pageSizeHex = String.format("%08x", pageSize)
        this.space2 = SPACE
        this.nextPageAddrHex = String.format("%08x", nextPageAddr)
        this.space3 = SPACE
        this.eol20D = EOL2_0D
        this.eol20A = EOL2_0A
    }

    val isValid: Boolean
        /**
         * Проверить корректность заголовка блока
         * @return true, если заголовок блока корректен, иначе false
         */
        get() = eol0D == EOL_0D && eol0A == EOL_0A && space1 == SPACE && space2 == SPACE && space3 == SPACE && eol20D == EOL2_0D && eol20A == EOL2_0A

    var dataSize: Int
        /**
         * Получить размер данных
         * @return размер данных
         */
        get() {
            try {
                return dataSizeHex.toInt(16)
            } catch (e: NumberFormatException) {
                return 0
            }
        }
        /**
         * Установить размер данных
         * @param dataSize размер данных
         */
        set(dataSize) {
            this.dataSizeHex = String.format("%08x", dataSize)
        }

    var pageSize: Int
        /**
         * Получить размер страницы
         * @return размер страницы
         */
        get() {
            try {
                return pageSizeHex.toInt(16)
            } catch (e: NumberFormatException) {
                return FileHeader.DEFAULT_PAGE_SIZE
            }
        }
        /**
         * Установить размер страницы
         * @param pageSize размер страницы
         */
        set(pageSize) {
            this.pageSizeHex = String.format("%08x", pageSize)
        }

    var nextPageAddr: Int
        /**
         * Получить адрес следующей страницы
         * @return адрес следующей страницы
         */
        get() {
            try {
                return nextPageAddrHex.toInt(16)
            } catch (e: NumberFormatException) {
                return FileHeader.FF_SIGNATURE
            }
        }
        /**
         * Установить адрес следующей страницы
         * @param nextPageAddr адрес следующей страницы
         */
        set(nextPageAddr) {
            this.nextPageAddrHex = String.format("%08x", nextPageAddr)
        }

    companion object {
        // Символы окончания строки
        val EOL_0D: Char = 0x0d.toChar()
        val EOL_0A: Char = 0x0a.toChar()
        val EOL2_0D: Char = 0x0d.toChar()
        val EOL2_0A: Char = 0x0a.toChar()

        // Пробелы
        val SPACE: Char = 0x20.toChar()

        val Size: Int
            /**
             * Получить размер заголовка блока
             * @return размер заголовка блока в байтах
             */
            get() = 1 + 1 + 8 + 1 + 8 + 1 + 8 + 1 + 1 + 1 // 30 байт
    }
}