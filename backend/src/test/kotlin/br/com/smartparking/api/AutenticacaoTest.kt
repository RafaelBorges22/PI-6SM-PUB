package br.com.smartparking.api

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/** Etapa 5 da especificacao: autenticacao e autorizacao com JWT. */
@DisplayName("Autenticacao e autorizacao (JWT)")
class AutenticacaoTest : TesteBase() {

    @Test
    fun `login com credenciais validas devolve token e dados do usuario`() {
        mockMvc.perform(
            post("/api/auth/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(mapOf("email" to EMAIL_ADMIN, "senha" to SENHA))),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.token").isNotEmpty)
            .andExpect(jsonPath("$.type").value("Bearer"))
            .andExpect(jsonPath("$.role").value("ADMIN"))
            .andExpect(jsonPath("$.email").value(EMAIL_ADMIN))
    }

    @Test
    fun `login com senha errada devolve 401`() {
        mockMvc.perform(
            post("/api/auth/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(mapOf("email" to EMAIL_ADMIN, "senha" to "senha-errada"))),
        ).andExpect(status().isUnauthorized)
    }

    @Test
    fun `login com email inexistente devolve 401`() {
        mockMvc.perform(
            post("/api/auth/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(mapOf("email" to "ninguem@smartparking.com", "senha" to SENHA))),
        ).andExpect(status().isUnauthorized)
    }

    @Test
    fun `requisicao sem token devolve 401`() {
        mockMvc.perform(get("/api/blocos")).andExpect(status().isUnauthorized)
    }

    @Test
    fun `token invalido devolve 401`() {
        mockMvc.perform(get("/api/blocos").header("Authorization", "Bearer token.completamente.invalido"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `USER pode consultar mas nao pode criar bloco`() {
        mockMvc.perform(get("/api/blocos").header("Authorization", bearer(tokenUser)))
            .andExpect(status().isOk)

        mockMvc.perform(
            post("/api/blocos")
                .header("Authorization", bearer(tokenUser))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(mapOf("nome" to "Bloco Proibido"))),
        ).andExpect(status().isForbidden)
    }

    @Test
    fun `USER nao acessa o CRUD de usuarios`() {
        mockMvc.perform(get("/api/usuarios").header("Authorization", bearer(tokenUser)))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `ADMIN acessa o CRUD de usuarios`() {
        mockMvc.perform(get("/api/usuarios").header("Authorization", bearer(tokenAdmin)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
    }

    @Test
    fun `USER nao acessa sensores, dashboard nem leituras`() {
        listOf("/api/dispositivos", "/api/dashboard", "/api/medicoes").forEach { rota ->
            mockMvc.perform(get(rota).header("Authorization", bearer(tokenUser)))
                .andExpect(status().isForbidden)
        }
    }

    @Test
    fun `ADMIN acessa sensores, dashboard e leituras`() {
        listOf("/api/dispositivos", "/api/dashboard", "/api/medicoes").forEach { rota ->
            mockMvc.perform(get(rota).header("Authorization", bearer(tokenAdmin)))
                .andExpect(status().isOk)
        }
    }

    @Test
    fun `me devolve o usuario dono do token`() {
        mockMvc.perform(get("/api/auth/me").header("Authorization", bearer(tokenUser)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.email").value(EMAIL_USER))
            .andExpect(jsonPath("$.role").value("USER"))
    }
}
