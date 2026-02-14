package com.company.commitet_jm.security

import io.jmix.core.JmixSecurityFilterChainOrder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.web.SecurityFilterChain

/**
 * This configuration complements standard security configurations that come from Jmix modules (security-flowui, oidc,
 * authserver).
 *
 * You can configure custom API endpoints security by defining [SecurityFilterChain] beans in this class.
 * In most cases, custom SecurityFilterChain must be applied first, so the proper
 * [org.springframework.core.annotation.Order] should be defined for the bean. The order value from the
 * [io.jmix.core.JmixSecurityFilterChainOrder#CUSTOM] is guaranteed to be smaller than any other filter chain
 * order from Jmix.
 *
 * @see io.jmix.securityflowui.security.FlowuiVaadinWebSecurity
 */
@Configuration
open class CommitetJmSecurityConfiguration {

    /**
     * Конфигурация безопасности для REST API.
     * API требует базовой аутентификации (HTTP Basic).
     */
    @Bean
    @Order(JmixSecurityFilterChainOrder.CUSTOM)
    open fun restApiSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .securityMatcher("/api/**", "/newcmt/**")
            .authorizeHttpRequests { authorize ->
                authorize.anyRequest().authenticated()
            }
            .httpBasic { }
            .csrf { it.disable() }

        return http.build()
    }
}
