package br.com.smartparking.api

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Etapa 4 da especificacao: CRUD 3 (Dispositivos) e CRUD 4 (Usuarios). */
@DisplayName("CRUD de dispositivos e usuarios")
class CrudDispositivoUsuarioTest : TesteBase() {

    private fun criarDispositivo(
        nome: String = "ESP32-VAGA-01",
        mac: String = "AA:BB:CC:DD:EE:01",
        topico: String = "smart-parking/vaga/01",
        vagaId: Long? = null,
    ): Long {
        val corpo = mutableMapOf<String, Any>(
            "nome" to nome,
            "macAddress" to mac,
            "tipo" to "ESP32",
            "mqttTopic" to topico,
            "status" to "ONLINE",
        )
        vagaId?.let { corpo["vagaId"] = it }

        val resposta = mockMvc.perform(
            post("/api/dispositivos")
                .header("Authorization", bearer(tokenAdmin))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(corpo)),
        ).andExpect(status().isCreated).andReturn().response.contentAsString
        return objectMapper.readTree(resposta).get("id").asLong()
    }

    private fun criarBlocoEVaga(numero: Int = 1): Long {
        val bloco = mockMvc.perform(
            post("/api/blocos")
                .header("Authorization", bearer(tokenAdmin))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(mapOf("nome" to "Bloco $numero"))),
        ).andReturn().response.contentAsString
        val blocoId = objectMapper.readTree(bloco).get("id").asLong()

        val vaga = mockMvc.perform(
            post("/api/vagas")
                .header("Authorization", bearer(tokenAdmin))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(mapOf("numero" to numero, "blocoId" to blocoId))),
        ).andReturn().response.contentAsString
        return objectMapper.readTree(vaga).get("id").asLong()
    }

    @Test
    fun `ciclo completo do CRUD de dispositivos`() {
        val id = criarDispositivo()

        mockMvc.perform(get("/api/dispositivos").header("Authorization", bearer(tokenAdmin)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))

        mockMvc.perform(get("/api/dispositivos/$id").header("Authorization", bearer(tokenAdmin)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.mqttTopic").value("smart-parking/vaga/01"))
            .andExpect(jsonPath("$.macAddress").value("AA:BB:CC:DD:EE:01"))

        mockMvc.perform(
            put("/api/dispositivos/$id")
                .header("Authorization", bearer(tokenAdmin))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    json(
                        mapOf(
                            "nome" to "ESP32-VAGA-01",
                            "macAddress" to "AA:BB:CC:DD:EE:01",
                            "tipo" to "ESP32",
                            "mqttTopic" to "smart-parking/vaga/99",
                            "status" to "OFFLINE",
                        ),
                    ),
                ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.mqttTopic").value("smart-parking/vaga/99"))
            .andExpect(jsonPath("$.status").value("OFFLINE"))

        mockMvc.perform(delete("/api/dispositivos/$id").header("Authorization", bearer(tokenAdmin)))
            .andExpect(status().isNoContent)
        mockMvc.perform(get("/api/dispositivos/$id").header("Authorization", bearer(tokenAdmin)))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `MAC address em formato invalido devolve 400`() {
        mockMvc.perform(
            post("/api/dispositivos")
                .header("Authorization", bearer(tokenAdmin))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    json(
                        mapOf(
                            "nome" to "ESP32-X",
                            "macAddress" to "mac-invalido",
                            "mqttTopic" to "smart-parking/vaga/50",
                        ),
                    ),
                ),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.campos.macAddress").exists())
    }

    @Test
    fun `topico MQTT duplicado devolve 409`() {
        criarDispositivo()
        mockMvc.perform(
            post("/api/dispositivos")
                .header("Authorization", bearer(tokenAdmin))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    json(
                        mapOf(
                            "nome" to "ESP32-VAGA-02",
                            "macAddress" to "AA:BB:CC:DD:EE:02",
                            "mqttTopic" to "smart-parking/vaga/01",
                        ),
                    ),
                ),
        ).andExpect(status().isConflict)
    }

    @Test
    fun `vincular dispositivo a vaga preenche os dois lados do relacionamento`() {
        val vagaId = criarBlocoEVaga(1)
        val dispositivoId = criarDispositivo(vagaId = vagaId)

        mockMvc.perform(get("/api/dispositivos/$dispositivoId").header("Authorization", bearer(tokenAdmin)))
            .andExpect(jsonPath("$.vagaId").value(vagaId))
            .andExpect(jsonPath("$.vagaNumero").value(1))

        mockMvc.perform(get("/api/vagas/$vagaId").header("Authorization", bearer(tokenAdmin)))
            .andExpect(jsonPath("$.dispositivoId").value(dispositivoId))
    }

    @Test
    fun `excluir dispositivo desvincula a vaga em vez de falhar`() {
        val vagaId = criarBlocoEVaga(1)
        val dispositivoId = criarDispositivo(vagaId = vagaId)

        mockMvc.perform(delete("/api/dispositivos/$dispositivoId").header("Authorization", bearer(tokenAdmin)))
            .andExpect(status().isNoContent)

        assertNull(vagaRepository.findById(vagaId).orElseThrow().dispositivo)
    }

    @Test
    fun `dispositivo nao pode ser vinculado a duas vagas`() {
        val vaga1 = criarBlocoEVaga(1)
        val vaga2 = criarBlocoEVaga(2)
        criarDispositivo(vagaId = vaga1)
        val outro = criarDispositivo(
            nome = "ESP32-VAGA-02",
            mac = "AA:BB:CC:DD:EE:02",
            topico = "smart-parking/vaga/02",
            vagaId = vaga2,
        )

        mockMvc.perform(
            put("/api/dispositivos/$outro")
                .header("Authorization", bearer(tokenAdmin))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    json(
                        mapOf(
                            "nome" to "ESP32-VAGA-02",
                            "macAddress" to "AA:BB:CC:DD:EE:02",
                            "mqttTopic" to "smart-parking/vaga/02",
                            "vagaId" to vaga1,
                        ),
                    ),
                ),
        ).andExpect(status().isConflict)
    }

    @Test
    fun `ciclo completo do CRUD de usuarios`() {
        val resposta = mockMvc.perform(
            post("/api/usuarios")
                .header("Authorization", bearer(tokenAdmin))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    json(
                        mapOf(
                            "nome" to "Maria",
                            "email" to "maria@smartparking.com",
                            "senha" to "senha123",
                            "role" to "USER",
                            "ativo" to true,
                        ),
                    ),
                ),
        ).andExpect(status().isCreated).andReturn().response.contentAsString
        val id = objectMapper.readTree(resposta).get("id").asLong()

        // A senha nunca pode aparecer na resposta da API.
        assertEquals(false, resposta.contains("senha"))

        mockMvc.perform(get("/api/usuarios/$id").header("Authorization", bearer(tokenAdmin)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.email").value("maria@smartparking.com"))

        mockMvc.perform(
            put("/api/usuarios/$id")
                .header("Authorization", bearer(tokenAdmin))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    json(
                        mapOf(
                            "nome" to "Maria Silva",
                            "email" to "maria@smartparking.com",
                            "role" to "ADMIN",
                            "ativo" to true,
                        ),
                    ),
                ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.nome").value("Maria Silva"))
            .andExpect(jsonPath("$.role").value("ADMIN"))

        // Senha em branco no update mantem a senha original.
        assertEquals("maria@smartparking.com", objectMapper.readTree(
            mockMvc.perform(
                post("/api/auth/signin")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json(mapOf("email" to "maria@smartparking.com", "senha" to "senha123"))),
            ).andExpect(status().isOk).andReturn().response.contentAsString,
        ).get("email").asText())

        mockMvc.perform(delete("/api/usuarios/$id").header("Authorization", bearer(tokenAdmin)))
            .andExpect(status().isNoContent)
    }

    @Test
    fun `email de usuario duplicado devolve 409 e email invalido devolve 400`() {
        mockMvc.perform(
            post("/api/usuarios")
                .header("Authorization", bearer(tokenAdmin))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(mapOf("nome" to "Outro Admin", "email" to EMAIL_ADMIN, "senha" to "senha123"))),
        ).andExpect(status().isConflict)

        mockMvc.perform(
            post("/api/usuarios")
                .header("Authorization", bearer(tokenAdmin))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(mapOf("nome" to "Joao", "email" to "nao-e-email", "senha" to "senha123"))),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.campos.email").exists())
    }

    @Test
    fun `nao permite remover o ultimo administrador ativo`() {
        val adminId = usuarioRepository.findByEmailIgnoreCase(EMAIL_ADMIN)!!.id!!

        mockMvc.perform(delete("/api/usuarios/$adminId").header("Authorization", bearer(tokenAdmin)))
            .andExpect(status().isConflict)
    }
}
