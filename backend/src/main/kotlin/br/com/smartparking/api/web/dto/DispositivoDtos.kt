package br.com.smartparking.api.web.dto

import br.com.smartparking.api.domain.StatusDispositivo
import br.com.smartparking.api.domain.TipoDispositivo
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.PastOrPresent
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.time.LocalDate
import java.time.LocalDateTime

data class DispositivoRequest(
    @field:NotBlank(message = "nome e obrigatorio")
    @field:Size(max = 100, message = "nome deve ter no maximo 100 caracteres")
    val nome: String,

    @field:NotBlank(message = "macAddress e obrigatorio")
    @field:Pattern(
        regexp = "^([0-9A-Fa-f]{2}:){5}[0-9A-Fa-f]{2}$",
        message = "macAddress deve estar no formato AA:BB:CC:DD:EE:FF",
    )
    val macAddress: String,

    val tipo: TipoDispositivo = TipoDispositivo.ESP32,

    @field:NotBlank(message = "mqttTopic e obrigatorio")
    @field:Size(max = 150, message = "mqttTopic deve ter no maximo 150 caracteres")
    val mqttTopic: String,

    val status: StatusDispositivo = StatusDispositivo.OFFLINE,

    @field:PastOrPresent(message = "dataInstalacao nao pode estar no futuro")
    val dataInstalacao: LocalDate? = null,

    /** Vaga monitorada por este dispositivo; nulo desvincula. */
    val vagaId: Long? = null,
)

data class DispositivoResponse(
    val id: Long,
    val nome: String,
    val macAddress: String,
    val tipo: TipoDispositivo,
    val mqttTopic: String,
    val status: StatusDispositivo,
    val dataInstalacao: LocalDate?,
    val ultimaComunicacao: LocalDateTime?,
    val vagaId: Long?,
    val vagaNumero: Int?,
)
