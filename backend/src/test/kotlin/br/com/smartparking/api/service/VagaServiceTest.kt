package br.com.smartparking.api.service

import br.com.smartparking.api.domain.Bloco
import br.com.smartparking.api.domain.StatusVaga
import br.com.smartparking.api.domain.Vaga
import br.com.smartparking.api.mapper.VagaMapper
import br.com.smartparking.api.repository.DispositivoRepository
import br.com.smartparking.api.repository.VagaRepository
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Teste de UNIDADE de [VagaService.aplicarLeituraDoSensor] (UC01, regras RN04 e RN06).
 *
 *  - MOCK: VagaRepository - verifica se a vaga foi (ou nao) persistida.
 *  - STUBS: DispositivoRepository, BlocoService e VagaMapper sao exigidos pelo construtor,
 *    mas nao participam deste metodo; entram como mocks vazios so para montar o servico.
 *  - DRIVER: o proprio teste, no papel do LeituraSensorService, chama o metodo com a vaga
 *    e o status lido pelo sensor.
 */
@DisplayName("VagaService.aplicarLeituraDoSensor (unidade, com mocks)")
class VagaServiceTest {

    private val vagaRepository: VagaRepository = mock(VagaRepository::class.java)

    private val service = VagaService(
        vagaRepository,
        mock(DispositivoRepository::class.java), // stub nao utilizado
        mock(BlocoService::class.java), // stub nao utilizado
        mock(VagaMapper::class.java), // stub nao utilizado
    )

    private fun vaga(status: StatusVaga) =
        Vaga(numero = 1, bloco = Bloco(nome = "Bloco A", id = 1), status = status, id = 1)

    @Test
    fun `LIVRE para OCUPADA muda o status e persiste (transicao t2)`() {
        val vaga = vaga(StatusVaga.LIVRE)
        `when`(vagaRepository.save(vaga)).thenReturn(vaga)

        assertTrue(service.aplicarLeituraDoSensor(vaga, StatusVaga.OCUPADA))
        assertEquals(StatusVaga.OCUPADA, vaga.status)
        verify(vagaRepository).save(vaga)
    }

    @Test
    fun `OCUPADA para LIVRE muda o status e persiste (transicao t3)`() {
        val vaga = vaga(StatusVaga.OCUPADA)
        `when`(vagaRepository.save(vaga)).thenReturn(vaga)

        assertTrue(service.aplicarLeituraDoSensor(vaga, StatusVaga.LIVRE))
        assertEquals(StatusVaga.LIVRE, vaga.status)
        verify(vagaRepository).save(vaga)
    }

    @ParameterizedTest(name = "status {0} lido igual ao atual nao e regravado")
    @EnumSource(value = StatusVaga::class, names = ["LIVRE", "OCUPADA"])
    fun `RN06 - status igual ao atual nao persiste (transicoes t4 e t5)`(status: StatusVaga) {
        val vaga = vaga(status)

        assertFalse(service.aplicarLeituraDoSensor(vaga, status))
        assertEquals(status, vaga.status)
        verify(vagaRepository, never()).save(vaga)
    }

    @ParameterizedTest(name = "leitura {0} em vaga MANUTENCAO e ignorada")
    @EnumSource(value = StatusVaga::class, names = ["LIVRE", "OCUPADA"])
    fun `RN04 - vaga em MANUTENCAO nao e alterada pelo sensor (transicao t8)`(statusLido: StatusVaga) {
        val vaga = vaga(StatusVaga.MANUTENCAO)

        assertFalse(service.aplicarLeituraDoSensor(vaga, statusLido))
        assertEquals(StatusVaga.MANUTENCAO, vaga.status)
        verify(vagaRepository, never()).save(vaga)
    }
}
