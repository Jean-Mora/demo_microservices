package com.pucetec.diary.controllers

import com.pucetec.diary.dto.MeResponse
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

/**
 * CHECKPOINT 6 — el misterio del email.
 *
 * Este endpoint devuelve, tal cual, lo que el access_token trae dentro.
 * Y el `email` va a salir NULL, aunque el usuario sí tenga email en Cognito.
 *
 *   access_token -> "¿qué puedes hacer?"  Es una LLAVE.  (scope, cognito:groups)
 *   id_token     -> "¿quién eres?"        Es una CÉDULA. (email, nombre, foto)
 *
 * Nuestro backend valida el access_token, así que nunca ve el email de nadie.
 * Y está bien: este microservicio no necesita el email para funcionar.
 */
@RestController
class MeController {

    @GetMapping("/me")
    fun me(@AuthenticationPrincipal jwt: Jwt): MeResponse =
        MeResponse(
            username = jwt.getClaimAsString("username"),
            sub = jwt.subject,
            email = jwt.getClaimAsString("email"),                        // 👀 null. A propósito.
            groups = jwt.getClaimAsStringList("cognito:groups") ?: emptyList()
        )
}
