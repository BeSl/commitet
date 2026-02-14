package com.company.commitet_jm.rest.service

import com.company.commitet_jm.entity.*
import com.company.commitet_jm.rest.dto.CommitCreateRequest
import com.company.commitet_jm.rest.dto.CommitCreateResponse
import com.company.commitet_jm.rest.dto.FileData
import io.jmix.core.DataManager
import io.jmix.core.FileStorage
import io.jmix.core.FileStorageLocator
import io.jmix.core.querycondition.PropertyCondition
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.ByteArrayInputStream
import java.time.LocalDateTime
import java.util.*

/**
 * Сервис для обработки REST запросов на создание коммитов.
 */
@Service
class CommitRestService(
    private val dataManager: DataManager,
    private val fileStorageLocator: FileStorageLocator
) {
    companion object {
        private val log = LoggerFactory.getLogger(CommitRestService::class.java)
    }

    /**
     * Username пользователя по умолчанию, используется если указанный пользователь не найден.
     */
    @Value("\${commit.default.user:admin}")
    private lateinit var defaultUsername: String

    /**
     * Создает новый коммит на основе данных из REST запроса.
     *
     * @param request Данные запроса на создание коммита.
     * @return Ответ с результатом создания коммита.
     */
    fun createCommit(request: CommitCreateRequest): CommitCreateResponse {
        log.info("Создание коммита: taskNum={}, externalProjectId={}, externalUserId={}",
            request.taskNum, request.externalProjectId, request.externalUserId)

        // Поиск проекта по внешнему ID или основному UUID
        val project = if (!request.externalProjectId.isNullOrBlank()) {
            findProjectByExternalId(request.externalProjectId)
        } else {
            request.projectId?.let { findProjectById(it) }
        } ?: return CommitCreateResponse(
            success = false,
            message = "Проект не найден"
        )

        // Поиск пользователя по внешнему ID
        val author = findUserByExternalId(request.externalUserId)
            ?: return CommitCreateResponse(
                success = false,
                message = "Пользователь не найден и пользователь по умолчанию ($defaultUsername) не настроен"
            )

        val userWasDefault = !request.externalUserId.isNullOrBlank() && author.username == defaultUsername
        if (userWasDefault) {
            log.warn("Пользователь с внешним ID {} не найден, назначен пользователь по умолчанию: {}",
                request.externalUserId, author.username)
        }

        // Создание коммита
        val commit = dataManager.create(Commit::class.java).apply {
            this.author = author
            this.project = project
            this.taskNum = request.taskNum
            this.description = request.description
            this.fixCommit = request.fixCommit
            this.setStatus(StatusSheduler.NEW)
            this.dateCreated = LocalDateTime.now()
        }

        // Сохранение коммита
        val savedCommit = dataManager.save(commit)

        // Создание и сохранение файлов
        if (request.files.isNotEmpty()) {
            val fileCommits = createFileCommits(request.files, savedCommit)
            dataManager.save(*fileCommits.toTypedArray())
            log.info("Сохранено {} файлов для коммита {}", fileCommits.size, savedCommit.id)
        }

        val message = if (userWasDefault) {
            "Коммит создан успешно. Указанный пользователь не найден, назначен пользователь по умолчанию."
        } else {
            "Коммит создан успешно"
        }

        log.info("Коммит создан: id={}, author={}", savedCommit.id, author.username)

        return CommitCreateResponse(
            success = true,
            commitId = savedCommit.id,
            message = message,
            authorId = author.id,
            authorUsername = author.username
        )
    }

    /**
     * Ищет пользователя по внешнему ID.
     * Если не найден, возвращает пользователя по умолчанию.
     *
     * @param externalUserId Внешний ID пользователя для поиска (может быть null).
     * @return Найденный пользователь или пользователь по умолчанию, либо null если ничего не найдено.
     */
    private fun findUserByExternalId(externalUserId: String?): User? {
        if (externalUserId.isNullOrBlank()) {
            log.warn("External user ID пустой, ищем пользователя по умолчанию")
            return findDefaultUser()
        }

        // Поиск UserExternalId по externalId
        val userExternalId = dataManager.load(UserExternalId::class.java)
            .query("select e from UserExternalId e where e.externalId = :externalId")
            .parameter("externalId", externalUserId)
            .optional()
            .orElse(null)

        if (userExternalId != null) {
            log.debug("Найден пользователь по внешнему ID {}: {}",
                externalUserId, userExternalId.user?.username)
            return userExternalId.user
        }

        log.warn("Пользователь с внешним ID {} не найден, используем пользователя по умолчанию",
            externalUserId)
        return findDefaultUser()
    }

    /**
     * Ищет пользователя по умолчанию по username из настроек.
     *
     * @return Пользователь по умолчанию или null если не найден.
     */
    private fun findDefaultUser(): User? {
        val defaultUser = dataManager.load(User::class.java)
            .condition(PropertyCondition.equal("username", defaultUsername))
            .optional()
            .orElse(null)

        if (defaultUser != null) {
            log.debug("Найден пользователь по умолчанию: {}", defaultUser.username)
        } else {
            log.error("Пользователь по умолчанию с username '{}' не найден", defaultUsername)
        }

        return defaultUser
    }

    /**
     * Ищет проект по UUID.
     *
     * @param projectId UUID проекта.
     * @return Найденный проект или null.
     */
    private fun findProjectById(projectId: UUID): Project? {
        return dataManager.load(Project::class.java)
            .id(projectId)
            .optional()
            .orElse(null)
    }

    /**
     * Ищет проект по внешнему ID.
     *
     * @param externalProjectId Внешний ID проекта.
     * @return Найденный проект или null.
     */
    private fun findProjectByExternalId(externalProjectId: String?): Project? {
        if (externalProjectId.isNullOrBlank()) {
            return null
        }

        val projectExternalId = dataManager.load(ProjectExternalId::class.java)
            .query("select e from ProjectExternalId e where e.externalId = :externalId")
            .parameter("externalId", externalProjectId)
            .optional()
            .orElse(null)

        if (projectExternalId != null) {
            log.debug("Найден проект по внешнему ID {}: {}",
                externalProjectId, projectExternalId.project?.name)
            return projectExternalId.project
        }

        return null
    }

    /**
     * Получает коммит по UUID.
     *
     * @param commitId UUID коммита.
     * @return Найденный коммит или null.
     */
    fun getCommitById(commitId: UUID): Commit? {
        return dataManager.load(Commit::class.java)
            .id(commitId)
            .optional()
            .orElse(null)
    }

    /**
     * Создает список FileCommit из данных запроса.
     * Декодирует Base64 данные и сохраняет файлы в FileStorage.
     *
     * @param files Список данных файлов из запроса.
     * @param commit Коммит, к которому привязываются файлы.
     * @return Список созданных FileCommit сущностей.
     */
    private fun createFileCommits(files: List<FileData>, commit: Commit): List<FileCommit> {
        val fileStorage = fileStorageLocator.getDefault<FileStorage>()

        return files.mapNotNull { fileData ->
            try {
                // Декодирование Base64
                val decodedBytes = Base64.getDecoder().decode(fileData.data)

                // Определение типа файла
                val fileType = TypesFiles.values().find {
                    it.name.equals(fileData.type, ignoreCase = true)
                }

                if (fileType == null) {
                    log.warn("Неизвестный тип файла: {}, пропускаем файл: {}", fileData.type, fileData.name)
                    return@mapNotNull null
                }

                // Сохранение файла в FileStorage
                val fileRef = ByteArrayInputStream(decodedBytes).use { inputStream ->
                    fileStorage.saveStream(fileData.name, inputStream)
                }

                // Создание FileCommit
                dataManager.create(FileCommit::class.java).apply {
                    this.name = fileData.name
                    this.data = fileRef
                    this.setType(fileType)
                    this.code = fileData.code
                    this.commit = commit
                }
            } catch (e: IllegalArgumentException) {
                log.error("Ошибка декодирования Base64 для файла {}: {}", fileData.name, e.message)
                null
            } catch (e: Exception) {
                log.error("Ошибка при создании файла {}: {}", fileData.name, e.message, e)
                null
            }
        }
    }
}
