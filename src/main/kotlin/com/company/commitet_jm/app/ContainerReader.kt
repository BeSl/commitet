import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.Inflater
import kotlin.math.min
import java.io.File as IOFile

class ContainerReader(private val file: RandomAccessFile) {
    companion object {
        const val END_MARKER = 2147483647

        private fun parseHex(bytes: ByteArray): Int {
            val hexStr = String(bytes, Charsets.US_ASCII).trim()
            return hexStr.toInt(16)
        }

        private fun ByteArray.split(separator: ByteArray): List<ByteArray> {
            val result = mutableListOf<ByteArray>()
            var start = 0
            var index = 0
            while (index <= size - separator.size) {
                if ((index..<index + separator.size).all { this[it] == separator[it - index] }) {
                    result.add(copyOfRange(start, index))
                    start = index + separator.size
                    index = start
                } else {
                    index++
                }
            }
            result.add(copyOfRange(start, size))
            return result
        }
    }

    data class Header(val firstEmptyBlockOffset: Int?, val defaultBlockSize: Int)
    data class Block(val docSize: Int, val currentBlockSize: Int, val nextBlockOffset: Int, val data: ByteArray)
    data class Document(val size: Int, val data: Sequence<ByteArray>)
    data class FileInfo(val name: String, val size: Int, val created: Long, val modified: Long, val data: Document)

    val entries: LinkedHashMap<String, FileInfo>
    private val defaultBlockSize: Int

    init {
        val header = readHeader()
        require(header.defaultBlockSize != 0) { "Container is empty" }
        this.defaultBlockSize = header.defaultBlockSize
        entries = readEntries()
    }

    private fun readHeader(): Header {
        file.seek(0)
        val buffer = ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN)
        file.readFully(buffer.array())
        return Header(
            buffer.int.takeIf { it != END_MARKER },
            buffer.int
        )
    }

    private fun readBlock(offset: Long, maxDataLength: Int? = null): Block {
        file.seek(offset)
        val headerBytes = ByteArray(32)
        file.readFully(headerBytes)

        val docSize = parseHex(headerBytes.copyOfRange(2, 10))
        val currentBlockSize = parseHex(headerBytes.copyOfRange(11, 19))
        val nextBlockOffset = parseHex(headerBytes.copyOfRange(20, 28))

        val dataLength = maxDataLength?.let { min(currentBlockSize, it) } ?: currentBlockSize
        val data = ByteArray(dataLength)
        file.readFully(data)

        return Block(docSize, currentBlockSize, nextBlockOffset, data)
    }

    private fun readDocument(offset: Long): Document {
        val headerBlock = readBlock(offset)
        val dataSequence = sequence {
            yield(headerBlock.data)
            var leftBytes = headerBlock.docSize - headerBlock.data.size
            var nextBlockOffset = headerBlock.nextBlockOffset

            while (leftBytes > 0 && nextBlockOffset != END_MARKER) {
                val block = readBlock(nextBlockOffset.toLong(), leftBytes)
                yield(block.data)
                leftBytes -= block.data.size
                nextBlockOffset = block.nextBlockOffset
            }
        }

        return Document(headerBlock.docSize, dataSequence)
    }

    private fun readEntries(): LinkedHashMap<String, FileInfo> {
        val tocOffset = 16L
        val doc = readDocument(tocOffset)
        val files = LinkedHashMap<String, FileInfo>()

        val separator = byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0x7F)
        val chunks = doc.data.flatMap { it.asIterable() }.toList().toByteArray()
            .split(separator).dropLast(1)

        for (chunk in chunks) {
            val buffer = ByteBuffer.wrap(chunk).order(ByteOrder.LITTLE_ENDIAN)
            val fileDescOffset = buffer.int
            val fileDataOffset = buffer.int

            val fileDescDoc = readDocument(fileDescOffset.toLong())
            val fileData = readDocument(fileDataOffset.toLong())

            val descBuffer = ByteBuffer.wrap(
                fileDescDoc.data.flatMap { it.asIterable() }.toList().toByteArray()
            ).order(ByteOrder.LITTLE_ENDIAN)

            val created = descBuffer.long
            val modified = descBuffer.long
            val nameLen = descBuffer.int
            val nameBytes = ByteArray(descBuffer.remaining())
            descBuffer.get(nameBytes)

            val name = String(nameBytes, Charsets.UTF_16LE).substringBefore('\u0000')
            files[name] = FileInfo(name, fileData.size, created, modified, fileData)
        }

        return files
    }

    fun extract(
        outputPath: String,
        deflate: Boolean = false,
        recursive: Boolean = false
    ) {
        val outputDir = IOFile(outputPath).apply {
            if (exists()) deleteRecursively()
            mkdirs()
        }

        for ((name, fileInfo) in entries) {
            val targetFile = IOFile(outputDir, name).apply {
                parentFile?.mkdirs()
            }

            targetFile.outputStream().use { os ->
                if (deflate) {
                    val inflater =
                        {
                        for (chunk in fileInfo.data.data) {
                            var inflater = Inflater(true)
                            inflater.setInput(chunk)
                            val buffer = ByteArray(1024)
                            while (!inflater.finished()) {
                                val count = inflater.inflate(buffer)
                                if (count > 0) os.write(buffer, 0, count)
                            }
                        }
                    }
                } else {
                    fileInfo.data.data.forEach { os.write(it) }
                }
            }

            if (recursive) {
                val isContainerFile = targetFile.inputStream().use {
                    it.readNBytes(4).contentEquals(
                        byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0x7F)
                    )
                }

                if (isContainerFile) {
                    val tempFile = IOFile(targetFile.parentFile, "${targetFile.name}.tmp").apply {
                        deleteOnExit()
                    }
                    targetFile.renameTo(tempFile)

                    RandomAccessFile(tempFile, "r").use { raf ->
                        ContainerReader(raf).extract(targetFile.absolutePath, true, true)
                    }
                    tempFile.delete()
                }
            }
        }
    }
}
