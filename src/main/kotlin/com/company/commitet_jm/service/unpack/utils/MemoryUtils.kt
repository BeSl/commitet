package com.company.commitet_jm.service.unpack.utils

import java.nio.ByteBuffer
import java.nio.ByteOrder


/**
 * Утилиты для работы с памятью
 *
 * Этот класс содержит вспомогательные методы для работы с памятью,
 * такие как преобразование между различными типами данных и байтовыми массивами.
 */
object MemoryUtils {
    /**
     * Преобразовать int в массив байтов (little-endian)
     * @param value значение для преобразования
     * @return массив байтов
     */
    fun intToByteArray(value: Int): ByteArray {
        return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array()
    }

    /**
     * Преобразовать массив байтов в int (little-endian)
     * @param bytes массив байтов для преобразования
     * @return значение int
     */
    fun byteArrayToInt(bytes: ByteArray?): Int {
        if (bytes == null || bytes.size < 4) {
            return 0
        }
        return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getInt()
    }

    /**
     * Преобразовать массив байтов в int (little-endian) с указанным смещением
     * @param bytes массив байтов для преобразования
     * @param offset смещение в массиве байтов
     * @return значение int
     */
    fun byteArrayToInt(bytes: ByteArray?, offset: Int): Int {
        if (bytes == null || bytes.size < offset + 4) {
            return 0
        }
        return ByteBuffer.wrap(bytes, offset, 4).order(ByteOrder.LITTLE_ENDIAN).getInt()
    }

    /**
     * Преобразовать long в массив байтов (little-endian)
     * @param value значение для преобразования
     * @return массив байтов
     */
    fun longToByteArray(value: Long): ByteArray {
        return ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(value).array()
    }

    /**
     * Преобразовать массив байтов в long (little-endian)
     * @param bytes массив байтов для преобразования
     * @return значение long
     */
    fun byteArrayToLong(bytes: ByteArray?): Long {
        if (bytes == null || bytes.size < 8) {
            return 0
        }
        return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getLong()
    }

    /**
     * Преобразовать массив байтов в long (little-endian) с указанным смещением
     * @param bytes массив байтов для преобразования
     * @param offset смещение в массиве байтов
     * @return значение long
     */
    fun byteArrayToLong(bytes: ByteArray?, offset: Int): Long {
        if (bytes == null || bytes.size < offset + 8) {
            return 0
        }
        return ByteBuffer.wrap(bytes, offset, 8).order(ByteOrder.LITTLE_ENDIAN).getLong()
    }

    /**
     * Преобразовать шестнадцатеричную строку в int
     * @param hex шестнадцатеричная строка
     * @return значение int
     */
    fun hexToInt(hex: String?): Int {
        var hex = hex
        if (hex == null || hex.isEmpty()) {
            return 0
        }


        // Удаляем пробелы в начале и конце
        hex = hex.trim { it <= ' ' }


        // Находим конец значащей части строки
        var end = 0
        for (i in 0..<hex.length) {
            val c = hex.get(i).lowercaseChar()
            if ((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f')) {
                end = i + 1
            } else if (c == ' ') {
                break
            } else {
                break
            }
        }

        if (end == 0) {
            return 0
        }

        try {
            return hex.substring(0, end).toInt(16)
        } catch (e: NumberFormatException) {
            return 0
        }
    }

    /**
     * Преобразовать int в шестнадцатеричную строку
     * @param value значение для преобразования
     * @return шестнадцатеричная строка
     */
    fun intToHex(value: Int): String {
        return String.format("%08x", value)
    }

    /**
     * Объединить два массива байтов
     * @param array1 первый массив байтов
     * @param array2 второй массив байтов
     * @return объединенный массив байтов
     */
    fun concatArrays(array1: ByteArray?, array2: ByteArray?): ByteArray? {
        if (array1 == null) {
            return if (array2 != null) array2.clone() else ByteArray(0)
        }
        if (array2 == null) {
            return array1.clone()
        }

        val result = ByteArray(array1.size + array2.size)
        System.arraycopy(array1, 0, result, 0, array1.size)
        System.arraycopy(array2, 0, result, array1.size, array2.size)
        return result
    }
}