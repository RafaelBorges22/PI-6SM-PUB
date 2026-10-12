package br.com.smartparking.api.mqtt

import jakarta.annotation.PreDestroy
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets

/**
 * Cliente MQTT do backend (secao 36 da especificacao).
 *
 * Conecta ao broker Mosquitto, assina `smart-parking/vaga/+` e repassa cada
 * mensagem ao [LeituraSensorService]. A reconexao automatica do Paho reassina
 * o topico no callback `connectComplete`.
 *
 * Quando `smartparking.mqtt.habilitado=false` o componente fica inerte, o que
 * permite subir a API (e rodar os testes) sem broker disponivel.
 */
@Component
class MqttSubscriber(
    private val properties: MqttProperties,
    private val leituraSensorService: LeituraSensorService,
) : MqttCallbackExtended {

    private val log = LoggerFactory.getLogger(javaClass)

    @Volatile
    private var client: MqttClient? = null

    @EventListener(ApplicationReadyEvent::class)
    fun conectar() {
        if (!properties.habilitado) {
            log.info("Cliente MQTT desabilitado (smartparking.mqtt.habilitado=false)")
            return
        }
        try {
            val mqttClient = MqttClient(properties.broker, properties.clientId, MemoryPersistence())
            mqttClient.setCallback(this)
            // O campo precisa estar preenchido ANTES do connect: o callback
            // connectComplete roda na thread do Paho e e quem assina o topico.
            client = mqttClient
            mqttClient.connect(opcoesDeConexao())
            log.info("Conectado ao broker MQTT {} como {}", properties.broker, properties.clientId)
        } catch (e: MqttException) {
            // Nao derruba a aplicacao: a API REST continua util e o Paho tenta reconectar.
            log.error(
                "Falha ao conectar no broker MQTT {}: {}. A API continua no ar; verifique o Mosquitto.",
                properties.broker,
                e.message,
            )
        }
    }

    private fun opcoesDeConexao() = MqttConnectOptions().apply {
        isCleanSession = true
        isAutomaticReconnect = true
        connectionTimeout = properties.timeoutConexaoSegundos
        keepAliveInterval = properties.keepAliveSegundos
        properties.usuario?.takeIf { it.isNotBlank() }?.let { userName = it }
        properties.senha?.takeIf { it.isNotBlank() }?.let { password = it.toCharArray() }
    }

    override fun connectComplete(reconexao: Boolean, serverURI: String) {
        val mqttClient = client
        if (mqttClient == null) {
            log.error("Conexao concluida sem cliente inicializado; nenhuma assinatura foi feita")
            return
        }
        try {
            mqttClient.subscribe(properties.topico, properties.qos)
            log.info(
                "Inscrito no topico {} (qos={}, reconexao={})",
                properties.topico,
                properties.qos,
                reconexao,
            )
        } catch (e: MqttException) {
            log.error("Falha ao assinar o topico {}: {}", properties.topico, e.message)
        }
    }

    override fun messageArrived(topic: String, message: MqttMessage) {
        val conteudo = String(message.payload, StandardCharsets.UTF_8)
        try {
            leituraSensorService.processar(topic, conteudo)
        } catch (e: Exception) {
            // Uma mensagem problematica nao pode derrubar a thread do cliente MQTT.
            log.error("Erro ao processar mensagem do topico {}: {}", topic, e.message, e)
        }
    }

    override fun connectionLost(cause: Throwable?) {
        log.warn("Conexao com o broker MQTT perdida: {}. Tentando reconectar...", cause?.message)
    }

    override fun deliveryComplete(token: IMqttDeliveryToken?) = Unit

    @PreDestroy
    fun desconectar() {
        try {
            client?.takeIf { it.isConnected }?.disconnect()
            client?.close()
        } catch (e: MqttException) {
            log.debug("Erro ao encerrar o cliente MQTT: {}", e.message)
        }
    }
}
