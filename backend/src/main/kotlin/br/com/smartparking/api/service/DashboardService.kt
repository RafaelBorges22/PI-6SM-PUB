package br.com.smartparking.api.service

import br.com.smartparking.api.domain.StatusDispositivo
import br.com.smartparking.api.domain.StatusVaga
import br.com.smartparking.api.mapper.DispositivoMapper
import br.com.smartparking.api.repository.BlocoRepository
import br.com.smartparking.api.repository.DispositivoRepository
import br.com.smartparking.api.repository.MedicaoRepository
import br.com.smartparking.api.repository.VagaRepository
import br.com.smartparking.api.web.dto.BlocoOcupacaoResponse
import br.com.smartparking.api.web.dto.DashboardResponse
import br.com.smartparking.api.web.dto.MedicaoResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.math.roundToInt

/** Consolida os numeros exibidos no dashboard de Web, Mobile e Desktop. */
@Service
@Transactional(readOnly = true)
class DashboardService(
    private val blocoRepository: BlocoRepository,
    private val vagaRepository: VagaRepository,
    private val dispositivoRepository: DispositivoRepository,
    private val medicaoRepository: MedicaoRepository,
    private val dispositivoMapper: DispositivoMapper,
) {

    fun resumo(): DashboardResponse {
        val vagas = vagaRepository.findAll()
        val total = vagas.size.toLong()
        val ocupadas = vagas.count { it.status == StatusVaga.OCUPADA }.toLong()

        val porBloco = blocoRepository.findAll().sortedBy { it.nome }.map { bloco ->
            val doBloco = vagas.filter { it.bloco.id == bloco.id }
            BlocoOcupacaoResponse(
                blocoId = bloco.id!!,
                nome = bloco.nome,
                totalVagas = doBloco.size,
                livres = doBloco.count { it.status == StatusVaga.LIVRE },
                ocupadas = doBloco.count { it.status == StatusVaga.OCUPADA },
                manutencao = doBloco.count { it.status == StatusVaga.MANUTENCAO },
            )
        }

        return DashboardResponse(
            totalBlocos = blocoRepository.count(),
            totalVagas = total,
            livres = vagas.count { it.status == StatusVaga.LIVRE }.toLong(),
            ocupadas = ocupadas,
            manutencao = vagas.count { it.status == StatusVaga.MANUTENCAO }.toLong(),
            taxaOcupacaoPercentual = if (total == 0L) 0 else (ocupadas * 100.0 / total).roundToInt(),
            dispositivosOnline = dispositivoRepository.countByStatus(StatusDispositivo.ONLINE),
            dispositivosOffline = dispositivoRepository.countByStatus(StatusDispositivo.OFFLINE),
            blocos = porBloco,
        )
    }

    fun ultimasMedicoes(): List<MedicaoResponse> =
        medicaoRepository.findTop100ByOrderByDataHoraDesc().map { dispositivoMapper.paraMedicaoResponse(it) }

    fun medicoesDoDispositivo(dispositivoId: Long): List<MedicaoResponse> =
        medicaoRepository.findTop50ByDispositivoIdOrderByDataHoraDesc(dispositivoId)
            .map { dispositivoMapper.paraMedicaoResponse(it) }

    /** Busca parcial pelo nome do dispositivo (consulta JPQL com LIKE). */
    fun buscarMedicoesPorDispositivo(nome: String): List<MedicaoResponse> =
        medicaoRepository.buscarPorNomeDoDispositivo(nome.trim()).map { dispositivoMapper.paraMedicaoResponse(it) }
}
