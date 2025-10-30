package com.company.commitet_jm;

import com.company.commitet_jm.component.ShellExecutor;
import com.company.commitet_jm.service.ChatHistoryService;
import com.company.commitet_jm.service.ones.OneRunner;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.theme.Theme;
import io.jmix.core.DataManager;
import io.jmix.flowui.UiEventPublisher;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.sql.DataSource;

@Push
@Theme(value = "commitet_jm")
@PWA(name = "Commitet_jm", shortName = "Commitet_jm")
@SpringBootApplication
@EnableScheduling
public class CommitetJmApplication implements AppShellConfigurator {

    public static void main(String[] args) {
        SpringApplication.run(CommitetJmApplication.class, args);
    }

    @Autowired
    private Environment environment;

    @Bean
    @Primary
    @ConfigurationProperties("main.datasource")
    public DataSourceProperties dataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @Primary
    @ConfigurationProperties("main.datasource.hikari")
    public DataSource dataSource(DataSourceProperties dataSourceProperties) {
        return dataSourceProperties.initializeDataSourceBuilder().build();
    }

    @EventListener
    public void printApplicationUrl(ApplicationStartedEvent event) {
        LoggerFactory.getLogger(CommitetJmApplication.class).info(
                "Application started at http://localhost:"
                        + (environment.getProperty("local.server.port") != null ? environment.getProperty("local.server.port") : "")
                        + (environment.getProperty("server.servlet.context-path") != null ? environment.getProperty("server.servlet.context-path") : ""));
    }

    @Bean
    public ChatHistoryService chatHistoryService(
            DataManager dataManager,
            UiEventPublisher uiEventPublisher
    ) {
        return new ChatHistoryService(dataManager, uiEventPublisher);
    }

    @Bean
    public ShellExecutor shellExecutor() {
        return new ShellExecutor();
    }

    @Bean
    public OneRunner oneRunner(DataManager dataManager) {
        return new OneRunner(dataManager);
    }
}