package com.company.commitet_jm.service.unpack.utils


import com.company.commitet_jm.service.unpack.model.BlockHeader
import com.company.commitet_jm.service.unpack.model.FileHeader
import kotlin.math.max
import kotlin.math.min

object V8BlockReader {
    fun readBlockData(fileBytes: ByteArray, blockHeaderOffset: Int): ByteArray {
        if (blockHeaderOffset < 0 || blockHeaderOffset + BlockHeader.Size > fileBytes.size) return ByteArray(0)

        val dataSize = httoi(fileBytes, blockHeaderOffset + 2) // after EOL 0D,0A
        val out = ByteArray(max(0, dataSize))
        if (dataSize <= 0) return out

        var readInBytes = 0
        var headerOffset = blockHeaderOffset
        while (readInBytes < dataSize && headerOffset >= 0 && headerOffset + BlockHeader.Size <= fileBytes.size) {
            // quick header sanity
            if (!isValidHeader(fileBytes, headerOffset)) break

            val pageSize = httoi(fileBytes, headerOffset + 2 + 8 + 1) // after data_size_hex and space1
            val nextPageAddr = httoi(fileBytes, headerOffset + 2 + 8 + 1 + 8 + 1) // after page_size_hex and space2

            val bytesToRead = min(pageSize, dataSize - readInBytes)
            val dataStart: Int = headerOffset + BlockHeader.Size
            val dataEnd = min(fileBytes.size, dataStart + bytesToRead)
            if (dataStart < 0 || dataStart > fileBytes.size || dataStart > dataEnd) break
            if (dataEnd > dataStart) {
                System.arraycopy(fileBytes, dataStart, out, readInBytes, dataEnd - dataStart)
                readInBytes += (dataEnd - dataStart)
            }

            if (nextPageAddr != FileHeader.FF_SIGNATURE) {
                headerOffset = nextPageAddr
            } else {
                break
            }
        }
        return out
    }

    private fun isValidHeader(data: ByteArray, off: Int): Boolean {
        if (off + BlockHeader.Size > data.size) return false
        return data[off] == BlockHeader.EOL_0D.code.toByte() && data[off + 1] == BlockHeader.EOL_0A.code.toByte() && data[off + 2 + 8] == BlockHeader.SPACE.code.toByte() && data[off + 2 + 8 + 1 + 8] == BlockHeader.SPACE.code.toByte() && data[off + 2 + 8 + 1 + 8 + 1 + 8] == BlockHeader.SPACE.code.toByte() && data[off + BlockHeader.Size - 2] == BlockHeader.EOL2_0D.code.toByte() && data[off + BlockHeader.Size - 1] == BlockHeader.EOL2_0A.code.toByte()
    }

    private fun httoi(data: ByteArray, off: Int): Int {
        var `val` = 0
        for (i in 0..7) {
            val idx = off + i
            if (idx >= data.size) break
            val c = Char(data[idx].toUShort()).lowercaseChar().code
            val d: Int
            if (c >= '0'.code && c <= '9'.code) d = c - '0'.code
            else if (c >= 'a'.code && c <= 'f'.code) d = 10 + (c - 'a'.code)
            else break
            `val` = (`val` shl 4) or d
        }
        return `val`
    }
}

