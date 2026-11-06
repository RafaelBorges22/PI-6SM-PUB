package br.com.smartparking.api.service

import br.com.smartparking.api.domain.Dispositivo
import br.com.smartparking.api.domain.StatusDispositivo
import br.com.smartparking.api.repository.DispositivoRepository
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.time.LocalDateTime
import kotlin.test.assertEquals

/**
 * Teste de UNIDADE de [MonitorDispositivosService.marcarOfflineInativos].
 *
 *  - MOCK: DispositivoRepository - devolve os dispositivos inativos e verifica a gravacao.
 *  - DRIVER: o proprio teste, no papel do agendador, chama o metodo com um "agora" fixo.
 */
@DisplayName("MonitorDispositivosService (unidade, com mocks)")
class MonitorDispositivosServiceTest {

    private val repository: DispositivoRepository = mock(DispositivoRepository::class.java)
    private val service = MonitorDispositivosService(
        repository,
        MonitorDispositivosProperties(tempoLimiteOfflineSegundos = 90),
    )

    private val agora = LocalDateTime.of(2026, 11, 6, 14, 0, 0)
    private val limiteEsperado = agora.minusSeconds(90)

    private fun dispositivo(nome: String) = Dispositivo(
        nome = nome,
        macAddress = "AA:BB:CC:DD:EE:01",
        mqttTopic = "smart-parking/vaga/01",
        status = StatusDispositivo.ONLINE,
        ultimaComunicacao = agora.minusMinutes(5),
    )

    @Test
    fun `consulta o repositorio com agora menos o tempo limite`() {
        `when`(repository.buscarOnlineSemComunicacaoDesde(limiteEsperado)).thenReturn(emptyList())

        service.marcarOfflineInativos(agora)

        verify(repository).buscarOnlineSemComunicacaoDesde(limiteEsperado)
    }

    @Test
    fun `marca OFFLINE e grava os dispositivos inativos`() {
        val inativos = listOf(dispositivo("ESP32-VAGA-01"), dispositivo("ESP32-VAGA-02"))
        `when`(repository.buscarOnlineSemComunicacaoDesde(limiteEsperado)).thenReturn(inativos)

        val alterados = service.marcarOfflineInativos(agora)

        assertEquals(2, alterados)
        inativos.forEach { assertEquals(StatusDispositivo.OFFLINE, it.status) }
        verify(repository).saveAll(inativos)
    }

    @Test
    fun `sem dispositivos inativos nada e gravado`() {
        `when`(repository.buscarOnlineSemComunicacaoDesde(limiteEsperado)).thenReturn(emptyList())

        val alterados = service.marcarOfflineInativos(agora)

        assertEquals(0, alterados)
        verify(repository, never()).saveAll(emptyList<Dispositivo>())
    }
}
