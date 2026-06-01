package com.company.commitet_jm.messaging

import com.company.commitet_jm.rest.dto.CommitCreateRequest
import com.company.commitet_jm.rest.dto.CommitCreateResponse
import com.company.commitet_jm.rest.service.CommitRestService
import io.jmix.core.security.SystemAuthenticator
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

/**
 * Слушатель очереди RabbitMQ. Работает в отдельном пуле потоков, который задаёт
 * [RabbitMqConfig.commitRabbitListenerContainerFactory], и для каждого сообщения
 * создаёт коммит, переиспользуя [CommitRestService] — ту же логику, что и REST API.
 *
 * Тело сообщения — JSON в формате [CommitCreateRequest].
 */
@Component
@ConditionalOnProperty(prefix = "commit.rabbitmq", name = ["enabled"], havingValue = "true")
class CommitQueueListener(
    private val commitRestService: CommitRestService,
    private val systemAuthenticator: SystemAuthenticator
) {
    companion object {
        private val log = LoggerFactory.getLogger(CommitQueueListener::class.java)
    }

    @RabbitListener(
        queues = ["\${commit.rabbitmq.queue:commitet.commits}"],
        containerFactory = "commitRabbitListenerContainerFactory"
    )
    fun onCommitMessage(request: CommitCreateRequest) {
        log.info(
            "[RABBIT] Получено сообщение очереди: taskNum={}, externalProjectId={}, externalUserId={}, files={}",
            request.taskNum, request.externalProjectId, request.externalUserId, request.files.size
        )

        // REST-сервис работает с DataManager, поэтому выполняем под системным
        // пользователем (вне веб-запроса контекст аутентификации отсутствует).
        var response: CommitCreateResponse? = null
        systemAuthenticator.runWithSystem {
            response = commitRestService.createCommit(request)
        }

        val result = response
        if (result != null && result.success) {
            log.info("[RABBIT] Коммит создан: id={}, author={}", result.commitId, result.authorUsername)
        } else {
            // Бизнес-ошибка (например, проект не найден). Повторная обработка не
            // поможет, поэтому сообщение подтверждаем и не возвращаем в очередь.
            log.error("[RABBIT] Не удалось создать коммит из сообщения: {}", result?.message)
        }
    }
}
