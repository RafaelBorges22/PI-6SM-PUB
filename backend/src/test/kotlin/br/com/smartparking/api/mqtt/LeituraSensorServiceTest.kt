package br.com.smartparking.api.mqtt

import br.com.smartparking.api.domain.Bloco
import br.com.smartparking.api.domain.Dispositivo
import br.com.smartparking.api.domain.Medicao
import br.com.smartparking.api.domain.StatusDispositivo
import br.com.smartparking.api.domain.StatusVaga
import br.com.smartparking.api.domain.Vaga
import br.com.smartparking.api.repository.DispositivoRepository
import br.com.smartparking.api.repository.MedicaoRepository
import br.com.smartparking.api.repository.VagaRepository
import br.com.smartparking.api.service.VagaService
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Teste de UNIDADE de [LeituraSensorService.processar] (UC01 - Monitorar Ocupacao da Vaga via Sensor).
 *
 * A classe e testada isolada do Spring, do banco e do broker:
 *  - MOCKS (Mockito): DispositivoRepository, VagaRepository, MedicaoRepository e VagaService.
 *    Substituem as dependencias reais; os testes programam o retorno delas (stubbing) e
 *    verificam as chamadas recebidas (verify).
 *  - STUB: os `save` dos repositorios sao programados para devolver a propria entidade
 *    recebida, simulando a persistencia sem banco.
 *  - DRIVER: cada teste faz o papel do MqttSubscriber, chamando `processar(topico, json)`
 *    diretamente com o payload que o ESP32 publicaria.
 *  - O ObjectMapper e o real (jackson-module-kotlin + JavaTimeModule), pois a conversao do
 *    JSON faz parte da regra testada (fluxo de excecao E1).
 *
 * Cada teste indica o caminho independente do GFC (C1..C11 do Quadro 2a) e as regras de negocio.
 */
@DisplayName("LeituraSensorService (unidade, com mocks)")
class LeituraSensorServiceTest {

    // --- MOCKS das dependencias da classe sob teste ---
    private val dispositivoRepository: DispositivoRepository = mock(DispositivoRepository::class.java)
    private val vagaRepository: VagaRepository = mock(VagaRepository::class.java)
    private val medicaoRepository: MedicaoRepository = mock(MedicaoRepository::class.java)
    private val vagaService: VagaService = mock(VagaService::class.java)

    private val service = LeituraSensorService(
        dispositivoRepository,
        vagaRepository,
        medicaoRepository,
        vagaService,
        jacksonObjectMapper().findAndRegisterModules(),
    )

    private val topico = "smart-parking/vaga/01"
    private val bloco = Bloco(nome = "Bloco A", id = 1)
    private lateinit var dispositivo: Dispositivo
    private lateinit var vaga: Vaga

    @BeforeEach
    fun prepararCenario() {
        dispositivo = Dispositivo(
            nome = "ESP32-VAGA-01",
            macAddress = "AA:BB:CC:DD:EE:01",
            mqttTopic = topico,
            status = StatusDispositivo.OFFLINE,
            id = 10,
        )
        vaga = Vaga(numero = 1, bloco = bloco, dispositivo = dispositivo, status = StatusVaga.LIVRE, id = 1)

        // STUB: persistencia simulada, devolve a propria entidade recebida.
        `when`(dispositivoRepository.save(anyObj<Dispositivo>())).thenAnswer { it.arguments[0] }
        `when`(medicaoRepository.save(anyObj<Medicao>())).thenAnswer { it.arguments[0] }
    }

    private fun payload(
        status: String = "OCUPADA",
        distancia: String? = "80.5",
        deviceId: String = "ESP32-VAGA-01",
        vagaId: Long? = 1,
        timestamp: String? = "2026-11-08T14:30:00",
    ): String {
        val campos = buildList {
            add("\"deviceId\":\"$deviceId\"")
            vagaId?.let { add("\"vagaId\":$it") }
            distancia?.let { add("\"distancia\":$it") }
            add("\"status\":\"$status\"")
            timestamp?.let { add("\"timestamp\":\"$it\"") }
        }
        return campos.joinToString(",", "{", "}")
    }

    /** Programa os mocks para o fluxo principal: dispositivo pelo topico e vaga vinculada. */
    private fun dispositivoPeloTopicoComVaga() {
        `when`(dispositivoRepository.findByMqttTopic(topico)).thenReturn(dispositivo)
        `when`(vagaRepository.findByDispositivoId(10)).thenReturn(vaga)
    }

    private fun medicaoGravada(): Medicao {
        val captor = ArgumentCaptor.forClass(Medicao::class.java)
        verify(medicaoRepository).save(captor.capture())
        return captor.value
    }

