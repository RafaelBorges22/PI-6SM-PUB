package br.com.smartparking.api.domain

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Teste de UNIDADE das entidades de dominio usadas no UC01 (Vaga, Dispositivo, Medicao).
 * Nao ha mocks: as entidades sao objetos simples e sao instanciadas diretamente.
 */
@DisplayName("Entidades do UC01 (unidade)")
class EntidadesUc01Test {

    private val bloco = Bloco(nome = "Bloco A")

    @Test
    fun `vaga nasce LIVRE, NORMAL e sem dispositivo (transicao t1 do GE)`() {
        val vaga = Vaga(numero = 1, bloco = bloco)

        assertEquals(StatusVaga.LIVRE, vaga.status)
        assertEquals(TipoVaga.NORMAL, vaga.tipo)
        assertNull(vaga.dispositivo)
        assertEquals(0.0, vaga.valorHora)
    }

    @Test
    fun `dispositivo nasce OFFLINE, ESP32 e sem comunicacao registrada`() {
        val dispositivo = Dispositivo(nome = "ESP32-VAGA-01", macAddress = "AA:BB:CC:DD:EE:01", mqttTopic = "t")

        assertEquals(StatusDispositivo.OFFLINE, dispositivo.status)
        assertEquals(TipoDispositivo.ESP32, dispositivo.tipo)
        assertNull(dispositivo.ultimaComunicacao)
        assertNull(dispositivo.vaga)
    }

    @Test
    fun `medicao sem data recebe o momento da criacao`() {
        val antes = LocalDateTime.now()
        val dispositivo = Dispositivo(nome = "ESP32-VAGA-01", macAddress = "AA:BB:CC:DD:EE:01", mqttTopic = "t")

        val medicao = Medicao(dispositivo = dispositivo, distancia = BigDecimal("80.50"), status = StatusVaga.OCUPADA)

        assertFalse(medicao.dataHora.isBefore(antes))
        assertEquals(dispositivo, medicao.dispositivo)
    }

    @Test
    fun `vinculo vaga-dispositivo e mantido pelo lado da vaga`() {
        val dispositivo = Dispositivo(nome = "ESP32-VAGA-01", macAddress = "AA:BB:CC:DD:EE:01", mqttTopic = "t")
        val vaga = Vaga(numero = 1, bloco = bloco, dispositivo = dispositivo)

        assertEquals(dispositivo, vaga.dispositivo)
        assertEquals(bloco, vaga.bloco)
    }

    @Test
    fun `status da vaga possui exatamente LIVRE, OCUPADA e MANUTENCAO`() {
        assertEquals(listOf("LIVRE", "OCUPADA", "MANUTENCAO"), StatusVaga.entries.map { it.name })
        assertTrue(StatusDispositivo.entries.contains(StatusDispositivo.ONLINE))
    }
}
