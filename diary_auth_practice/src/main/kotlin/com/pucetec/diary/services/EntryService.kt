package com.pucetec.diary.services

import com.pucetec.diary.dto.EntryRequest
import com.pucetec.diary.dto.EntryResponse
import com.pucetec.diary.entities.Entry
import com.pucetec.diary.exceptions.EntryNotFoundException
import com.pucetec.diary.exceptions.NotYourEntryException
import com.pucetec.diary.mappers.EntryMapper
import com.pucetec.diary.repositories.EntryRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class EntryService(
    private val entryRepository: EntryRepository,
    private val entryMapper: EntryMapper
) {

    private val logger = LoggerFactory.getLogger(EntryService::class.java)

    fun findMine(author: String): List<EntryResponse> =
        entryMapper.toResponseList(entryRepository.findByAuthorOrderByCreatedAtDesc(author))

    fun create(request: EntryRequest, author: String): EntryResponse {
        logger.info("$author escribió una entrada nueva")
        val entry = entryRepository.save(entryMapper.toEntity(request, author))
        return entryMapper.toResponse(entry)
    }

    fun findOne(id: Long, author: String): EntryResponse =
        entryMapper.toResponse(findMineOrThrow(id, author))

    fun update(id: Long, request: EntryRequest, author: String): EntryResponse {
        val entry = findMineOrThrow(id, author)
        entry.title = request.title
        entry.body = request.body
        return entryMapper.toResponse(entryRepository.save(entry))
    }

    fun delete(id: Long, author: String) {
        val entry = findMineOrThrow(id, author)
        entryRepository.delete(entry)
    }

    /**
     * LA ÚNICA forma de traer una entrada en toda la app, y siempre exige el dueño.
     * Ese `if` de tres líneas es toda la seguridad de la aplicación, y está escrito una sola vez.
     */
    private fun findMineOrThrow(id: Long, author: String): Entry {
        val entry = entryRepository.findById(id)
            .orElseThrow { EntryNotFoundException("No existe la entrada con id $id") }

        if (entry.author != author) {
            throw NotYourEntryException("La entrada $id no es tuya")
        }
        return entry
    }
}
