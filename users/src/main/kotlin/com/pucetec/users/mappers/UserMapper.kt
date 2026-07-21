package com.pucetec.users.mappers

import com.pucetec.users.dto.UserRequest
import com.pucetec.users.dto.UserResponse
import com.pucetec.users.entities.User

// mapea un request + su cognitoId a un entity.
// El cognitoId se pasa aparte porque no viaja en el request: sale del token.
fun UserRequest.toEntity(cognitoId: String) = User(
    cognitoId = cognitoId,
    name = this.name,
    email = this.email,
    phone = this.phone
)

// mapea un entity a un response
fun User.toResponse() = UserResponse(
    id = this.id,
    cognitoId = this.cognitoId,
    name = this.name,
    email = this.email,
    phone = this.phone
)
