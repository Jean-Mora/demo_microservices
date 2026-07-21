package com.pucetec.diary.mappers

import com.pucetec.diary.dto.EntryRequest
import com.pucetec.diary.dto.EntryResponse
import com.pucetec.diary.entities.Entry
import org.springframework.stereotype.Component

@Component
class EntryMapper {

    /**
     * El `author` entra por parámetro, JAMÁS desde el request: el DTO ni siquiera tiene ese campo.
     */
    fun toEntity(request: EntryRequest, author: String): Entry =
        Entry(
            title = request.title,
            body = request.body,
            author = author
        )

    fun toResponse(entry: Entry): EntryResponse =
        EntryResponse(
            id = entry.id,
            title = entry.title,
            body = entry.body,
            author = entry.author,
            createdAt = entry.createdAt
        )

    fun toResponseList(entries: List<Entry>): List<EntryResponse> = entries.map { toResponse(it) }
}
