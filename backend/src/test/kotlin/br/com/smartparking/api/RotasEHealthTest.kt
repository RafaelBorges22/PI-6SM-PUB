package br.com.smartparking.api

import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * Rotas inexistentes, metodo HTTP errado e health check.
 *
 * Antes, as duas primeiras caiam no handler generico e viravam 500; e
 * `/actuator/health` nao existia (faltava o spring-boot-starter-actuator).
 */
class RotasEHealthTest : TesteBase() {

    @Test
    fun `rota inexistente com token devolve 404 no corpo padrao`() {
        mockMvc.perform(get("/api/rota-que-nao-existe").header("Authorization", "Bearer $tokenAdmin"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.caminho").value("/api/rota-que-nao-existe"))
    }

    @Test
    fun `metodo HTTP nao suportado devolve 405`() {
        mockMvc.perform(delete("/api/auth/signin").header("Authorization", "Bearer $tokenAdmin"))
            .andExpect(status().isMethodNotAllowed)
            .andExpect(jsonPath("$.status").value(405))
    }

    @Test
    fun `health check responde UP sem autenticacao`() {
        mockMvc.perform(get("/actuator/health"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("UP"))
    }
}
