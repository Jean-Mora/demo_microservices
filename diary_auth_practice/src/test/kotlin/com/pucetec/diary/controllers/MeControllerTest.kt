package com.pucetec.diary.controllers

import com.pucetec.diary.config.SecurityConfig
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(MeController::class)
@Import(SecurityConfig::class)
class MeControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Test
    fun `sin token, me devuelve 401`() {
        mockMvc.perform(get("/me"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `me devuelve lo que trae el access_token — y el email viene NULL`() {
        // Un access_token de Cognito, tal cual: trae sub y username… pero NO trae email.
        val accessToken = jwt().jwt {
            it.subject("a1b2c3d4-5e6f-7890-abcd-ef1234567890")
                .claim("username", "ana")
                .claim("token_use", "access")
        }

        mockMvc.perform(get("/me").with(accessToken))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.username").value("ana"))
            .andExpect(jsonPath("$.sub").value("a1b2c3d4-5e6f-7890-abcd-ef1234567890"))
            .andExpect(jsonPath("$.email").doesNotExist())   // 👀 el email NO está en el access_token
            .andExpect(jsonPath("$.groups").isEmpty)         // y tampoco hay grupos: no hay roles
    }
}
