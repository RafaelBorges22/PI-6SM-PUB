package br.com.smartparking.api.mqtt

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Configuracao do cliente MQTT (`smartparking.mqtt.*` no application.yml).
 *
 * O backend assina [topico] (por padrao `smart-parking/vaga/+`), recebendo as
 * mensagens publicadas por todos os ESP32.
 */
@ConfigurationProperties(prefix = "smartparking.mqtt")
data class MqttProperties(
    val habilitado: Boolean = true,
    val broker: String = "tcp://localhost:1883",
    val clientId: String = "smart-parking-backend",
    val topico: String = "smart-parking/vaga/+",
    val qos: Int = 1,
    val usuario: String? = null,
    val senha: String? = null,
    val timeoutConexaoSegundos: Int = 10,
    val keepAliveSegundos: Int = 60,
)
