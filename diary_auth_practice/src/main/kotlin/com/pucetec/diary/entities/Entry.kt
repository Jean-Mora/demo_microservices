package com.pucetec.diary.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "entries")
class Entry(

    var title: String,

    @Column(columnDefinition = "text")
    var body: String,

    // El dueño de la entrada. Es el `username` del JWT. NUNCA viene del body.
    var author: String,

    @Column(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0
)
