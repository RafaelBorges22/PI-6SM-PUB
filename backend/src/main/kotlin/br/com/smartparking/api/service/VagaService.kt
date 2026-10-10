package br.com.smartparking.api.service

import br.com.smartparking.api.common.RecursoNaoEncontradoException
import br.com.smartparking.api.common.RegraDeNegocioException
import br.com.smartparking.api.domain.Dispositivo
import br.com.smartparking.api.domain.StatusVaga
import br.com.smartparking.api.domain.TipoVaga
import br.com.smartparking.api.domain.Vaga
import br.com.smartparking.api.mapper.VagaMapper
import br.com.smartparking.api.repository.DispositivoRepository
import br.com.smartparking.api.repository.VagaRepository
import br.com.smartparking.api.web.dto.VagaRequest
import br.com.smartparking.api.web.dto.VagaResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** CRUD 2 - Vagas (secao 16 da especificacao). */
@Service
@Transactional(readOnly = true)
class VagaService(
    private val vagaRepository: VagaRepository,
    private val dispositivoRepository: DispositivoRepository,
    private val blocoService: BlocoService,
    private val mapper: VagaMapper,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun listar(status: StatusVaga?, tipo: TipoVaga?, blocoId: Long?): List<VagaResponse> =
        vagaRepository.buscarComFiltros(status, tipo, blocoId).map { mapper.paraResponse(it) }

    /** Busca parcial pela observacao (consulta JPQL com LIKE). */
    fun buscarPorObservacao(observacao: String): List<VagaResponse> =
        vagaRepository.buscarPorObservacao(observacao.trim()).map { mapper.paraResponse(it) }

    fun buscarPorId(id: Long): VagaResponse = mapper.paraResponse(obter(id))

    @Transactional
    fun criar(request: VagaRequest): VagaResponse {
        if (vagaRepository.existsByNumero(request.numero)) {
            throw RegraDeNegocioException("Ja existe uma vaga com o numero ${request.numero}")
        }
        val bloco = blocoService.obter(request.blocoId)
        val vaga = mapper.paraEntidade(request, bloco, resolverDispositivo(request.dispositivoId, vagaId = null))
        return mapper.paraResponse(vagaRepository.save(vaga))
    }

    @Transactional
    fun atualizar(id: Long, request: VagaRequest): VagaResponse {
        val vaga = obter(id)
        val homonima = vagaRepository.findByNumero(request.numero)
        if (homonima != null && homonima.id != id) {
            throw RegraDeNegocioException("Ja existe uma vaga com o numero ${request.numero}")
        }
        mapper.atualizarEntidade(
            vaga,
            request,
            bloco = blocoService.obter(request.blocoId),
            dispositivo = resolverDispositivo(request.dispositivoId, vagaId = id),
        )
        return mapper.paraResponse(vagaRepository.save(vaga))
    }

    @Transactional
    fun excluir(id: Long) {
        val vaga = obter(id)
        // O dispositivo carregado ainda apontaria para a vaga removida e o flush falharia.
        vaga.dispositivo?.vaga = null
        vaga.dispositivo = null
        vagaRepository.delete(vaga)
    }

    /**
     * Aplica ao dominio uma leitura recebida do sensor via MQTT.
     *
     * Vagas em MANUTENCAO sao um estado administrativo e por isso nao sao
     * sobrescritas pelo sensor. Retorna `true` quando o status mudou de fato.
     */
    @Transactional
    fun aplicarLeituraDoSensor(vaga: Vaga, statusLido: StatusVaga): Boolean {
        if (vaga.status == StatusVaga.MANUTENCAO) {
            log.debug("Vaga {} esta em MANUTENCAO; leitura do sensor ignorada", vaga.numero)
            return false
        }
        if (vaga.status == statusLido) {
            return false
        }
        log.info("Vaga {}: {} -> {}", vaga.numero, vaga.status, statusLido)
        vaga.status = statusLido
        vagaRepository.save(vaga)
        return true
    }

    fun obter(id: Long): Vaga = vagaRepository.findById(id)
        .orElseThrow { RecursoNaoEncontradoException("Vaga $id nao encontrada") }

    /**
     * Valida o vinculo 1:1 com o dispositivo: um dispositivo so pode monitorar
     * uma vaga por vez.
     */
    private fun resolverDispositivo(dispositivoId: Long?, vagaId: Long?): Dispositivo? {
        if (dispositivoId == null) return null
        val dispositivo = dispositivoRepository.findById(dispositivoId)
            .orElseThrow { RecursoNaoEncontradoException("Dispositivo $dispositivoId nao encontrado") }
        val vagaAtual = vagaRepository.findByDispositivoId(dispositivoId)
        if (vagaAtual != null && vagaAtual.id != vagaId) {
            throw RegraDeNegocioException(
                "O dispositivo ${dispositivo.nome} ja esta vinculado a vaga ${vagaAtual.numero}",
            )
        }
        return dispositivo
    }
}
