package br.com.smartparking.api.web.dto

import br.com.smartparking.api.domain.StatusBloco
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.PositiveOrZero
import jakarta.validation.constraints.Size

data class BlocoRequest(
    @field:NotBlank(message = "nome e obrigatorio")
    @field:Size(max = 100, message = "nome deve ter no maximo 100 caracteres")
    val nome: String,

    @field:Size(max = 255, message = "descricao deve ter no maximo 255 caracteres")
    val descricao: String? = null,

    @field:Size(max = 150, message = "localizacao deve ter no maximo 150 caracteres")
    val localizacao: String? = null,

    val status: StatusBloco = StatusBloco.ATIVO,

    @field:PositiveOrZero(message = "capacidade nao pode ser negativa")
    @field:Max(value = 10000, message = "capacidade deve ser no maximo 10000")
    val capacidade: Int = 0,

    val coberto: Boolean = false,
)

data class BlocoResponse(
    val id: Long,
    val nome: String,
    val descricao: String?,
    val localizacao: String?,
    val status: StatusBloco,
    val capacidade: Int,
    val coberto: Boolean,
    val totalVagas: Int,
)

/** Resposta de `GET /api/blocos/{id}/vagas` (secao 15 da especificacao). */
data class BlocoComVagasResponse(
    val id: Long,
    val nome: String,
    val descricao: String?,
    val localizacao: String?,
    val status: StatusBloco,
    val vagas: List<VagaResumoResponse>,
    val livres: Int,
    val ocupadas: Int,
    val manutencao: Int,
)