    // ------------------------------------------------------------ fluxo principal

    @Test
    fun `C5 fluxo principal - leitura OCUPADA atualiza a vaga, grava medicao e marca dispositivo ONLINE (RN01, RN02, RN03, RN05)`() {
        dispositivoPeloTopicoComVaga()
        `when`(vagaService.aplicarLeituraDoSensor(vaga, StatusVaga.OCUPADA)).thenReturn(true)

        val resultado = service.processar(topico, payload())

        assertEquals(LeituraSensorService.Resultado(true, "ok", 1, StatusVaga.OCUPADA, true), resultado)
        assertEquals(StatusDispositivo.ONLINE, dispositivo.status)
        assertEquals(LocalDateTime.parse("2026-11-08T14:30:00"), dispositivo.ultimaComunicacao)
        val medicao = medicaoGravada()
        assertEquals(0, BigDecimal("80.5").compareTo(medicao.distancia))
        assertEquals(StatusVaga.OCUPADA, medicao.status)
        assertEquals(dispositivo, medicao.dispositivo)
        verify(vagaService).aplicarLeituraDoSensor(vaga, StatusVaga.OCUPADA)
    }

    @Test
    fun `RN02 - status em minusculas e aceito (particao valida)`() {
        dispositivoPeloTopicoComVaga()

        val resultado = service.processar(topico, payload(status = "livre", distancia = "250.8"))

        assertTrue(resultado.processado)
        verify(vagaService).aplicarLeituraDoSensor(vaga, StatusVaga.LIVRE)
    }

    @Test
    fun `RN01 - distancia 0 e o limite inferior valido`() {
        dispositivoPeloTopicoComVaga()

        val resultado = service.processar(topico, payload(distancia = "0"))

        assertTrue(resultado.processado)
        assertEquals(0, BigDecimal.ZERO.compareTo(medicaoGravada().distancia))
    }

    // ------------------------------------------------------------ fluxos de excecao

    @Test
    fun `C1 E1 - mensagem que nao e JSON e descartada sem acessar repositorios`() {
        val resultado = service.processar(topico, "isso nao e json")

        assertEquals(LeituraSensorService.Resultado(false, "payload invalido"), resultado)
        verifyNoInteractions(dispositivoRepository, vagaRepository, medicaoRepository, vagaService)
    }

    @Test
    fun `C2 E2 - distancia negativa (-0,01) e descartada (RN01, valor limite)`() {
        val resultado = service.processar(topico, payload(distancia = "-0.01"))

        assertEquals("distancia ausente ou negativa", resultado.motivo)
        assertFalse(resultado.processado)
        verifyNoInteractions(medicaoRepository, vagaService)
    }

    @Test
    fun `C2 E2 - distancia ausente e descartada (RN01)`() {
        val resultado = service.processar(topico, payload(distancia = null))

        assertEquals("distancia ausente ou negativa", resultado.motivo)
        verifyNoInteractions(medicaoRepository, vagaService)
    }

    @Test
    fun `C3 E3 - status diferente de LIVRE ou OCUPADA e descartado (RN02)`() {
        assertEquals("status desconhecido", service.processar(topico, payload(status = "TALVEZ")).motivo)
        // O sensor nao pode colocar a vaga em MANUTENCAO.
        assertEquals("status desconhecido", service.processar(topico, payload(status = "MANUTENCAO")).motivo)
        verifyNoInteractions(medicaoRepository, vagaService)
    }

    @Test
    fun `C4 E4 - dispositivo nao identificado por topico, deviceId nem vaga e descartado (RN03)`() {
        // Mocks sem stubbing devolvem null / Optional vazio: nada e encontrado.
        `when`(vagaRepository.findById(99)).thenReturn(Optional.empty())

        val resultado = service.processar("smart-parking/vaga/99", payload(deviceId = "DESCONHECIDO", vagaId = 99))

        assertEquals(LeituraSensorService.Resultado(false, "dispositivo nao cadastrado"), resultado)
        verify(dispositivoRepository, never()).save(anyObj())
        verifyNoInteractions(medicaoRepository, vagaService)
    }

    @Test
    fun `C4 E4 - sem deviceId conhecido e sem vagaId o dispositivo nao e resolvido (RN03)`() {
        val resultado = service.processar("smart-parking/vaga/99", payload(deviceId = "DESCONHECIDO", vagaId = null))

        assertEquals("dispositivo nao cadastrado", resultado.motivo)
    }

    // ------------------------------------------------------------ fluxos alternativos

