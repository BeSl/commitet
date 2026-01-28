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
        log.info("Создание коммита: taskNum={}, projectId={}, userId={}",
            request.taskNum, request.projectId, request.userId)

        // Поиск проекта
        val project = findProjectById(request.projectId)
            ?: return CommitCreateResponse(
                success = false,
                message = "Проект с ID ${request.projectId} не найден"
            )

        // Поиск пользователя или назначение пользователя по умолчанию
        val author = findUserOrDefault(request.userId)
            ?: return CommitCreateResponse(
                success = false,
                message = "Пользователь не найден и пользователь по умолчанию ($defaultUsername) не настроен"
            )

        val userWasDefault = request.userId != null && request.userId != author.id
        if (userWasDefault) {
            log.warn("Пользователь с ID {} не найден, назначен пользователь по умолчанию: {}",
                request.userId, author.username)
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
     * Ищет пользователя по UUID. Если пользователь не найден или userId равен null,
     * возвращает пользователя по умолчанию.
     *
     * @param userId UUID пользователя для поиска (может быть null).
     * @return Найденный пользователь или пользователь по умолчанию, либо null если ничего не найдено.
     */
    private fun findUserOrDefault(userId: UUID?): User? {
        // Если указан userId, пробуем найти пользователя
        if (userId != null) {
            val user = dataManager.load(User::class.java)
                .id(userId)
                .optional()
                .orElse(null)

            if (user != null) {
                log.debug("Найден пользователь по ID {}: {}", userId, user.username)
                return user
            }
            log.warn("Пользователь с ID {} не найден, ищем пользователя по умолчанию", userId)
        }

        // Поиск пользователя по умолчанию
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
