package com.company.commitet_jm.service.project

import com.company.commitet_jm.entity.Project
import com.company.commitet_jm.service.git.GitService
import io.jmix.core.DataManager
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

/**
 * Сервис для управления проектами и их репозиториями
 */
@Service
class ProjectService(
    private val dataManager: DataManager,
    private val gitService: GitService
) {
    companion object {
        private val log = LoggerFactory.getLogger(ProjectService::class.java)
    }

    @Value("\${app.workdir:./repos}")
    private lateinit var workDir: String

    /**
     * Получить рабочий каталог приложения
     */
    fun getWorkDir(): String = workDir

    /**
     * Получить путь к репозиторию проекта
     * Формируется как: workdir / sanitized_project_name
     */
    fun getProjectRepoPath(project: Project): String {
        val projectDirName = sanitizeProjectDirName(project.name ?: project.id.toString())
        return Paths.get(workDir, projectDirName).toAbsolutePath().toString()
    }

    /**
     * Получить путь к репозиторию проекта по имени
     */
    fun getProjectRepoPath(projectName: String): String {
        val projectDirName = sanitizeProjectDirName(projectName)
        return Paths.get(workDir, projectDirName).toAbsolutePath().toString()
    }

    /**
     * Клонировать репозиторий проекта
     * @return Pair<успех, сообщение об ошибке или путь>
     */
    fun cloneProjectRepo(project: Project): Pair<Boolean, String> {
        val urlRepo = project.urlRepo
        val defaultBranch = project.defaultBranch

        if (urlRepo.isNullOrBlank()) {
            return Pair(false, "URL репозитория не указан")
        }

        if (defaultBranch.isNullOrBlank()) {
            return Pair(false, "Ветка по умолчанию не указана")
        }

        val repoPath = getProjectRepoPath(project)
        val repoDir = File(repoPath)

        // Проверяем, не существует ли уже каталог с файлами
        if (repoDir.exists() && repoDir.list()?.isNotEmpty() == true) {
            log.warn("Каталог $repoPath уже существует и не пуст")
            return Pair(false, "Каталог $repoPath уже существует и не пуст. Удалите его или выберите другое имя проекта.")
        }

        // Создаём родительский каталог если не существует
        val workDirPath = Paths.get(workDir)
        if (!Files.exists(workDirPath)) {
            Files.createDirectories(workDirPath)
            log.info("Создан рабочий каталог: $workDir")
        }

        // Формируем URL с .git если нужно
        var fullUrlRepo = urlRepo
        if (!fullUrlRepo.endsWith(".git")) {
            fullUrlRepo += ".git"
        }

        log.info("Клонирование репозитория $fullUrlRepo в $repoPath")

        val result = gitService.cloneRepo(fullUrlRepo, repoPath, defaultBranch)

        if (result.first) {
            // Обновляем localPath в проекте
            project.localPath = repoPath
            dataManager.save(project)
            log.info("Репозиторий успешно склонирован в $repoPath")
            return Pair(true, repoPath)
        } else {
            log.error("Ошибка клонирования: ${result.second}")
            return result
        }
    }

    /**
     * Проверить, склонирован ли репозиторий проекта
     */
    fun isProjectCloned(project: Project): Boolean {
        val repoPath = project.localPath ?: return false
        val gitDir = File(repoPath, ".git")
        return gitDir.exists() && gitDir.isDirectory
    }

    /**
     * Очистить имя проекта для использования как имя каталога
     * Удаляет/заменяет недопустимые символы
     */
    fun sanitizeProjectDirName(name: String): String {
        return name
            .trim()
            .replace(Regex("[\\\\/:*?\"<>|]"), "_")  // Заменяем недопустимые символы
            .replace(Regex("\\s+"), "_")              // Заменяем пробелы
            .replace(Regex("_+"), "_")                // Убираем множественные подчёркивания
            .removePrefix("_")
            .removeSuffix("_")
            .take(100)  // Ограничиваем длину
            .ifEmpty { "project" }
    }
}
