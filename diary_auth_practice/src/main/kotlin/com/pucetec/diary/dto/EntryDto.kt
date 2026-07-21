package com.pucetec.diary.dto

import java.time.LocalDateTime

// Dos campos. No hay "author": ese dato jamás se acepta del cliente.
data class EntryRequest(
    val title: String,
    val body: String
)

data class EntryResponse(
    val id: Long,
    val title: String,
    val body: String,
    val author: String,
    val createdAt: LocalDateTime
)
