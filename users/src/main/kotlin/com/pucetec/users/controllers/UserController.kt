package com.pucetec.users.controllers

import com.pucetec.users.dto.UserRequest
import com.pucetec.users.dto.UserResponse
import com.pucetec.users.services.UserService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
class UserController(
    val userService: UserService
) {

    private val logger = LoggerFactory.getLogger(UserController::class.java)

    // ============================================================
    // Endpoints "del propio usuario": el cognitoId sale del token,
    // NO del cliente. Esto demuestra la razon de ser del micro.
    // @AuthenticationPrincipal Jwt jwt -> Spring ya valido el token
    // y nos entrega sus claims. jwt.subject es el "sub" (el cognitoId).
    // ============================================================

    // Registra el perfil del usuario autenticado y lo asocia a su cognitoId.
    @PostMapping("/api/users/me")
    fun createMyProfile(
        @AuthenticationPrincipal jwt: Jwt,
        @RequestBody request: UserRequest
    ): UserResponse {
        val cognitoId = jwt.subject
        logger.info("Creating profile for authenticated user $cognitoId")
        return userService.createUser(cognitoId, request)
    }

    // Devuelve los datos propios asociados al usuario autenticado.
    @GetMapping("/api/users/me")
    fun getMyProfile(
        @AuthenticationPrincipal jwt: Jwt
    ): UserResponse {
        val cognitoId = jwt.subject
        logger.info("Getting profile for authenticated user $cognitoId")
        return userService.getUserByCognitoId(cognitoId)
    }

    // Actualiza los datos propios del usuario autenticado.
    @PutMapping("/api/users/me")
    fun updateMyProfile(
        @AuthenticationPrincipal jwt: Jwt,
        @RequestBody request: UserRequest
    ): UserResponse {
        val cognitoId = jwt.subject
        logger.info("Updating profile for authenticated user $cognitoId")
        return userService.updateUser(cognitoId, request)
    }

    // ============================================================
    // Endpoints administrativos / de consulta.
    // ============================================================

    @GetMapping("/api/users")
    fun getAllUsers(): List<UserResponse> {
        logger.info("Getting all users")
        return userService.getAllUsers()
    }

    @GetMapping("/api/users/{id}")
    fun getUserById(
        @PathVariable id: Long
    ): UserResponse {
        logger.info("Getting user with id: $id")
        return userService.getUserById(id)
    }

    // Consulta directa por cognitoId (util para otros microservicios).
    @GetMapping("/api/users/cognito/{cognitoId}")
    fun getUserByCognitoId(
        @PathVariable cognitoId: String
    ): UserResponse {
        logger.info("Getting user with cognitoId: $cognitoId")
        return userService.getUserByCognitoId(cognitoId)
    }

    @DeleteMapping("/api/users/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteUser(
        @PathVariable id: Long
    ) {
        logger.info("Deleting user $id")
        userService.deleteUser(id)
    }
}
