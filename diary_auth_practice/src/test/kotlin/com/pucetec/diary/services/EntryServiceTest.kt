package com.pucetec.diary.services

import com.pucetec.diary.dto.EntryRequest
import com.pucetec.diary.entities.Entry
import com.pucetec.diary.exceptions.EntryNotFoundException
import com.pucetec.diary.exceptions.NotYourEntryException
import com.pucetec.diary.mappers.EntryMapper
import com.pucetec.diary.repositories.EntryRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.Optional

class EntryServiceTest {

    private lateinit var entryRepository: EntryRepository
    private lateinit var entryService: EntryService

    @BeforeEach
    fun setUp() {
        entryRepository = mock()
        // El mapper es puro: usarlo de verdad prueba también que el service responde DTOs.
        entryService = EntryService(entryRepository, EntryMapper())
    }

    private fun entradaDeAna() = Entry(title = "Martes horrible", body = "...", author = "ana", id = 1)

    // ---------- Camino feliz ----------

    @Test
    fun `listar mis entradas consulta SOLO por mi autor`() {
        whenever(entryRepository.findByAuthorOrderByCreatedAtDesc("ana"))
            .thenReturn(listOf(entradaDeAna()))

        val resultado = entryService.findMine("ana")

        assertEquals(1, resultado.size)
        assertEquals("ana", resultado.first().author)
        // Nunca se llama findAll(): no hay forma de pedir "todas las entradas".
        verify(entryRepository, never()).findAll()
    }

    @Test
    fun `crear una entrada la firma con el autor recibido`() {
        whenever(entryRepository.save(any<Entry>())).thenAnswer { it.arguments[0] as Entry }

        val entrada = entryService.create(EntryRequest("Martes horrible", "..."), "ana")

        assertEquals("ana", entrada.author)
        assertEquals("Martes horrible", entrada.title)
    }

    @Test
    fun `puedo leer mi propia entrada`() {
        whenever(entryRepository.findById(1)).thenReturn(Optional.of(entradaDeAna()))

        val entrada = entryService.findOne(1, "ana")

        assertEquals("ana", entrada.author)
    }

    @Test
    fun `puedo editar mi propia entrada`() {
        whenever(entryRepository.findById(1)).thenReturn(Optional.of(entradaDeAna()))
        whenever(entryRepository.save(any<Entry>())).thenAnswer { it.arguments[0] as Entry }

        val entrada = entryService.update(1, EntryRequest("Martes mejor", "ya pasó"), "ana")

        assertEquals("Martes mejor", entrada.title)
        assertEquals("ya pasó", entrada.body)
    }

    @Test
    fun `puedo borrar mi propia entrada`() {
        val entrada = entradaDeAna()
        whenever(entryRepository.findById(1)).thenReturn(Optional.of(entrada))

        entryService.delete(1, "ana")

        verify(entryRepository).delete(entrada)
    }

    // ---------- 🔒 Los que blindan el bug del Checkpoint 4 ----------

    @Test
    fun `NO puedo leer la entrada de otro usuario`() {
        // La entrada 1 SÍ existe en la base… pero es de ana.
        whenever(entryRepository.findById(1)).thenReturn(Optional.of(entradaDeAna()))

        assertThrows(NotYourEntryException::class.java) {   // 403, no 404: la entrada existe
            entryService.findOne(1, "beto")
        }
    }

    @Test
    fun `NO puedo editar la entrada de otro usuario`() {
        whenever(entryRepository.findById(1)).thenReturn(Optional.of(entradaDeAna()))

        assertThrows(NotYourEntryException::class.java) {
            entryService.update(1, EntryRequest("hackeada", "jaja"), "beto")
        }
        verify(entryRepository, never()).save(any<Entry>())
    }

    @Test
    fun `NO puedo borrar la entrada de otro usuario`() {
        whenever(entryRepository.findById(1)).thenReturn(Optional.of(entradaDeAna()))

        assertThrows(NotYourEntryException::class.java) {
            entryService.delete(1, "beto")
        }
        verify(entryRepository, never()).delete(any<Entry>())
    }

    // ---------- 404 de verdad ----------

    @Test
    fun `una entrada que no existe da 404, no 403`() {
        whenever(entryRepository.findById(999)).thenReturn(Optional.empty())

        assertThrows(EntryNotFoundException::class.java) {
            entryService.findOne(999, "ana")
        }
    }
}
