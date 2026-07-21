package com.pucetec.diary.controllers

import com.pucetec.diary.dto.EntryRequest
import com.pucetec.diary.dto.EntryResponse
import com.pucetec.diary.services.EntryService
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/**
 * CHECKPOINT 3 — el `?author=` desapareció de todos los endpoints.
 *
 * `GET /entries` ya no recibe NINGÚN parámetro (ni query, ni path, ni body) y aun así
 * sabe perfectamente qué entradas devolver. Ese endpoint es imposible de abusar: no hay
 * nada que manipular, no existe un lugar donde escribir el nombre de otro.
 *
 * La mejor forma de prevenir un bug es borrar el lugar donde el bug podía existir.
 */
@RestController
@RequestMapping("/entries")
class EntryController(private val entryService: EntryService) {

    @GetMapping
    fun mine(@AuthenticationPrincipal jwt: Jwt): List<EntryResponse> =
        entryService.findMine(jwt.username())

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @RequestBody request: EntryRequest,
        @AuthenticationPrincipal jwt: Jwt
    ): EntryResponse =
        entryService.create(request, jwt.username())

    @GetMapping("/{id}")
    fun one(
        @PathVariable id: Long,
        @AuthenticationPrincipal jwt: Jwt
    ): EntryResponse =
        entryService.findOne(id, jwt.username())

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: Long,
        @RequestBody request: EntryRequest,
        @AuthenticationPrincipal jwt: Jwt
    ): EntryResponse =
        entryService.update(id, request, jwt.username())

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(
        @PathVariable id: Long,
        @AuthenticationPrincipal jwt: Jwt
    ) = entryService.delete(id, jwt.username())

    /**
     * LA LÍNEA. Aquí se decide quién eres, en toda la aplicación.
     *
     * El JWT ya viene verificado por Spring Security (firma, emisor, expiración).
     * `username` es un claim del access_token de Cognito, y no se puede falsificar
     * sin la llave privada de AWS.
     */
    private fun Jwt.username(): String = getClaimAsString("username")
}
