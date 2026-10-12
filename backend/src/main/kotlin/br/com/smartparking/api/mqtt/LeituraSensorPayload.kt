package br.com.smartparking.api.mqtt

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Payload publicado pelo ESP32 (secao 13 da especificacao):
 *
 * ```json
 * {"deviceId":"ESP32-VAGA-01","vagaId":1,"distancia":80.5,"status":"OCUPADA","timestamp":"2026-08-14T14:30:00"}
 * ```
 *
 * O `timestamp` do dispositivo e opcional: o ESP32 nem sempre tem relogio
 * sincronizado, entao o backend usa a propria hora quando ele vier ausente.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class LeituraSensorPayload(
    val deviceId: String? = null,
    val vagaId: Long? = null,
    val distancia: BigDecimal? = null,
    val status: String? = null,
    val timestamp: LocalDateTime? = null,
)
