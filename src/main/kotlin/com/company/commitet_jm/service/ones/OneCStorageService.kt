package com.company.commitet_jm.service.ones

import com.company.commitet_jm.component.ShellExecutor
import com.company.commitet_jm.entity.OneCStorage
import com.company.commitet_jm.view.onecstorage.HistoryOptions
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.io.File
import java.nio.file.AccessDeniedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

@Component
class OneCStorageService(
    private val executor: ShellExecutor,
    private val oneRunner: OneRunner,
    private val fileUtils: OneCFileUtils
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
            storage.project?.tempBasePath ?: ".${File.separator}temp",
            storage.name
        )

        if (!fileUtils.clearOrCreateDirectory(path)) {
            throw IllegalStateException("Failed to prepare directory: $path")
        }
        return path
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

    /**
     * Очищает содержимое директории, не удаляя саму директорию
     */
    fun clearDirectoryNio(directoryPath: String): Boolean {
        return fileUtils.clearOrCreateDirectory(directoryPath, deleteRoot = false)
    }

    fun generateHistoryReport(storage: OneCStorage, options: HistoryOptions) {
        // Реализация генерации отчета
    }

    fun addUserToStorage(
        storage: OneCStorage,
        username: String,
        password: String,
        rights: UserRights,
        restoreDeleted: Boolean
    ) {
        // Реализация добавления пользователя
    }

    fun copyUsersBetweenStorages(
        source: OneCStorage,
        target: OneCStorage,
        restoreDeleted: Boolean
    ) {
        // Реализация копирования пользователей
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
}
