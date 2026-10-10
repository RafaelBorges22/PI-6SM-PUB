package br.com.smartparking.api.common

import java.time.LocalDateTime

/** Corpo padrao das respostas de erro da API. */
data class ApiError(
    val status: Int,
    val erro: String,
    val mensagem: String,
    val caminho: String,
    val campos: Map<String, String> = emptyMap(),
    val timestamp: LocalDateTime = LocalDateTime.now(),
)
