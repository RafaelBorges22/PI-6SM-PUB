package br.com.smartparking.api.service

import br.com.smartparking.api.common.RecursoNaoEncontradoException
import br.com.smartparking.api.common.RegraDeNegocioException
import br.com.smartparking.api.domain.Bloco
import br.com.smartparking.api.mapper.BlocoMapper
import br.com.smartparking.api.repository.BlocoRepository
import br.com.smartparking.api.repository.VagaRepository
import br.com.smartparking.api.web.dto.BlocoComVagasResponse
import br.com.smartparking.api.web.dto.BlocoRequest
import br.com.smartparking.api.web.dto.BlocoResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** CRUD 1 - Blocos (secao 15 da especificacao). */
@Service
@Transactional(readOnly = true)
class BlocoService(
    private val blocoRepository: BlocoRepository,
    private val vagaRepository: VagaRepository,
    private val mapper: BlocoMapper,
) {

    fun listar(): List<BlocoResponse> = blocoRepository.findAll()
        .sortedBy { it.nome }
        .map { paraResponse(it) }

    /** Busca parcial pelo nome (consulta JPQL com LIKE). */
    fun buscarPorNome(nome: String): List<BlocoResponse> =
        blocoRepository.buscarPorNome(nome.trim()).map { paraResponse(it) }

    fun buscarPorId(id: Long): BlocoResponse = paraResponse(obter(id))

    /** `GET /api/blocos/{id}/vagas`: bloco com suas vagas e o resumo de ocupacao. */
    fun buscarComVagas(id: Long): BlocoComVagasResponse =
        mapper.paraResponseComVagas(obter(id), vagaRepository.findByBlocoIdOrderByNumeroAsc(id))

    @Transactional
    fun criar(request: BlocoRequest): BlocoResponse {
        if (blocoRepository.existsByNomeIgnoreCase(request.nome.trim())) {
            throw RegraDeNegocioException("Ja existe um bloco com o nome ${request.nome}")
        }
        return mapper.paraResponse(blocoRepository.save(mapper.paraEntidade(request)), 0)
    }

    @Transactional
    fun atualizar(id: Long, request: BlocoRequest): BlocoResponse {
        val bloco = obter(id)
        val homonimo = blocoRepository.findByNomeIgnoreCase(request.nome.trim())
        if (homonimo != null && homonimo.id != id) {
            throw RegraDeNegocioException("Ja existe um bloco com o nome ${request.nome}")
        }
        mapper.atualizarEntidade(bloco, request)
        return paraResponse(blocoRepository.save(bloco))
    }

    @Transactional
    fun excluir(id: Long) {
        val bloco = obter(id)
        val vagas = vagaRepository.countByBlocoId(id)
        if (vagas > 0) {
            throw RegraDeNegocioException(
                "Nao e possivel excluir o bloco ${bloco.nome}: existem $vagas vaga(s) vinculada(s)",
            )
        }
        blocoRepository.delete(bloco)
    }

    /** Uso interno pelos demais servicos; lanca 404 quando o bloco nao existe. */
    fun obter(id: Long): Bloco = blocoRepository.findById(id)
        .orElseThrow { RecursoNaoEncontradoException("Bloco $id nao encontrado") }

    private fun paraResponse(bloco: Bloco): BlocoResponse =
        mapper.paraResponse(bloco, vagaRepository.countByBlocoId(bloco.id!!).toInt())
}
