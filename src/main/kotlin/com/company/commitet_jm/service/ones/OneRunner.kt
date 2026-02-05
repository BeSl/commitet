package com.company.commitet_jm.service.ones

import com.company.commitet_jm.component.ShellExecutor
import com.company.commitet_jm.service.unpack.UnpackService
import io.jmix.core.DataManager
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.text.SimpleDateFormat
import java.util.Date

import java.io.File

@Service
class OneRunner(
    private val dataManager: DataManager,
    private val fileUtils: OneCFileUtils
) {

    companion object {
        private val log = LoggerFactory.getLogger(OneRunner::class.java)
    }
    @Autowired
    private lateinit var shellExecutor: ShellExecutor

    @Value("\${app.onelogpath}")
    val oneCLogPath: String = "../test"

    @Value("\${app.v8unpack}")
    var v8unpackPath : String = ""
    
    /**
     * Получает абсолютный путь к v8unpack.exe относительно каталога запуска приложения
     */
    private fun getAbsoluteV8unpackPath(): String {
        // Если путь уже абсолютный, возвращаем его как есть
        val file = File(v8unpackPath)
        if (file.isAbsolute) {
            return v8unpackPath
        }
        
        // Если путь относительный, строим абсолютный путь от каталога запуска приложения
        val currentDir = File("").absoluteFile
        val absolutePath = File(currentDir, v8unpackPath).absolutePath
        return absolutePath
    }

    fun uploadExtFiles(inputFile: File, outDir: String,pathInstall: String, version: String ) {
        log.debug("2 Подготовка выгрузка файлов ")
        // Имя файла без расширения
        val fileNameWithoutExt = inputFile.nameWithoutExtension

        // Итоговый каталог: outDir/<имя_файла_без_расширения>
        val targetDir = File(outDir)

        // Создаём каталог, если его нет
        if (!targetDir.exists()) {
            targetDir.mkdirs()
        }
        // Формат времени для имени лог-файла
        val timeStamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(Date())
        val logFileName = "$oneCLogPath${File.separator}out_$timeStamp.log"

        val res = shellExecutor.executeCommand(listOf(
            pathPlatform(pathInstall, version),
            "DESIGNER",
            "/DisableStartupDialogs",
            "/DumpExternalDataProcessorOrReportToFiles",
            "\"${targetDir.path}${File.separator}\"",
            "\"${inputFile.path}\"",
            "/Out",
            logFileName
        ))
        log.debug("2 Строка запуска $res")
    }

    fun unpackExtFiles(inputFile: File, outDir: String){
        var unpackEnabled = true
        if (unpackEnabled) {
            if (v8unpackPath.isEmpty()){

               throw RuntimeException("Path b8Unpack is Empty!!!")
            }

            val res = shellExecutor.executeCommand(listOf(
                    getAbsoluteV8unpackPath(),
                    "-U",
                    inputFile.path,
                    outDir
                    ))
        }else{
            val unp = UnpackService()
            unp.unpackToDirectory(inputFile.path, outDir)
        }


        log.info("unpack rename files")

        fileUtils.filterAndRenameFiles(
            directory = File(outDir),
            keepFiles = setOf("form.data", "module.data", "Form.bin"),
            renameRule = { originalName ->
                when (originalName) {
                    "form.data" -> "form"
                    "module.data" -> "Module.bsl"
                    else -> originalName
                }
            }
        )
        log.debug("hand made unpack")

    }

    fun pathPlatform(basePath: String?, version: String?):String{
        val osName = System.getProperty("os.name").lowercase()
        return if (osName.contains("windows")) {
            "\"$basePath\\$version\\bin\\1cv8.exe\""
        } else {
            "$basePath/$version/1cv8s"
        }
    }
}