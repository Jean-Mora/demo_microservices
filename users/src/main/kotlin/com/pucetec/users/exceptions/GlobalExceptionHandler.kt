package com.pucetec.users.exceptions

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(BlankNameException::class)
    fun handleBlankNameException(
        e: BlankNameException
    ): ResponseEntity<ExceptionResponse> {
        val response = ExceptionResponse(
            message = e.message ?: "Nombre en blanco - ERROR",
            source = "UserService"
        )
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(response)
    }

    @ExceptionHandler(UserNotFoundException::class)
    fun handleUserNotFoundException(
        e: UserNotFoundException
    ): ResponseEntity<ExceptionResponse> {
        val response = ExceptionResponse(
            message = e.message ?: "Usuario no encontrado - ERROR",
            source = "UserService"
        )
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(response)
    }

    @ExceptionHandler(DuplicateCognitoIdException::class)
    fun handleDuplicateCognitoIdException(
        e: DuplicateCognitoIdException
    ): ResponseEntity<ExceptionResponse> {
        val response = ExceptionResponse(
            message = e.message ?: "El usuario ya tiene un perfil asociado - ERROR",
            source = "UserService"
        )
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(response)
    }
}

data class ExceptionResponse(
    val message: String,
    val source: String,
)
