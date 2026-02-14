package com.company.commitet_jm.rest.controller

import com.company.commitet_jm.rest.dto.CommitCreateRequest
import com.company.commitet_jm.rest.dto.CommitCreateResponse
import com.company.commitet_jm.rest.dto.ErrorResponse
import com.company.commitet_jm.rest.service.CommitRestService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

/**
 * REST контроллер для управления коммитами.
 *
 * Документация API доступна в docs/REST_API.md
 */
@RestController
@RequestMapping("/api/commits")
class CommitRestController(
    private val commitRestService: CommitRestService
) {
    companion object {
        private val log = LoggerFactory.getLogger(CommitRestController::class.java)
    }

    /**
     * Создает новый коммит.
     *
     * POST /api/commits
     *
     * Пользователь определяется по переданному userId (UUID).
     * Если пользователь не найден, назначается пользователь по умолчанию.
     * Данные файлов передаются в формате Base64.
     *
     * @param request Данные для создания коммита.
     * @return Ответ с результатом создания коммита.
     */
    @PostMapping(
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun createCommit(
        @RequestBody request: CommitCreateRequest
    ): ResponseEntity<Any> {
        log.info("POST /api/commits - создание коммита для проекта {}", request.projectId)

        val validationErrors = mutableListOf<String>()

        if (request.taskNum.isBlank()) {
            validationErrors.add("taskNum не может быть пустым")
        }
        if (request.description.isBlank()) {
            validationErrors.add("description не может быть пустым")
        }

        if (validationErrors.isNotEmpty()) {
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse(
                    error = "VALIDATION_ERROR",
                    message = "Ошибка валидации данных",
                    details = mapOf("errors" to validationErrors)
                ))
        }

        return try {
            val response = commitRestService.createCommit(request)

            if (response.success) {
                ResponseEntity.status(HttpStatus.CREATED).body(response)
            } else {
                val status = when {
                    response.message.contains("не найден") -> HttpStatus.NOT_FOUND
                    else -> HttpStatus.BAD_REQUEST
                }
                ResponseEntity.status(status).body(ErrorResponse(
                    error = "CREATE_FAILED",
                    message = response.message
                ))
            }
        } catch (e: Exception) {
            log.error("Ошибка при создании коммита: {}", e.message, e)
            ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse(
                    error = "INTERNAL_ERROR",
                    message = "Внутренняя ошибка сервера",
                    details = mapOf("exception" to (e.message ?: "Unknown error"))
                ))
        }
    }

    /**
     * Получает статус коммита по ID.
     *
     * GET /api/commits/{commitId}/status
     *
     * @param commitId UUID коммита.
     * @return Статус коммита.
     */
    @GetMapping("/{commitId}/status")
    fun getCommitStatus(
        @PathVariable commitId: UUID
    ): ResponseEntity<Map<String, Any?>> {
        log.info("GET /api/commits/{}/status", commitId)

        return try {
            val commit = commitRestService.getCommitById(commitId)
            if (commit != null) {
                ResponseEntity.ok(mapOf(
                    "commitId" to commit.id,
                    "status" to commit.getStatus()?.name,
                    "hashCommit" to commit.hashCommit,
                    "urlBranch" to commit.urlBranch,
                    "errorInfo" to commit.errorInfo,
                    "dateCreated" to commit.dateCreated
                ))
            } else {
                ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(mapOf("error" to "NOT_FOUND", "message" to "Коммит не найден"))
            }
        } catch (e: Exception) {
            log.error("Ошибка при получении статуса коммита {}: {}", commitId, e.message, e)
            ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to "INTERNAL_ERROR", "message" to e.message))
        }
    }
}
