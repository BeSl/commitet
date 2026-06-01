package com.company.commitet_jm.messaging

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.amqp.core.Queue
import org.springframework.amqp.core.QueueBuilder
import org.springframework.amqp.rabbit.annotation.EnableRabbit
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.amqp.support.converter.MessageConverter
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Конфигурация инфраструктуры RabbitMQ для чтения очереди коммитов.
 *
 * Бины создаются только если `commit.rabbitmq.enabled=true`, поэтому при
 * отключённой интеграции приложение не пытается подключиться к брокеру и
 * стартует штатно.
 */
@Configuration
@EnableRabbit
@EnableConfigurationProperties(RabbitMqProperties::class)
@ConditionalOnProperty(prefix = "commit.rabbitmq", name = ["enabled"], havingValue = "true")
open class RabbitMqConfig(
    private val properties: RabbitMqProperties
) {

    /**
     * Объявляем durable-очередь, чтобы сообщения переживали перезапуск брокера.
     */
    @Bean
    open fun commitQueue(): Queue = QueueBuilder.durable(properties.queue).build()

    /**
     * Конвертер тела сообщения JSON -> DTO. Переиспользуем общий ObjectMapper
     * (с зарегистрированным Kotlin-модулем) из контекста Spring.
     */
    @Bean
    open fun commitMessageConverter(objectMapper: ObjectMapper): MessageConverter =
        Jackson2JsonMessageConverter(objectMapper)

    /**
     * Отдельная фабрика контейнеров слушателей: задаёт собственный пул потоков
     * для обработки очереди коммитов и отключает повторную постановку
     * «ядовитых» сообщений в очередь.
     */
    @Bean
    open fun commitRabbitListenerContainerFactory(
        connectionFactory: ConnectionFactory,
        commitMessageConverter: MessageConverter
    ): SimpleRabbitListenerContainerFactory {
        return SimpleRabbitListenerContainerFactory().apply {
            setConnectionFactory(connectionFactory)
            setMessageConverter(commitMessageConverter)
            setConcurrentConsumers(properties.concurrency)
            setMaxConcurrentConsumers(properties.maxConcurrency)
            setDefaultRequeueRejected(false)
        }
    }
}
