package com.pucetec.users.exceptions

// Se lanza cuando se intenta registrar un perfil para un cognitoId
// que ya tiene uno asociado (la relacion cognitoId -> perfil es 1 a 1).
class DuplicateCognitoIdException(
    message: String? = null
) : Exception(message)
