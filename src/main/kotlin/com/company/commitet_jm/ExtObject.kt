package com.company.commitet_jm

import com.company.commitet_jm.rest.dto.CommitCreateRequest
import com.company.commitet_jm.rest.service.CommitRestService
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * Legacy REST контроллер для обратной совместимости.
 * Рекомендуется использовать новый API: POST /api/commits
 *
 * @deprecated Используйте CommitRestController (/api/commits) вместо этого эндпоинта.
 */
@RestController
@RequestMapping("/newcmt")
@Deprecated("Используйте /api/commits вместо /newcmt")
class ExtObject(
    private val commitRestService: CommitRestService
) {
    /**
     * Создает новый коммит (legacy endpoint).
     * Перенаправляет на новый сервис создания коммитов.
     */
    @PostMapping(
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun createCommit(@RequestBody request: CommitCreateRequest): ResponseEntity<Any> {
        val response = commitRestService.createCommit(request)
        return if (response.success) {
            ResponseEntity.ok(response)
        } else {
            ResponseEntity.badRequest().body(response)
        }
    }
}
