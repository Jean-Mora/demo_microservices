package com.pucetec.users.services

import com.pucetec.users.dto.UserRequest
import com.pucetec.users.dto.UserResponse
import com.pucetec.users.entities.User
import com.pucetec.users.exceptions.BlankNameException
import com.pucetec.users.exceptions.DuplicateCognitoIdException
import com.pucetec.users.exceptions.UserNotFoundException
import com.pucetec.users.mappers.toEntity
import com.pucetec.users.mappers.toResponse
import com.pucetec.users.repositories.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

// es el que almacena la logica del negocio
@Service
class UserService(
    private val userRepository: UserRepository
) {

    private val logger = LoggerFactory.getLogger(UserService::class.java)

    // Registra el perfil de un usuario y lo asocia a su cognitoId.
    // El cognitoId sale del token (claim "sub"), no del body.
    fun createUser(cognitoId: String, request: UserRequest): UserResponse {
        logger.info("Creating user profile for cognitoId $cognitoId")

        if (request.name.isBlank()) {
            throw BlankNameException("Name cannot be blank")
        }

        // La relacion cognitoId -> perfil es 1 a 1: no puede haber dos perfiles
        // para el mismo usuario de Cognito.
        if (userRepository.existsByCognitoId(cognitoId)) {
            throw DuplicateCognitoIdException("Ya existe un perfil para el usuario $cognitoId")
        }

        val userEntity = request.toEntity(cognitoId)
        val savedUser = userRepository.save(userEntity)
        return savedUser.toResponse()
    }

    fun getAllUsers(): List<UserResponse> {
        logger.info("Getting all users")
        return userRepository.findAll().map { it.toResponse() }
    }

    fun getUserById(id: Long): UserResponse {
        logger.info("Getting user by id: $id")
        val user = userRepository.findById(id).orElseThrow {
            UserNotFoundException("Usuario $id no encontrado")
        }
        return user.toResponse()
    }

    // El corazon del micro: dado un cognitoId, devuelve los datos propios asociados.
    fun getUserByCognitoId(cognitoId: String): UserResponse {
        logger.info("Getting user by cognitoId: $cognitoId")
        val user = userRepository.findByCognitoId(cognitoId).orElseThrow {
            UserNotFoundException("No existe un perfil para el usuario $cognitoId")
        }
        return user.toResponse()
    }

    fun updateUser(cognitoId: String, request: UserRequest): UserResponse {
        logger.info("Updating user profile for cognitoId $cognitoId")
        val user = userRepository.findByCognitoId(cognitoId).orElseThrow {
            UserNotFoundException("No existe un perfil para el usuario $cognitoId")
        }
        if (request.name.isBlank()) {
            throw BlankNameException("Name cannot be blank")
        }
        val updated = User(
            id = user.id,
            cognitoId = user.cognitoId,
            name = request.name,
            email = request.email,
            phone = request.phone
        )
        return userRepository.save(updated).toResponse()
    }

    fun deleteUser(id: Long) {
        logger.info("Deleting user $id")
        if (!userRepository.existsById(id)) {
            throw UserNotFoundException("Usuario $id no encontrado")
        }
        userRepository.deleteById(id)
    }
}
