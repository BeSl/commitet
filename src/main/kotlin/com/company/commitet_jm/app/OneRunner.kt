package com.company.commitet_jm.app

import com.company.commitet_jm.entity.AppSettings
import com.company.commitet_jm.service.GitWorker
import io.jmix.core.DataManager
import org.slf4j.LoggerFactory

import java.io.File


class OneRunner(private val dataManager: DataManager,private val pathInstall: String, private val version:String) {

    companion object {
        private  val log = LoggerFactory.getLogger(GitWorker::class.java)
    }

    fun UploadExtFiles(inputFile: File, outDir: String){
        val executor = ShellExecutor()
        val res = executor.executeCommand(listOf(
            "$pathInstall\\$version\\bin\\1cv8.exe",
            "DESIGNER",
            "/DumpExternalDataProcessorOrReportToFiles",
            "\"$outDir\"",
            "\"${inputFile.path}\""
        ))
        log.debug("Строка запуска $res")
    }

    fun unpackExtFiles(inputFile: File, outDir: String){
        val executor = ShellExecutor()

        val unpackPath = dataManager.load(AppSettings::class.java)
            .query("select apps from AppSettings apps where apps.name = :pName")
            .parameter("pName", "v8unpack")
            .optional().get()

        val res = executor.executeCommand(listOf(
            unpackPath.value,
            "-U",
            inputFile.path,
            outDir

        ))

        log.info("start rename files")

        filterAndRenameFiles(
            directory = File(outDir),
            keepFiles = setOf("form.data", "module.data", "Form.bin"),
            renameRule = { originalName ->
                when (originalName){
                    "form.data" -> "form"
                    "module.data" -> "Module.bsl"
                    else -> {originalName}
                }

            }
        )
        log.debug("Unpack command $res")
    }
    /**
     * Оставляет в директории только указанные файлы, переименовывает их и удаляет остальные
     * @param directory Целевая директория
     * @param keepFiles Список имён файлов для сохранения (регистрозависимый)
     * @param renameRule Функция для генерации нового имени файла на основе старого
     */
    fun filterAndRenameFiles(
        directory: File,
        keepFiles: Set<String>,
        renameRule: (String) -> String
    ) {
        if (!directory.isDirectory) return

        directory.listFiles()?.forEach { file ->
            when {
                // Для файлов из списка сохранения
                file.isFile && file.name in keepFiles -> {
                    val newName = renameRule(file.name)
                    val newFile = File(directory, newName)

                    // Убедимся, что новое имя не совпадает с текущим
                    if (newName != file.name) {
                        // Удаляем существующий файл с новым именем (если есть)
                        if (newFile.exists()) newFile.delete()
                        file.renameTo(newFile)
                    }
                }

                // Для всех остальных файлов и директорий
                else -> {
                    if (file.isDirectory) {
                        // Для директорий: удалить рекурсивно (раскомментировать при необходимости)
                        // file.deleteRecursively()
                    } else {
                        file.delete()
                    }
                }
            }
        }
    }


}