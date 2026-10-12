package br.com.smartparking.api

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/** Itens da prova AV1: /signup, buscas JPQL com LIKE, novos campos e validacoes. */
@DisplayName("Requisitos da avaliacao AV1")
class RequisitosAvaliacaoTest : TesteBase() {

    @Test
    fun `signup publico cria usuario USER que consegue fazer signin`() {
        mockMvc.perform(
            post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(mapOf("nome" to "Novo Aluno", "email" to "novo@fatec.sp.gov.br", "senha" to "segredo1"))),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.role").value("USER"))
            .andExpect(jsonPath("$.dataCadastro").isNotEmpty)

        mockMvc.perform(
            post("/api/auth/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(mapOf("email" to "novo@fatec.sp.gov.br", "senha" to "segredo1"))),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.token").isNotEmpty)
            .andExpect(jsonPath("$.role").value("USER"))
    }

    @Test
    fun `signup com email ja cadastrado devolve 409`() {
        mockMvc.perform(
            post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(mapOf("nome" to "Outro", "email" to EMAIL_ADMIN, "senha" to "segredo1"))),
        ).andExpect(status().isConflict)
    }

    @Test
    fun `signup com email invalido e senha curta devolve 400`() {
        mockMvc.perform(
            post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(mapOf("nome" to "X", "email" to "nao-e-email", "senha" to "123"))),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.campos.email").exists())
            .andExpect(jsonPath("$.campos.senha").exists())
    }

    @Test
    fun `bloco grava e devolve capacidade e coberto e busca por nome parcial`() {
        mockMvc.perform(
            post("/api/blocos")
                .header("Authorization", bearer(tokenAdmin))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(mapOf("nome" to "Garagem Norte", "capacidade" to 40, "coberto" to true))),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.capacidade").value(40))
            .andExpect(jsonPath("$.coberto").value(true))

        mockMvc.perform(get("/api/blocos/buscar").param("nome", "rage").header("Authorization", bearer(tokenUser)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].nome").value("Garagem Norte"))
    }

    @Test
    fun `bloco com capacidade negativa devolve 400`() {
        mockMvc.perform(
            post("/api/blocos")
                .header("Authorization", bearer(tokenAdmin))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(mapOf("nome" to "Bloco Z", "capacidade" to -1))),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.campos.capacidade").exists())
    }

    @Test
    fun `usuarios podem ser buscados por nome parcial`() {
        mockMvc.perform(get("/api/usuarios/buscar").param("nome", "admin").header("Authorization", bearer(tokenAdmin)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].email").value(EMAIL_ADMIN))
    }

    @Test
    fun `dispositivo com data de instalacao no futuro devolve 400`() {
        mockMvc.perform(
            post("/api/dispositivos")
                .header("Authorization", bearer(tokenAdmin))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    json(
                        mapOf(
                            "nome" to "ESP32-FUTURO",
                            "macAddress" to "AA:BB:CC:DD:EE:99",
                            "mqttTopic" to "smart-parking/vaga/99",
                            "dataInstalacao" to "2999-01-01",
                        ),
                    ),
                ),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.campos.dataInstalacao").exists())
    }
}
