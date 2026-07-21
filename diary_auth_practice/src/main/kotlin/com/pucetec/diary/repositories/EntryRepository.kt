package com.pucetec.diary.repositories

import com.pucetec.diary.entities.Entry
import org.springframework.data.jpa.repository.JpaRepository

interface EntryRepository : JpaRepository<Entry, Long> {

    // Para LISTAR nunca usamos findAll(): la consulta exige el dueño.
    fun findByAuthorOrderByCreatedAtDesc(author: String): List<Entry>
}
