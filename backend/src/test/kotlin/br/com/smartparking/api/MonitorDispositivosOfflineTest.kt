package br.com.smartparking.api

import br.com.smartparking.api.domain.Dispositivo
import br.com.smartparking.api.domain.StatusDispositivo
import br.com.smartparking.api.mqtt.LeituraSensorService
import br.com.smartparking.api.service.MonitorDispositivosService
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime
import kotlin.test.assertEquals

/**
 * Monitor de dispositivos offline sobre o banco real (H2): valida a consulta JPQL e a
 * volta para ONLINE quando o dispositivo publica de novo. Tempo limite do perfil de
 * teste: 90 s.
 */
@DisplayName("Monitor de dispositivos offline (integracao)")
class MonitorDispositivosOfflineTest : TesteBase() {

    @Autowired private lateinit var monitor: MonitorDispositivosService
    @Autowired private lateinit var leituraSensorService: LeituraSensorService

    private val agora = LocalDateTime.of(2026, 11, 6, 14, 0, 0)

    private fun salvar(
        n: Int,
        status: StatusDispositivo,
        ultimaComunicacao: LocalDateTime?,
    ): Long = dispositivoRepository.save(
        Dispositivo(
            nome = "ESP32-VAGA-0$n",
            macAddress = "AA:BB:CC:DD:EE:0$n",
            mqttTopic = "smart-parking/vaga/0$n",
            status = status,
            ultimaComunicacao = ultimaComunicacao,
        ),
    ).id!!

    private fun statusDe(id: Long) = dispositivoRepository.findById(id).orElseThrow().status

    @Test
    fun `marca OFFLINE apenas os ONLINE sem comunicacao alem do tempo limite`() {
        val inativo = salvar(1, StatusDispositivo.ONLINE, agora.minusSeconds(91))
        val recente = salvar(2, StatusDispositivo.ONLINE, agora.minusSeconds(30))
        val nuncaComunicou = salvar(3, StatusDispositivo.ONLINE, null)
        val manutencao = salvar(4, StatusDispositivo.MANUTENCAO, agora.minusHours(2))
        val jaOffline = salvar(5, StatusDispositivo.OFFLINE, agora.minusHours(2))

        val alterados = monitor.marcarOfflineInativos(agora)

        assertEquals(2, alterados)
        assertEquals(StatusDispositivo.OFFLINE, statusDe(inativo))
        assertEquals(StatusDispositivo.ONLINE, statusDe(recente))
        assertEquals(StatusDispositivo.OFFLINE, statusDe(nuncaComunicou))
        assertEquals(StatusDispositivo.MANUTENCAO, statusDe(manutencao))
        assertEquals(StatusDispositivo.OFFLINE, statusDe(jaOffline))
    }

    @Test
    fun `dispositivo OFFLINE volta a ONLINE quando publica de novo`() {
        val id = salvar(1, StatusDispositivo.ONLINE, LocalDateTime.now().minusMinutes(10))
        monitor.marcarOfflineInativos()
        assertEquals(StatusDispositivo.OFFLINE, statusDe(id))

        leituraSensorService.processar(
            "smart-parking/vaga/01",
            """{"deviceId":"ESP32-VAGA-01","distancia":80.5,"status":"OCUPADA"}""",
        )

        assertEquals(StatusDispositivo.ONLINE, statusDe(id))
        assertEquals(0, monitor.marcarOfflineInativos())
    }
}
