package com.company.commitet_jm

import com.company.commitet_jm.app.ChatHistoryService
import com.vaadin.flow.component.page.AppShellConfigurator
import com.vaadin.flow.component.page.Push
import com.vaadin.flow.server.PWA
import com.vaadin.flow.theme.Theme
import io.jmix.core.DataManager
import io.jmix.flowui.UiEventPublisher
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.event.ApplicationStartedEvent
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.context.event.EventListener
import org.springframework.core.env.Environment
import org.springframework.scheduling.annotation.EnableScheduling
import javax.sql.DataSource

@Push
@Theme(value = "commitet_jm")
@PWA(name = "Commitet_jm", shortName = "Commitet_jm")
@SpringBootApplication
@EnableScheduling
open class CommitetJmApplication : AppShellConfigurator {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(CommitetJmApplication::class.java, *args)
        }
    }

    @Autowired
    private val environment: Environment? = null

    @Bean
    @Primary
    @ConfigurationProperties("main.datasource")
    open fun dataSourceProperties(): DataSourceProperties = DataSourceProperties()

    @Bean
    @Primary
    @ConfigurationProperties("main.datasource.hikari")
    open fun dataSource(dataSourceProperties: DataSourceProperties): DataSource =
            dataSourceProperties.initializeDataSourceBuilder().build()

    @EventListener
    open fun printApplicationUrl(event: ApplicationStartedEvent?) {
        LoggerFactory.getLogger(CommitetJmApplication::class.java).info(
                "Application started at http://localhost:"
                        + (environment?.getProperty("local.server.port") ?: "")
                        + (environment?.getProperty("server.servlet.context-path") ?: ""))
    }

    @Bean
    open fun chatHistoryService(
        dataManager: DataManager,
        uiEventPublisher: UiEventPublisher
    ): ChatHistoryService {
        return ChatHistoryService(dataManager, uiEventPublisher)
    }

}
