package com.company.containerextractor.service



import ContainerReader

import org.springframework.stereotype.Service
import java.io.File
import java.io.RandomAccessFile

@Service
class ContainerExtractService {

    fun extractContainer(inputFilePath: String, outputFolderPath: String): List<String> {
        val outputDir = File(outputFolderPath)
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }

        val extractedFileNames = mutableListOf<String>()
        val file = File(inputFilePath)
//        val containerReader = ContainerReader(file.absolutePath)
        try {
            val containerPath = "F:\\test.epf"
            val outputFolder = "F:\\testkontainer"

            // Открываем файл контейнера
            RandomAccessFile(containerPath, "r").use { raf ->
                // Создаем экземпляр контейнера
                val container = ContainerReader(raf)

                // Распаковываем с дефляцией и рекурсивной обработкой
                container.extract(
                    outputPath = outputFolder,
                    deflate = true,
                    recursive = true
                )

                // Выводим список файлов
                println("Extracted files:")
                container.entries.keys.forEach { println(" - $it") }
            }

        }catch (e: Exception){
            println(e.message)
        }


        outputDir.walkTopDown()
            .filter { it.isFile }
            .forEach { extractedFileNames.add(it.relativeTo(outputDir).path) }

        return extractedFileNames
    }
}
