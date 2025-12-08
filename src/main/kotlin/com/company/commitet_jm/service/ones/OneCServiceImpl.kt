package com.company.commitet_jm.service.ones

import com.company.commitet_jm.component.ShellExecutor
import io.jmix.core.DataManager
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

import java.io.File

@Service
class OneCServiceImpl(
    private val dataManager: DataManager,
    private val shellExecutor: ShellExecutor
) : OneCService {

    companion object {
        private val log = LoggerFactory.getLogger(OneCServiceImpl::class.java)
    }

    @Value("\${app.v8unpack}")
    private var v8unpackPath: String = ""

    @Value("\${app.onelogpath}")
    private var oneLogPath: String = ""

    @PostConstruct
    fun init() {
        log.info("Initialized OneCServiceImpl with v8unpackPath: $v8unpackPath")
        log.info("Initialized OneCServiceImpl with oneLogPath: $oneLogPath")
    }

    override fun uploadExtFiles(inputFile: File, outDir: String, pathInstall: String, version: String) {
        val res = shellExecutor.executeCommand(listOf(
            pathPlatform(pathInstall, version),
            "DESIGNER",
            "/DumpExternalDataProcessorOrReportToFiles",
            "\"$outDir\"",
            "\"${inputFile.path}\""
        ))
        log.debug("Строка запуска $res")
    }

    override fun unpackExtFiles(
        inputFile: File,
        outDir: String,
        pathInstall: String,
        version: String
    ) {
        TODO("Not yet implemented")
    }

    fun unpackExtFiles(inputFile: File, outDir: String) {
        if (v8unpackPath.isEmpty()) {
            throw RuntimeException("Path b8Unpack is Empty!!!")
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
                when (originalName) {
                    "form.data" -> "form"
                    "module.data" -> "Module.bsl"
                    else -> {
                        originalName
                    }
                }
            }
        )
        log.debug("Unpack command $res")
    }

    override fun pathPlatform(basePath: String?, version: String?): String {
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