package com.company.commitet_jm.service.ones

import com.company.commitet_jm.component.ShellExecutor
import com.company.commitet_jm.entity.AppSettings
import com.company.commitet_jm.service.unpack.UnpackService
import io.jmix.core.DataManager
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.text.SimpleDateFormat
import java.util.Date

import java.io.File

@Service
class OneRunner(private val dataManager: DataManager
) {

    companion object {
        private  val log = LoggerFactory.getLogger(OneRunner::class.java)
    }
    @Autowired
    private lateinit var shellExecutor: ShellExecutor

    var v8unpackPath : String = ""

    fun uploadExtFiles(inputFile: File, outDir: String,pathInstall: String, version: String ) {
        log.debug("2 Подготовка выгрузка файлов ")
        // Имя файла без расширения
        val fileNameWithoutExt = inputFile.nameWithoutExtension

        // Итоговый каталог: outDir/<имя_файла_без_расширения>
        val targetDir = File(outDir, fileNameWithoutExt)

        // Создаём каталог, если его нет
        if (!targetDir.exists()) {
            targetDir.mkdirs()
        }
        // Формат времени для имени лог-файла
        val timeStamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(Date())
        val logFileName = "OneLog${File.separator}out_$timeStamp.log"

        val res = shellExecutor.executeCommand(listOf(
            pathPlatform(pathInstall, version),
            "DESIGNER",
            "/DisableStartupDialogs",
            "/DumpExternalDataProcessorOrReportToFiles",
            "${targetDir.path}${File.separator}",
            "${inputFile.path}${File.separator}",
            "/Out",
            logFileName
        ))
        log.debug("2 Строка запуска $res")
    }

    fun unpackExtFiles(inputFile: File, outDir: String){
        var unpackEnabled = false
        if (unpackEnabled) {
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
        }else{
            val unp = UnpackService()
            unp.unpackToDirectory(inputFile.path, outDir)
        }


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
        log.debug("hand made unpack")

    }

    fun pathPlatform(basePath: String?, version: String?):String{
        val osName = System.getProperty("os.name").lowercase()
        return if (osName.contains("windows")) {
            "$basePath\\$version\\bin\\1cv8.exe"
        } else {
            "$basePath/$version/1cv8s"
        }
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