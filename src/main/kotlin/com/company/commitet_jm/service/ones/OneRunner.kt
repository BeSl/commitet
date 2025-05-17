package com.company.commitet_jm.service.ones

import com.company.commitet_jm.component.ShellExecutor
import com.company.commitet_jm.entity.AppSettings
import com.company.commitet_jm.service.GitWorker
import io.jmix.core.DataManager
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

import java.io.File

class OneRunner(private val dataManager: DataManager
) {

    companion object {
        private  val log = LoggerFactory.getLogger(GitWorker::class.java)
    }
    @Autowired
    private lateinit var shellExecutor: ShellExecutor

     var v8unpackPath : String = ""

    fun uploadExtFiles(inputFile: File, outDir: String,pathInstall: String, version: String ) {

        val res = shellExecutor.executeCommand(listOf(
            pathPlatform(pathInstall, version),
            "DESIGNER",
            "/DumpExternalDataProcessorOrReportToFiles",
            "\"$outDir\"",
            "\"${inputFile.path}\""
        ))
        log.debug("Строка запуска $res")
    }

    fun unpackExtFiles(inputFile: File, outDir: String){

        if (v8unpackPath.isEmpty()){
            val unpackPath = dataManager.load(AppSettings::class.java)
                .query("select apps from AppSettings apps where apps.name = :pName")
                .parameter("pName", "v8unpack")
                .optional().get()


            v8unpackPath = unpackPath.value.toString()
        }

        val res = shellExecutor.executeCommand(listOf(
            v8unpackPath,
            "-U",
            inputFile.path,
            outDir

        ))

        log.info("unpack rename files")

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

    fun pathPlatform(basePath: String?, version: String?):String{
        return "$basePath\\$version\\bin\\1cv8.exe"
    }
    /**
     * Оставляет в директории только указанные файлы, переименовывает их и удаляет остальные
     * @param directory Целевая директория
     * @param keepFiles Список имён файлов для сохранения (регистрозависимый)
     * @param renameRule Функция для генерации нового имени файла на основе старого
     */
    private fun filterAndRenameFiles(
        directory: File,
        keepFiles: Set<String>,
        renameRule: (String) -> String
    ) {
        if (!directory.isDirectory) return

        directory.listFiles()?.forEach { file ->
            when {
                file.isFile && file.name in keepFiles -> {
                    val newName = renameRule(file.name)
                    val newFile = File(directory, newName)
                    if (newName != file.name) {
                        if (newFile.exists()) newFile.delete()
                        file.renameTo(newFile)
                    }
                }
                else -> {
                    if (!file.isDirectory) {
                        file.delete()
                    }
                }
            }
        }
    }

}