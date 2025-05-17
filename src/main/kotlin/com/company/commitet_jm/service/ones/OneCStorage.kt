package com.company.commitet_jm.service.ones

//ПРИМЕР ПУТИ после имени диска 2 СЛЭША
//        val pathSource = """"C:\\develop\test\repo\src\""""

import com.company.commitet_jm.component.ShellExecutor
import com.company.commitet_jm.entity.OneCStorage
import com.company.commitet_jm.entity.Platform
import com.company.commitet_jm.service.GitWorker
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.io.IOException
import java.nio.file.AccessDeniedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

@Component
class OneCStorageService(
        private val executor: ShellExecutor,
        private val oneRunner: OneRunner
    ) {
        companion object {
            private val log = LoggerFactory.getLogger(OneCStorageService::class.java)
            private const val CONFIG_DIR = "src"
        }

        fun createOneCStorage(storage: OneCStorage) {
            requireNotNull(storage.project) { "Storage must be linked to project" }

            val basePath = prepareStorageDirectory(storage)
            createEmptyBase(storage, basePath)
            loadConfiguration(storage, basePath)
            setupRepository(storage, basePath)
        }

        private fun prepareStorageDirectory(storage: OneCStorage): Path {
            val path = Paths.get(
                storage.project?.tempBasePath ?: "./temp",
                storage.name
            )

            if (!clearOrCreateDirectory(path)) {
                throw IllegalStateException("Failed to prepare directory: $path")
            }
            return path
        }

        private fun clearOrCreateDirectory(path: Path): Boolean {
            return try {
                if (Files.exists(path)) {
                    Files.walk(path)
                        .sorted(reverseOrder())
                        .forEach { Files.deleteIfExists(it) }
                }
                Files.createDirectories(path)
                true
            } catch (e: IOException) {
                log.error("Directory operation failed: ${e.message}")
                false
            }
        }

        private fun createEmptyBase(storage: OneCStorage, path: Path) {
            val absPath = path.toAbsolutePath().toString()
            executor.executeCommand(
                command = listOf(
                    getPlatformPath(storage),
                    "CREATEINFOBASE",
                    "File=\"${absPath}\""
                )
            )
        }

        private fun loadConfiguration(storage: OneCStorage, basePath: Path) {
            val srcPath = Paths.get(storage.project?.localPath ?: "", CONFIG_DIR)

            executor.executeCommand(
                command = listOf(
                    getPlatformPath(storage),
                    "DESIGNER",
                    "/F", basePath.toString(),
                    "/LoadConfigFromFiles", srcPath.toString(),
                    "/UpdateDBCfg"
                )
            )
        }

        private fun setupRepository(storage: OneCStorage, basePath: Path) {
            executor.executeCommand(
                command = listOf(
                    getPlatformPath(storage),
                    "DESIGNER",
                    "/F", basePath.toString(),
                    "/ConfigurationRepositoryF", storage.path,
                    "/ConfigurationRepositoryN", storage.user,
                    "/ConfigurationRepositoryP", storage.password,
                    "/ConfigurationRepositoryCreate"
                )
            )
        }

        fun clearDirectoryNio(directoryPath: String): Boolean {
            val path = Paths.get(directoryPath)

        if (!Files.exists(path) || !Files.isDirectory(path)) {
            log.info("Каталог $directoryPath не существует или это не каталог")
            return false
        }

        return try {
            Files.walk(path)
                .sorted(Comparator.reverseOrder())
                .filter { it != path } // Не удаляем сам корневой каталог
                .forEach { filePath ->
                    Files.deleteIfExists(filePath)
                }
            log.info("Содержимое каталога $directoryPath успешно удалено (NIO)")
            true
        } catch (e: SecurityException) {
            log.error("Ошибка безопасности при удалении содержимого каталога: ${e.message}")
            false
        } catch (e: Exception) {
            log.error("Произошла ошибка при удалении содержимого каталога: ${e.message}")
            false
        }
    }


fun historyStorage(
    storage: OneCStorage,
    outputFile: Path,
    options: HistoryStorageOptions = HistoryStorageOptions()
) {
    requireNotNull(storage.path) { "Storage path must be configured" }
    validateFileWriteAccess(outputFile)

    val command = buildHistoryCommand(storage, outputFile, options)
    executeConfigurationCommand(storage, command)
}

fun addUserStorage(
    storage: OneCStorage,
    name: String,
    password: String,
    rights: UserRights,
    options: UserStorageOptions = UserStorageOptions()
) {
    validateCredentials(name, password)
    requireNotNull(storage.path) { "Storage path must be configured" }

    val command = buildAddUserCommand(storage, name, password, rights, options)
    executeConfigurationCommand(storage, command)
}

    fun copyUsersStorage(
        sourceStorage: OneCStorage,
        targetStorage: OneCStorage,
        adminUser: String,
        adminPassword: String,
        options: CopyUsersOptions = CopyUsersOptions()
        ) {
            requireNotNull(sourceStorage.path) { "Source storage path must be configured" }
            requireNotNull(targetStorage.path) { "Target storage path must be configured" }
            validateCredentials(adminUser, adminPassword)

            val command = buildCopyUsersCommand(sourceStorage, targetStorage, adminUser, adminPassword, options)
            executeConfigurationCommand(sourceStorage, command)
        }

private fun buildHistoryCommand(
    storage: OneCStorage,
    outputFile: Path,
    options: HistoryStorageOptions
): List<String> {
    return mutableListOf<String>().apply {
        add("DESIGNER")
        add("/ConfigurationRepositoryReport")
        add(outputFile.toAbsolutePath().toString())

        options.versionStart?.let { add("-NBegin"); add(it.toString()) }
        options.versionEnd?.let { add("-NEnd"); add(it.toString()) }
        options.dateStart?.let { add("-DateBegin"); add(it) }
        options.dateEnd?.let { add("-DateEnd"); add(it) }

        if (options.groupByObject) add("-GroupByObject")
        if (options.groupByComment) add("-GroupByComment")

        add("-ReportFormat")
        add(options.reportFormat.name)
    }
}





private fun executeConfigurationCommand(storage: OneCStorage, commandParts: List<String>) {
    try {
        val baseCommand = listOf(
            getPlatformPath(storage),
            "/F", storage.path!!
        )

        val fullCommand = baseCommand + commandParts
        log.info("Executing command: ${fullCommand.joinToString(" ")}")

        executor.executeCommand(fullCommand)
    } catch (e: RuntimeException) {
        log.error("Command execution failed: ${e.message}")
        throw StorageOperationException("Failed to execute configuration command", e)
    }
}

private fun validateCredentials(username: String, password: String) {
    require(username.isNotBlank()) { "Username cannot be empty" }
    require(password.isNotBlank()) { "Password cannot be empty" }
}

private fun validateFileWriteAccess(file: Path) {
    if (Files.exists(file) && !Files.isWritable(file)) {
        throw AccessDeniedException("No write access to file: $file")
    }
}

private fun getPlatformPath(storage: OneCStorage): String {
    val platform = requireNotNull(storage.project?.platform) { "Platform not configured" }
    return oneRunner.pathPlatform(platform.pathInstalled, platform.version)
}




// создать нового пользователя
//    /ConfigurationRepositoryAddUser



}

