package com.pucetec.users.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

// La unica funcion del micro: asociar un cognitoId (el "sub" del token de Cognito)
// con los datos propios del usuario que viven en NUESTRA base de datos.
@Entity
@Table(name = "users")
class User(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,

    // El identificador del usuario en Cognito (claim "sub"). Es unico: un
    // usuario de Cognito se asocia a un unico perfil en este micro.
    @Column(unique = true, nullable = false)
    val cognitoId: String = "",

    val name: String = "",

    val email: String? = null,

    val phone: String? = null,
)
