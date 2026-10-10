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

/** Etapa 4 da especificacao: CRUD 1 (Blocos) e CRUD 2 (Vagas). */
@DisplayName("CRUD de blocos e vagas")
class CrudBlocoVagaTest : TesteBase() {

    private fun criarBloco(nome: String = "Bloco A"): Long {
        val resposta = mockMvc.perform(
            post("/api/blocos")
                .header("Authorization", bearer(tokenAdmin))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    json(
                        mapOf(
                            "nome" to nome,
                            "descricao" to "Estacionamento Principal",
                            "localizacao" to "Terreo",
                            "status" to "ATIVO",
                        ),
                    ),
                ),
        ).andExpect(status().isCreated).andReturn().response.contentAsString
        return objectMapper.readTree(resposta).get("id").asLong()
    }

    private fun criarVaga(blocoId: Long, numero: Int, tipo: String = "NORMAL"): Long {
        val resposta = mockMvc.perform(
            post("/api/vagas")
                .header("Authorization", bearer(tokenAdmin))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(mapOf("numero" to numero, "tipo" to tipo, "status" to "LIVRE", "blocoId" to blocoId))),
        ).andExpect(status().isCreated).andReturn().response.contentAsString
        return objectMapper.readTree(resposta).get("id").asLong()
    }

    @Test
    fun `ciclo completo do CRUD de blocos`() {
        val id = criarBloco()

        mockMvc.perform(get("/api/blocos").header("Authorization", bearer(tokenAdmin)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))

        mockMvc.perform(get("/api/blocos/$id").header("Authorization", bearer(tokenAdmin)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.nome").value("Bloco A"))
            .andExpect(jsonPath("$.totalVagas").value(0))

        mockMvc.perform(
            put("/api/blocos/$id")
                .header("Authorization", bearer(tokenAdmin))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(mapOf("nome" to "Bloco A1", "localizacao" to "Subsolo", "status" to "INATIVO"))),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.nome").value("Bloco A1"))
            .andExpect(jsonPath("$.status").value("INATIVO"))

        mockMvc.perform(delete("/api/blocos/$id").header("Authorization", bearer(tokenAdmin)))
            .andExpect(status().isNoContent)

        mockMvc.perform(get("/api/blocos/$id").header("Authorization", bearer(tokenAdmin)))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `nome de bloco duplicado devolve 409`() {
        criarBloco("Bloco A")
        mockMvc.perform(
            post("/api/blocos")
                .header("Authorization", bearer(tokenAdmin))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(mapOf("nome" to "bloco a"))),
        ).andExpect(status().isConflict)
    }

    @Test
    fun `bloco sem nome devolve 400 com o campo detalhado`() {
        mockMvc.perform(
            post("/api/blocos")
                .header("Authorization", bearer(tokenAdmin))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(mapOf("nome" to ""))),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.campos.nome").exists())
    }

    @Test
    fun `nao exclui bloco que ainda possui vagas`() {
        val blocoId = criarBloco()
        criarVaga(blocoId, 1)

        mockMvc.perform(delete("/api/blocos/$blocoId").header("Authorization", bearer(tokenAdmin)))
            .andExpect(status().isConflict)
    }

    @Test
    fun `ciclo completo do CRUD de vagas`() {
        val blocoId = criarBloco()
        val vagaId = criarVaga(blocoId, 1)

        mockMvc.perform(get("/api/vagas/$vagaId").header("Authorization", bearer(tokenUser)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.numero").value(1))
            .andExpect(jsonPath("$.blocoNome").value("Bloco A"))

        mockMvc.perform(
            put("/api/vagas/$vagaId")
                .header("Authorization", bearer(tokenAdmin))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    json(
                        mapOf(
                            "numero" to 1,
                            "tipo" to "PCD",
                            "status" to "MANUTENCAO",
                            "blocoId" to blocoId,
                        ),
                    ),
                ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.tipo").value("PCD"))
            .andExpect(jsonPath("$.status").value("MANUTENCAO"))

        mockMvc.perform(delete("/api/vagas/$vagaId").header("Authorization", bearer(tokenAdmin)))
            .andExpect(status().isNoContent)
    }

    @Test
    fun `filtros de vagas por status tipo e bloco`() {
        val blocoA = criarBloco("Bloco A")
        val blocoB = criarBloco("Bloco B")
        criarVaga(blocoA, 1, "NORMAL")
        criarVaga(blocoA, 2, "PCD")
        criarVaga(blocoB, 3, "MOTO")

        mockMvc.perform(get("/api/vagas?blocoId=$blocoA").header("Authorization", bearer(tokenUser)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))

        mockMvc.perform(get("/api/vagas?tipo=PCD").header("Authorization", bearer(tokenUser)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].numero").value(2))

        mockMvc.perform(get("/api/vagas?status=LIVRE").header("Authorization", bearer(tokenUser)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(3))
    }

    @Test
    fun `vaga com numero duplicado devolve 409 e com bloco inexistente devolve 404`() {
        val blocoId = criarBloco()
        criarVaga(blocoId, 7)

        mockMvc.perform(
            post("/api/vagas")
                .header("Authorization", bearer(tokenAdmin))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(mapOf("numero" to 7, "blocoId" to blocoId))),
        ).andExpect(status().isConflict)

        mockMvc.perform(
            post("/api/vagas")
                .header("Authorization", bearer(tokenAdmin))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(mapOf("numero" to 8, "blocoId" to 9999))),
        ).andExpect(status().isNotFound)
    }

    @Test
    fun `endpoint de vagas do bloco traz o resumo de ocupacao`() {
        val blocoId = criarBloco()
        criarVaga(blocoId, 1)
        val vagaOcupada = criarVaga(blocoId, 2)

        mockMvc.perform(
            put("/api/vagas/$vagaOcupada")
                .header("Authorization", bearer(tokenAdmin))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(mapOf("numero" to 2, "status" to "OCUPADA", "blocoId" to blocoId))),
        ).andExpect(status().isOk)

        mockMvc.perform(get("/api/blocos/$blocoId/vagas").header("Authorization", bearer(tokenUser)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.vagas.length()").value(2))
            .andExpect(jsonPath("$.livres").value(1))
            .andExpect(jsonPath("$.ocupadas").value(1))
    }
}