private fun buildAddUserCommand(
    storage: OneCStorage,
    name: String,
    password: String,
    rights: UserRights,
    options: UserStorageOptions
): List<String> {
    return mutableListOf<String>().apply {
        add("DESIGNER")
        add("/ConfigurationRepositoryAddUser")
        add("-User"); add(name)
        add("-Pwd"); add(password)
        add("-Rights"); add(rights.cliValue)

        if (options.restoreDeleted) add("-RestoreDeletedUser")
        options.extension?.let { add("-Extension"); add(it) }
    }
}

private fun buildCopyUsersCommand(
    source: OneCStorage,
    target: OneCStorage,
    user: String,
    password: String,
    options: CopyUsersOptions
): List<String> {
    return mutableListOf<String>().apply {
        add("DESIGNER")
        add("/ConfigurationRepositoryCopyUsers")
        add("-Path"); add(target.path!!)
        add("-User"); add(user)
        add("-Pwd"); add(password)

        if (options.restoreDeleted) add("-RestoreDeletedUser")
        options.extension?.let { add("-Extension"); add(it) }
    }
}

data class HistoryStorageOptions(
    val versionStart: Int? = null,
    val versionEnd: Int? = null,
    val dateStart: String? = null,
    val dateEnd: String? = null,
    val reportFormat: ReportFormat = ReportFormat.TXT,
    val groupByObject: Boolean = false,
    val groupByComment: Boolean = false
)

enum class ReportFormat { TXT, MXL }

data class UserStorageOptions(
    val restoreDeleted: Boolean = false,
    val extension: String? = null
)

data class CopyUsersOptions(
    val restoreDeleted: Boolean = false,
    val extension: String? = null
)
enum class UserRights(val cliValue: String) {
    READ_ONLY("ReadOnly"),
    FULL_ACCESS("FullAccess"),
    VERSION_MANAGEMENT("VersionManagement");
}

class StorageOperationException(message: String, cause: Throwable?) : RuntimeException(message, cause)
