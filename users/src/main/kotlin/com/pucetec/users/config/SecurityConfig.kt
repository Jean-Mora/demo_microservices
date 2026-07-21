package com.pucetec.users.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.web.SecurityFilterChain

@Configuration
class SecurityConfig {

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            // Reglas de autorizacion por ruta.
            .authorizeHttpRequests { auth ->
                auth
                    // La consola H2 queda abierta solo para practicar en clase.
                    .requestMatchers("/h2-console/**").permitAll()
                    // Todo lo demas (los /api/**) exige un token valido de Cognito.
                    .anyRequest().authenticated()
            }
            // Convierte la app en un Resource Server: valida el JWT usando el
            // issuer-uri configurado en application.yaml (descarga el JWKS de Cognito
            // y verifica firma, expiracion e issuer del token).
            .oauth2ResourceServer { oauth2 ->
                oauth2.jwt { }
            }
            // CSRF no aplica a una API stateless que se autentica con Bearer token.
            .csrf { it.disable() }
            // La consola H2 se renderiza dentro de un <iframe>; hay que permitir frames del mismo origen.
            .headers { headers ->
                headers.frameOptions { it.sameOrigin() }
            }

        return http.build()
    }
}