    @Test
    fun `C6 A1 - dispositivo resolvido pelo deviceId quando o topico nao esta cadastrado (RN03)`() {
        `when`(dispositivoRepository.findByNomeIgnoreCase("esp32-vaga-01")).thenReturn(dispositivo)
        `when`(vagaRepository.findByDispositivoId(10)).thenReturn(vaga)

        val resultado = service.processar("smart-parking/vaga/99", payload(deviceId = "esp32-vaga-01"))

        assertTrue(resultado.processado)
        verify(vagaService).aplicarLeituraDoSensor(vaga, StatusVaga.OCUPADA)
    }

    @Test
    fun `C6 A1 - dispositivo resolvido pela vaga informada no payload (RN03)`() {
        `when`(vagaRepository.findById(1)).thenReturn(Optional.of(vaga))
        `when`(vagaRepository.findByDispositivoId(10)).thenReturn(vaga)

        val resultado = service.processar("smart-parking/vaga/99", payload(deviceId = "DESCONHECIDO", vagaId = 1))

        assertTrue(resultado.processado)
        assertEquals(1, resultado.vagaId)
    }

    @Test
    fun `C7 A2 - payload sem timestamp usa a hora do servidor (RN05)`() {
        dispositivoPeloTopicoComVaga()
        val antes = LocalDateTime.now()

        service.processar(topico, payload(timestamp = null))

        val momento = dispositivo.ultimaComunicacao!!
        assertFalse(momento.isBefore(antes))
        assertFalse(momento.isAfter(LocalDateTime.now()))
        assertEquals(momento, medicaoGravada().dataHora)
    }

    @Test
    fun `C8 A3 - dispositivo sem vaga vinculada usa o vagaId do payload`() {
        `when`(dispositivoRepository.findByMqttTopic(topico)).thenReturn(dispositivo)
        val vaga2 = Vaga(numero = 2, bloco = bloco, id = 2)
        `when`(vagaRepository.findById(2)).thenReturn(Optional.of(vaga2))
        `when`(vagaService.aplicarLeituraDoSensor(vaga2, StatusVaga.OCUPADA)).thenReturn(true)

        val resultado = service.processar(topico, payload(vagaId = 2))

        assertEquals(LeituraSensorService.Resultado(true, "ok", 2, StatusVaga.OCUPADA, true), resultado)
    }

    @Test
    fun `C9 E5 - sem vaga vinculada nem vagaId valido a medicao e gravada e nenhuma vaga muda`() {
        `when`(dispositivoRepository.findByMqttTopic(topico)).thenReturn(dispositivo)
        `when`(vagaRepository.findById(999)).thenReturn(Optional.empty())

        val resultado = service.processar(topico, payload(vagaId = 999))

        assertTrue(resultado.processado)
        assertEquals("medicao registrada sem vaga vinculada", resultado.motivo)
        assertNull(resultado.vagaId)
        medicaoGravada()
        verifyNoInteractions(vagaService)
    }

    @Test
    fun `C9 E5 - sem vaga vinculada e sem vagaId no payload`() {
        `when`(dispositivoRepository.findByMqttTopic(topico)).thenReturn(dispositivo)

        val resultado = service.processar(topico, payload(vagaId = null))

        assertEquals("medicao registrada sem vaga vinculada", resultado.motivo)
        verifyNoInteractions(vagaService)
    }

    @Test
    fun `C10 A4 - vaga em MANUTENCAO nao muda, mas a medicao e registrada (RN04)`() {
        vaga.status = StatusVaga.MANUTENCAO
        dispositivoPeloTopicoComVaga()
        // STUB do VagaService: a regra RN04 em si e testada em VagaServiceTest.
        `when`(vagaService.aplicarLeituraDoSensor(vaga, StatusVaga.OCUPADA)).thenReturn(false)

        val resultado = service.processar(topico, payload())

        assertTrue(resultado.processado)
        assertFalse(resultado.statusMudou)
        medicaoGravada()
    }

    @Test
    fun `C11 A5 - status lido igual ao atual nao altera a vaga (RN06)`() {
        dispositivoPeloTopicoComVaga()
        `when`(vagaService.aplicarLeituraDoSensor(vaga, StatusVaga.LIVRE)).thenReturn(false)

        val resultado = service.processar(topico, payload(status = "LIVRE", distancia = "250.8"))

        assertEquals(LeituraSensorService.Resultado(true, "ok", 1, StatusVaga.LIVRE, false), resultado)
    }
}

/**
 * `Mockito.any()` devolve null, o que o Kotlin rejeita em parametros nao nulos.
 * Este helper registra o matcher e devolve null "disfarcado" de T (tecnica do mockito-kotlin).
 */
@Suppress("UNCHECKED_CAST")
private fun <T> anyObj(): T {
    any<T>()
    return null as T
}
