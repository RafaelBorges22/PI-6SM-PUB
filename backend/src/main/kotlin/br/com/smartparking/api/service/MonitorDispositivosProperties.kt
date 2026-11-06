package br.com.smartparking.api.service

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Configuracao do monitor de dispositivos offline (`smartparking.dispositivos.*`).
 *
 * O ESP32 publica um heartbeat a cada 30 s; com o tempo limite padrao de 90 s o
 * dispositivo so e marcado OFFLINE depois de perder 3 heartbeats seguidos.
 */
@ConfigurationProperties(prefix = "smartparking.dispositivos")
data class MonitorDispositivosProperties(
    /** Liga/desliga a verificacao periodica (desligada nos testes). */
    val monitorOfflineHabilitado: Boolean = true,
    /** Sem mensagem MQTT ha mais que isso, o dispositivo ONLINE passa a OFFLINE. */
    val tempoLimiteOfflineSegundos: Long = 90,
    /** Intervalo entre as verificacoes. */
    val intervaloVerificacaoMs: Long = 30_000,
)
