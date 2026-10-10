package br.com.smartparking.api.web.dto

import br.com.smartparking.api.domain.StatusVaga
import br.com.smartparking.api.domain.TipoVaga
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

data class VagaRequest(
    @field:NotNull(message = "numero e obrigatorio")
    @field:Min(value = 1, message = "numero deve ser maior que zero")
    val numero: Int,

    val tipo: TipoVaga = TipoVaga.NORMAL,

    val status: StatusVaga = StatusVaga.LIVRE,

    @field:NotNull(message = "blocoId e obrigatorio")
    val blocoId: Long,

    val dispositivoId: Long? = null,

    @field:DecimalMin(value = "0.0", message = "valorHora nao pode ser negativo")
    val valorHora: Double = 0.0,

    @field:Size(max = 255, message = "observacao deve ter no maximo 255 caracteres")
    val observacao: String? = null,
)

data class VagaResponse(
    val id: Long,
    val numero: Int,
    val tipo: TipoVaga,
    val status: StatusVaga,
    val valorHora: Double,
    val observacao: String?,
    val blocoId: Long,
    val blocoNome: String,
    val dispositivoId: Long?,
    val dispositivoNome: String?,
)

/** Versao enxuta usada dentro de `GET /api/blocos/{id}/vagas`. */
data class VagaResumoResponse(
    val id: Long,
    val numero: Int,
    val tipo: TipoVaga,
    val status: StatusVaga,
    val dispositivoId: Long?,
)
