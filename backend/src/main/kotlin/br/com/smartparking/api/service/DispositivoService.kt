package br.com.smartparking.api.service

import br.com.smartparking.api.common.RecursoNaoEncontradoException
import br.com.smartparking.api.common.RegraDeNegocioException
import br.com.smartparking.api.domain.Dispositivo
import br.com.smartparking.api.mapper.DispositivoMapper
import br.com.smartparking.api.repository.DispositivoRepository
import br.com.smartparking.api.repository.VagaRepository
import br.com.smartparking.api.web.dto.DispositivoRequest
import br.com.smartparking.api.web.dto.DispositivoResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** CRUD 3 - Dispositivos IoT (secao 17 da especificacao). */
@Service
@Transactional(readOnly = true)
class DispositivoService(
    private val dispositivoRepository: DispositivoRepository,
    private val vagaRepository: VagaRepository,
    private val mapper: DispositivoMapper,
) {

    fun listar(): List<DispositivoResponse> = dispositivoRepository.findAll()
        .sortedBy { it.nome }
        .map { mapper.paraResponse(it) }

    /** Busca parcial pelo nome (consulta JPQL com LIKE). */
    fun buscarPorNome(nome: String): List<DispositivoResponse> =
        dispositivoRepository.buscarPorNome(nome.trim()).map { mapper.paraResponse(it) }

    fun buscarPorId(id: Long): DispositivoResponse = mapper.paraResponse(obter(id))

    @Transactional
    fun criar(request: DispositivoRequest): DispositivoResponse {
        validarUnicidade(request, idAtual = null)
        val dispositivo = dispositivoRepository.save(mapper.paraEntidade(request))
        vincularVaga(dispositivo, request.vagaId)
        return mapper.paraResponse(dispositivo)
    }

    @Transactional
    fun atualizar(id: Long, request: DispositivoRequest): DispositivoResponse {
        val dispositivo = obter(id)
        validarUnicidade(request, idAtual = id)
        mapper.atualizarEntidade(dispositivo, request)
        dispositivoRepository.save(dispositivo)
        vincularVaga(dispositivo, request.vagaId)
        return mapper.paraResponse(dispositivo)
    }

    @Transactional
    fun excluir(id: Long) {
        val dispositivo = obter(id)
        // A coluna da FK fica em `vagas.dispositivo_id`: e preciso desvincular antes de remover.
        vagaRepository.findByDispositivoId(id)?.let {
            it.dispositivo = null
            vagaRepository.save(it)
        }
        dispositivoRepository.delete(dispositivo)
    }

    fun obter(id: Long): Dispositivo = dispositivoRepository.findById(id)
        .orElseThrow { RecursoNaoEncontradoException("Dispositivo $id nao encontrado") }

    private fun validarUnicidade(request: DispositivoRequest, idAtual: Long?) {
        dispositivoRepository.findByNomeIgnoreCase(request.nome)?.let {
            if (it.id != idAtual) throw RegraDeNegocioException("Ja existe um dispositivo com o nome ${request.nome}")
        }
        dispositivoRepository.findByMacAddressIgnoreCase(request.macAddress)?.let {
            if (it.id != idAtual) {
                throw RegraDeNegocioException("Ja existe um dispositivo com o MAC ${request.macAddress}")
            }
        }
        dispositivoRepository.findByMqttTopic(request.mqttTopic)?.let {
            if (it.id != idAtual) {
                throw RegraDeNegocioException("Ja existe um dispositivo com o topico ${request.mqttTopic}")
            }
        }
    }

    /**
     * Sincroniza o lado dono do relacionamento (`vagas.dispositivo_id`) a partir
     * do `vagaId` informado no payload do dispositivo.
     */
    private fun vincularVaga(dispositivo: Dispositivo, vagaId: Long?) {
        val vagaAtual = vagaRepository.findByDispositivoId(dispositivo.id!!)
        if (vagaId == null) {
            if (vagaAtual != null) {
                vagaAtual.dispositivo = null
                vagaRepository.save(vagaAtual)
                dispositivo.vaga = null
            }
            return
        }
        if (vagaAtual?.id == vagaId) {
            dispositivo.vaga = vagaAtual
            return
        }
        val novaVaga = vagaRepository.findById(vagaId)
            .orElseThrow { RecursoNaoEncontradoException("Vaga $vagaId nao encontrada") }
        val ocupanteAtual = novaVaga.dispositivo
        if (ocupanteAtual != null && ocupanteAtual.id != dispositivo.id) {
            throw RegraDeNegocioException(
                "A vaga ${novaVaga.numero} ja possui o dispositivo ${ocupanteAtual.nome} vinculado",
            )
        }
        vagaAtual?.let {
            it.dispositivo = null
            vagaRepository.save(it)
        }
        novaVaga.dispositivo = dispositivo
        vagaRepository.save(novaVaga)
        dispositivo.vaga = novaVaga
    }
}
