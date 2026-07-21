package com.pucetec.diary.exceptions

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(EntryNotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleNotFound(ex: EntryNotFoundException) = mapOf("error" to ex.message)

    // Este 403 lo lanzamos NOSOTROS. Spring Security nunca lo pondría:
    // no sabe de quién es la fila que estás pidiendo.
    @ExceptionHandler(NotYourEntryException::class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    fun handleForbidden(ex: NotYourEntryException) = mapOf("error" to ex.message)
}
