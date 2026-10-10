package br.com.smartparking.app.model

import kotlinx.serialization.Serializable

/** Modelos de Bloco, Vaga e Dashboard (espelho dos DTOs da API REST). */

@Serializable
data class Bloco(
    val id: Long = 0,
    val nome: String = "",
    val descricao: String? = null,
    val localizacao: String? = null,
    val status: String = "ATIVO",
    val capacidade: Int = 0,
    val coberto: Boolean = false,
    val totalVagas: Int = 0,
)

@Serializable
data class BlocoRequest(
    val nome: String,
    val descricao: String? = null,
    val localizacao: String? = null,
    val status: String = "ATIVO",
    val capacidade: Int = 0,
    val coberto: Boolean = false,
)

@Serializable
data class Vaga(
    val id: Long = 0,
    val numero: Int = 0,
    val tipo: String = "NORMAL",
    val status: String = "LIVRE",
    val blocoId: Long = 0,
    val blocoNome: String = "",
    val dispositivoId: Long? = null,
    val dispositivoNome: String? = null,
    val valorHora: Double = 0.0,
    val observacao: String? = null,
)

@Serializable
data class VagaRequest(
    val numero: Int,
    val tipo: String = "NORMAL",
    val status: String = "LIVRE",
    val blocoId: Long,
    val dispositivoId: Long? = null,
    val valorHora: Double = 0.0,
    val observacao: String? = null,
)

@Serializable
data class VagaResumo(
    val id: Long = 0,
    val numero: Int = 0,
    val tipo: String = "NORMAL",
    val status: String = "LIVRE",
    val dispositivoId: Long? = null,
)

@Serializable
data class BlocoComVagas(
    val id: Long = 0,
    val nome: String = "",
    val descricao: String? = null,
    val localizacao: String? = null,
    val status: String = "ATIVO",
    val vagas: List<VagaResumo> = emptyList(),
    val livres: Int = 0,
    val ocupadas: Int = 0,
    val manutencao: Int = 0,
)

@Serializable
data class BlocoOcupacao(
    val blocoId: Long = 0,
    val nome: String = "",
    val totalVagas: Int = 0,
    val livres: Int = 0,
    val ocupadas: Int = 0,
    val manutencao: Int = 0,
)

@Serializable
data class Dashboard(
    val totalBlocos: Long = 0,
    val totalVagas: Long = 0,
    val livres: Long = 0,
    val ocupadas: Long = 0,
    val manutencao: Long = 0,
    val taxaOcupacaoPercentual: Int = 0,
    val dispositivosOnline: Long = 0,
    val dispositivosOffline: Long = 0,
    val blocos: List<BlocoOcupacao> = emptyList(),
)

/** Corpo de erro devolvido pela API (ApiError no backend). */
@Serializable
data class ApiError(
    val status: Int = 0,
    val erro: String = "",
    val mensagem: String = "",
    val caminho: String = "",
    val campos: Map<String, String> = emptyMap(),
)

/** Excecao com a mensagem ja pronta para exibir na interface. */
class ApiException(val codigo: Int, mensagem: String) : Exception(mensagem)
