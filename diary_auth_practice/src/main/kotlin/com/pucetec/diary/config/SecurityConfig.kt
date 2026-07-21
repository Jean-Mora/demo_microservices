package com.pucetec.diary.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.web.SecurityFilterChain

/**
 * El SecurityConfig más corto de todo el curso.
 *
 * No hay `hasRole(...)`. No hay `permitAll()`. No hay JwtAuthenticationConverter.
 * El converter existe para traducir `cognito:groups` -> `ROLE_...`, y eso solo sirve
 * si vas a usar `hasRole(...)`. Hoy no hay roles: no hace falta.
 *
 * Y aun así, este archivo NO protege el diario de Beto. Spring Security verifica la
 * firma, el emisor y la expiración del token, y dice "adelante". Saber de quién es la
 * fila 1 no es su trabajo: es el tuyo.
 */
@Configuration
@EnableWebSecurity
class SecurityConfig {

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .authorizeHttpRequests { auth ->
                auth.anyRequest().authenticated()   // TODO. Sin excepciones. No hay nada público.
            }
            .oauth2ResourceServer { oauth2 -> oauth2.jwt { } }
        return http.build()
    }
}
