package com.pucetec.users.repositories

import com.pucetec.users.entities.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

// es el que interactua con la base de datos
@Repository
interface UserRepository : JpaRepository<User, Long> {

    // Spring Data genera la consulta a partir del nombre del metodo:
    // "buscar un User cuyo cognitoId sea igual al parametro".
    fun findByCognitoId(cognitoId: String): Optional<User>

    fun existsByCognitoId(cognitoId: String): Boolean
}
