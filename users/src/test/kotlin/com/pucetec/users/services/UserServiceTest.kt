package com.pucetec.users.services

import com.pucetec.users.dto.UserRequest
import com.pucetec.users.entities.User
import com.pucetec.users.exceptions.BlankNameException
import com.pucetec.users.exceptions.DuplicateCognitoIdException
import com.pucetec.users.exceptions.UserNotFoundException
import com.pucetec.users.repositories.UserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.any
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.util.Optional

// @ExtendWith activa la integracion de Mockito con JUnit 5.
// Sin esto, las anotaciones @Mock e @InjectMocks no funcionan.
@ExtendWith(MockitoExtension::class)
class UserServiceTest {

    // @Mock crea un repositorio falso: no toca la BD real, simula sus respuestas.
    @Mock
    private lateinit var userRepository: UserRepository

    // @InjectMocks crea el UserService real e inyecta el mock de arriba.
    @InjectMocks
    private lateinit var userService: UserService

    private val cognitoId = "a1b2c3d4-sub-de-cognito"

    @Test
    fun `createUser asocia el cognitoId y retorna el perfil creado`() {
        val request = UserRequest(name = "Ana Lopez", email = "ana@puce.edu", phone = "0999999999")
        val savedUser = User(id = 1L, cognitoId = cognitoId, name = "Ana Lopez", email = "ana@puce.edu", phone = "0999999999")

        `when`(userRepository.existsByCognitoId(cognitoId)).thenReturn(false)
        `when`(userRepository.save(any(User::class.java))).thenReturn(savedUser)

        val response = userService.createUser(cognitoId, request)

        assertEquals(1L, response.id)
        assertEquals(cognitoId, response.cognitoId)
        assertEquals("Ana Lopez", response.name)
        assertEquals("ana@puce.edu", response.email)
    }

    @Test
    fun `createUser lanza BlankNameException cuando el nombre esta vacio`() {
        val request = UserRequest(name = "  ", email = "vacio@puce.edu", phone = null)

        assertThrows<BlankNameException> {
            userService.createUser(cognitoId, request)
        }
    }

    @Test
    fun `createUser lanza DuplicateCognitoIdException cuando el usuario ya tiene perfil`() {
        val request = UserRequest(name = "Ana Lopez", email = "ana@puce.edu", phone = null)

        // Simulamos que ya existe un perfil para ese cognitoId.
        `when`(userRepository.existsByCognitoId(cognitoId)).thenReturn(true)

        assertThrows<DuplicateCognitoIdException> {
            userService.createUser(cognitoId, request)
        }
    }

    @Test
    fun `getUserByCognitoId retorna el perfil asociado cuando existe`() {
        val user = User(id = 5L, cognitoId = cognitoId, name = "Maria", email = "maria@puce.edu", phone = null)

        `when`(userRepository.findByCognitoId(cognitoId)).thenReturn(Optional.of(user))

        val response = userService.getUserByCognitoId(cognitoId)

        assertEquals(5L, response.id)
        assertEquals(cognitoId, response.cognitoId)
        assertEquals("Maria", response.name)
    }

    @Test
    fun `getUserByCognitoId lanza UserNotFoundException cuando no hay perfil asociado`() {
        `when`(userRepository.findByCognitoId(cognitoId)).thenReturn(Optional.empty())

        assertThrows<UserNotFoundException> {
            userService.getUserByCognitoId(cognitoId)
        }
    }

    @Test
    fun `getAllUsers retorna la lista de usuarios`() {
        val users = listOf(
            User(id = 1L, cognitoId = "sub-1", name = "Ana", email = "ana@puce.edu", phone = null),
            User(id = 2L, cognitoId = "sub-2", name = "Juan", email = null, phone = null),
        )
        `when`(userRepository.findAll()).thenReturn(users)

        val responses = userService.getAllUsers()

        assertEquals(2, responses.size)
        assertEquals("Ana", responses[0].name)
        assertEquals("sub-2", responses[1].cognitoId)
    }

    @Test
    fun `updateUser actualiza los datos del perfil existente`() {
        val existing = User(id = 7L, cognitoId = cognitoId, name = "Viejo", email = "viejo@puce.edu", phone = null)
        val request = UserRequest(name = "Nuevo Nombre", email = "nuevo@puce.edu", phone = "0988888888")
        val saved = User(id = 7L, cognitoId = cognitoId, name = "Nuevo Nombre", email = "nuevo@puce.edu", phone = "0988888888")

        `when`(userRepository.findByCognitoId(cognitoId)).thenReturn(Optional.of(existing))
        `when`(userRepository.save(any(User::class.java))).thenReturn(saved)

        val response = userService.updateUser(cognitoId, request)

        assertEquals("Nuevo Nombre", response.name)
        assertEquals("nuevo@puce.edu", response.email)
        assertEquals(cognitoId, response.cognitoId)
    }

    @Test
    fun `deleteUser lanza UserNotFoundException cuando el id no existe`() {
        `when`(userRepository.existsById(99L)).thenReturn(false)

        assertThrows<UserNotFoundException> {
            userService.deleteUser(99L)
        }
    }
}
