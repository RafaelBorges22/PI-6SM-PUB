package br.com.smartparking.api

import br.com.smartparking.api.domain.Bloco
import br.com.smartparking.api.domain.Dispositivo
import br.com.smartparking.api.domain.StatusDispositivo
import br.com.smartparking.api.domain.StatusVaga
import br.com.smartparking.api.domain.Vaga
import br.com.smartparking.api.mqtt.LeituraSensorService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Etapa 7 da especificacao: backend recebendo dados do ESP32 via MQTT.
 *
 * Exercita [LeituraSensorService] com o mesmo payload que o ESP32 publica,
 * sem depender de um broker no ar.
 */
@DisplayName("Consumo das leituras publicadas via MQTT")
class LeituraMqttTest : TesteBase() {

    @Autowired
    private lateinit var leituraSensorService: LeituraSensorService

    private val topico = "smart-parking/vaga/01"

    private var vagaId: Long = 0
    private var dispositivoId: Long = 0

    @BeforeEach
    fun prepararVagaMonitorada() {
        val bloco = blocoRepository.save(Bloco(nome = "Bloco A", localizacao = "Terreo"))
        val dispositivo = dispositivoRepository.save(
            Dispositivo(
                nome = "ESP32-VAGA-01",
                macAddress = "AA:BB:CC:DD:EE:01",
                mqttTopic = topico,
                status = StatusDispositivo.OFFLINE,
            ),
        )
        val vaga = vagaRepository.save(
            Vaga(numero = 1, bloco = bloco, dispositivo = dispositivo, status = StatusVaga.LIVRE),
        )
        vagaId = vaga.id!!
        dispositivoId = dispositivo.id!!
    }

    private fun payload(status: String, distancia: Double) = """
        {
          "deviceId": "ESP32-VAGA-01",
          "vagaId": $vagaId,
          "distancia": $distancia,
          "status": "$status",
          "timestamp": "2026-08-14T14:30:00"
        }
    """.trimIndent()

    @Test
    fun `leitura OCUPADA muda o status da vaga e registra a medicao`() {
        val resultado = leituraSensorService.processar(topico, payload("OCUPADA", 80.5))

        assertTrue(resultado.processado)
        assertTrue(resultado.statusMudou)
        assertEquals(StatusVaga.OCUPADA, vagaRepository.findById(vagaId).orElseThrow().status)
        assertEquals(1, medicaoRepository.count().toInt())

        val medicao = medicaoRepository.findAll().first()
        assertEquals(StatusVaga.OCUPADA, medicao.status)
        assertEquals(0, medicao.distancia.compareTo(java.math.BigDecimal("80.5")))
    }

    @Test
    fun `leitura marca o dispositivo como ONLINE e grava a ultima comunicacao`() {
        leituraSensorService.processar(topico, payload("LIVRE", 250.8))

        val dispositivo = dispositivoRepository.findById(dispositivoId).orElseThrow()
        assertEquals(StatusDispositivo.ONLINE, dispositivo.status)
        assertEquals("2026-08-14T14:30", dispositivo.ultimaComunicacao.toString())
    }

    @Test
    fun `ciclo LIVRE - OCUPADA - LIVRE reflete a demonstracao da secao 42`() {
        assertFalse(leituraSensorService.processar(topico, payload("LIVRE", 250.5)).statusMudou)
        assertTrue(leituraSensorService.processar(topico, payload("OCUPADA", 80.5)).statusMudou)
        assertEquals(StatusVaga.OCUPADA, vagaRepository.findById(vagaId).orElseThrow().status)

        assertTrue(leituraSensorService.processar(topico, payload("LIVRE", 250.5)).statusMudou)
        assertEquals(StatusVaga.LIVRE, vagaRepository.findById(vagaId).orElseThrow().status)
        assertEquals(3, medicaoRepository.count().toInt())
    }

    @Test
    fun `vaga em MANUTENCAO nao e sobrescrita pelo sensor`() {
        val vaga = vagaRepository.findById(vagaId).orElseThrow()
        vaga.status = StatusVaga.MANUTENCAO
        vagaRepository.save(vaga)

        val resultado = leituraSensorService.processar(topico, payload("OCUPADA", 80.5))

        assertTrue(resultado.processado)
        assertFalse(resultado.statusMudou)
        assertEquals(StatusVaga.MANUTENCAO, vagaRepository.findById(vagaId).orElseThrow().status)
        // A medicao continua sendo registrada para nao perder o historico do sensor.
        assertEquals(1, medicaoRepository.count().toInt())
    }

    @Test
    fun `payload malformado e ignorado sem quebrar o consumidor`() {
        val resultado = leituraSensorService.processar(topico, "isso nao e json")

        assertFalse(resultado.processado)
        assertEquals(0, medicaoRepository.count().toInt())
        assertEquals(StatusVaga.LIVRE, vagaRepository.findById(vagaId).orElseThrow().status)
    }

    @Test
    fun `status desconhecido no payload e rejeitado`() {
        val resultado = leituraSensorService.processar(topico, payload("TALVEZ", 100.0))

        assertFalse(resultado.processado)
        assertEquals("status desconhecido", resultado.motivo)
        assertEquals(0, medicaoRepository.count().toInt())
    }

    @Test
    fun `mensagem de topico sem dispositivo cadastrado e descartada`() {
        val resultado = leituraSensorService.processar("smart-parking/vaga/77", """{"distancia":90,"status":"OCUPADA"}""")

        assertFalse(resultado.processado)
        assertEquals("dispositivo nao cadastrado", resultado.motivo)
    }

    @Test
    fun `dispositivo e resolvido pelo deviceId quando o topico nao bate`() {
        val resultado = leituraSensorService.processar(
            "smart-parking/vaga/outro-topico",
            """{"deviceId":"ESP32-VAGA-01","distancia":75.0,"status":"OCUPADA"}""",
        )

        assertTrue(resultado.processado)
        assertEquals(StatusVaga.OCUPADA, vagaRepository.findById(vagaId).orElseThrow().status)
    }

    @Test
    fun `payload sem timestamp usa a hora do servidor`() {
        val resultado = leituraSensorService.processar(
            topico,
            """{"deviceId":"ESP32-VAGA-01","distancia":75.0,"status":"OCUPADA"}""",
        )

        assertTrue(resultado.processado)
        assertEquals(1, medicaoRepository.count().toInt())
    }
}
