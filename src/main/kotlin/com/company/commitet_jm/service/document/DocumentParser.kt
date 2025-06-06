package com.company.commitet_jm.service.document

import java.io.IOException
import java.io.InputStream
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.ai.document.Document


class DocumentParser {

    val tika: Tika = Tika()

    @Throws(TikaException::class, IOException::class)
    fun extractText(inputStream: InputStream?): String {
        return tika.parseToString(inputStream)
    }


    fun chunkText(text: String, chunkSize: Int, overlap: Int): List<String> {
        val chunks = mutableListOf<String>()
        var start = 0
        while (start < text.length) {
            val end = minOf(start + chunkSize, text.length)
            chunks.add(text.substring(start, end))
            start = end - overlap
        }
        return chunks
    }

    fun lazyChunkDocuments(text: String, chunkSize: Int, overlap: Int, filename: String): List<Document> {
        val documents = mutableListOf<Document>()
        var start = 0
        while (start < text.length) {
            val end = minOf(start + chunkSize, text.length)
            val chunk = text.substring(start, end)
            documents.add(Document(chunk, mapOf("filename" to filename)))
            start = end - overlap
            if (start < 0) start = 0
        }
        return documents
    }

}
