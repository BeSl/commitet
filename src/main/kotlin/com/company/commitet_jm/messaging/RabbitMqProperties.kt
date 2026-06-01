package com.company.commitet_jm.messaging

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Настройки потребителя очереди RabbitMQ, через которую внешние обработки 1С
 * присылают данные для создания коммитов.
 *
 * Активируется свойством `commit.rabbitmq.enabled=true`. Параметры подключения
 * (host, port, username, password, virtual-host) задаются стандартными
 * свойствами Spring Boot `spring.rabbitmq.*`.
 *
 * @property enabled Включает обработку очереди. По умолчанию выключено, чтобы
 *  приложение стартовало без доступного брокера.
 * @property queue Имя очереди, из которой читаются сообщения с коммитами.
 * @property concurrency Стартовое количество конкурентных потребителей (потоков).
 * @property maxConcurrency Максимальное количество конкурентных потребителей.
 */
@ConfigurationProperties(prefix = "commit.rabbitmq")
data class RabbitMqProperties(
    val enabled: Boolean = false,
    val queue: String = "commitet.commits",
    val concurrency: Int = 1,
    val maxConcurrency: Int = 1
)
