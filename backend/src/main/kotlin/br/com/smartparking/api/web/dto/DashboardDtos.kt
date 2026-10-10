package br.com.smartparking.api.web.dto

import br.com.smartparking.api.domain.StatusVaga
import java.math.BigDecimal
import java.time.LocalDateTime

/** Resumo exibido no dashboard de Web/Mobile/Desktop (secao 25 da especificacao). */
data class DashboardResponse(
    val totalBlocos: Long,
    val totalVagas: Long,
    val livres: Long,
    val ocupadas: Long,
    val manutencao: Long,
    val taxaOcupacaoPercentual: Int,
    val dispositivosOnline: Long,
    val dispositivosOffline: Long,
    val blocos: List<BlocoOcupacaoResponse>,
)

data class BlocoOcupacaoResponse(
    val blocoId: Long,
    val nome: String,
    val totalVagas: Int,
    val livres: Int,
    val ocupadas: Int,
    val manutencao: Int,
)

data class MedicaoResponse(
    val id: Long,
    val dispositivoId: Long,
    val dispositivoNome: String,
    val vagaId: Long?,
    val vagaNumero: Int?,
    val distancia: BigDecimal,
    val status: StatusVaga,
    val dataHora: LocalDateTime,
)
