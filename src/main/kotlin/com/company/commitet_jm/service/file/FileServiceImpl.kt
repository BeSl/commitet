package com.company.commitet_jm.service.file

import com.company.commitet_jm.component.ShellExecutor
import com.company.commitet_jm.entity.FileCommit
import com.company.commitet_jm.entity.Platform
import com.company.commitet_jm.entity.TypesFiles
import com.company.commitet_jm.service.ones.OneRunner
import io.jmix.core.FileStorage
import io.jmix.core.FileStorageLocator
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

@Service
class FileServiceImpl(
    private val fileStorageLocator: FileStorageLocator,
    private val ones: OneRunner
) : FileService {

    companion object {
        private val log = LoggerFactory.getLogger(FileServiceImpl::class.java)
    }

    override fun saveFileCommit(baseDir: String, files: MutableList<FileCommit>, platform: Platform) {
        val executor = ShellExecutor(workingDir = File(baseDir), timeout = 7)
        val filesToUnpack = mutableListOf<Pair<String, String>>()
        for (file in files) {
            val content = file.data ?: continue

            // correctPath возвращает File, приводим к Path
            val path = file.getType()?.let { correctPath(baseDir, it).toPath() } ?: continue
            val targetPath = path.resolve(file.name.toString()).normalize()

            try {
                // Создаем директории, если нужно
                Files.createDirectories(targetPath.parent)
            } catch (e: IOException) {
                throw RuntimeException("Не удалось создать директорию: ${targetPath.parent}", e)
            }

            val fileStorage = fileStorageLocator.getDefault<FileStorage>()
            fileStorage.openStream(content).use { inputStream ->
                Files.copy(
                    inputStream,
                    targetPath,
                    StandardCopyOption.REPLACE_EXISTING
                )
            }
            file.getType()?.let { fileType ->
                val unpackPath = when (fileType) {
                    TypesFiles.REPORT -> "$baseDir\\DataProcessorsExt\\erf"
                    TypesFiles.DATAPROCESSOR -> "$baseDir\\DataProcessorsExt\\epf"
                    else -> null
                }
                unpackPath?.let {
                    filesToUnpack.add(targetPath.toString() to unpackPath)
                }
            }

        }
        if (filesToUnpack.isNotEmpty()) {
            unpackFiles(filesToUnpack, platform, executor, baseDir)
        }
    }

    override fun correctPath(baseDir: String, type: TypesFiles): File {
        return when (type) {
            TypesFiles.REPORT -> File(baseDir, "DataProcessorsExt\\Отчет\\")
            TypesFiles.DATAPROCESSOR -> File(baseDir, "DataProcessorsExt\\Обработка\\")
            TypesFiles.SCHEDULEDJOBS -> File(baseDir, "CodeExt")
            TypesFiles.EXTERNAL_CODE -> File(baseDir, "CodeExt")
            TypesFiles.EXCHANGE_RULES -> File(baseDir, "EXCHANGE_RULES")
        }
    }

    override fun findBinaryFilesFromGitStatus(repoDir: String, executor: ShellExecutor): List<File> {
        // Получаем список изменённых файлов
        val gitOutput = executor.executeCommand(listOf("git", "-C", repoDir, "status", "--porcelain")).trim()
        if (gitOutput.isBlank()) return emptyList()

        // Выделяем директории из вывода git status
        val changedDirs = gitOutput
            .lines()
            .mapNotNull { line ->
                val filePath = line.substringAfter(" ").takeIf { it.isNotBlank() }
                filePath?.let { File(repoDir, it) }
            }
            .filterNotNull()
            .distinct()

        // Ищем .bin файлы в изменённых директориях
        val tDir = changedDirs.flatMap { dir ->
            dir.walk()
                .filter { file ->
                    file.isFile && file.name.endsWith("Form.bin", ignoreCase = false)
                }
                .toList()
        }
        return tDir
    }

    override fun unpackFiles(files: List<Pair<String, String>>, platform: Platform, executor: ShellExecutor, baseDir: String) {
       
        if (files.isEmpty()) {
            return
        }
       
        for ((sourcePath, unpackPath) in files) {
            ones.uploadExtFiles(File(sourcePath), unpackPath, platform.pathInstalled.toString(), platform.version.toString())
        }
       
        val bFiles = findBinaryFilesFromGitStatus(baseDir, executor)
        if (bFiles.isEmpty()) {
            return
        }
        bFiles.forEach { binFile ->
            ones.unpackExtFiles(binFile, binFile.parent,
//                platform.pathInstalled.toString(), platform.version.toString()
            )
        }
    }
}