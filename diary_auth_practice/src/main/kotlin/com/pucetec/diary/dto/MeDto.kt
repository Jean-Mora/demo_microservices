package com.pucetec.diary.dto

/**
 * Lo que el backend sabe de ti, y SOLO lo que el access_token le dijo.
 *
 * Ojo con `email`: va a salir null. No es un bug — es que el email NO viaja en el
 * access_token de Cognito, viaja en el id_token. Ese es el Checkpoint 6.
 */
data class MeResponse(
    val username: String,
    val sub: String,
    val email: String?,
    val groups: List<String>
)
