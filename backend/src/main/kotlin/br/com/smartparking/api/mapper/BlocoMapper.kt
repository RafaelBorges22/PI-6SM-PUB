package br.com.smartparking.api.mapper

import br.com.smartparking.api.domain.Bloco
import br.com.smartparking.api.domain.StatusVaga
import br.com.smartparking.api.domain.Vaga
import br.com.smartparking.api.web.dto.BlocoComVagasResponse
import br.com.smartparking.api.web.dto.BlocoRequest
import br.com.smartparking.api.web.dto.BlocoResponse
import org.springframework.stereotype.Component

/** Conversoes entre a entidade [Bloco] e seus DTOs. */
@Component
class BlocoMapper(private val vagaMapper: VagaMapper) {

    fun paraEntidade(request: BlocoRequest): Bloco = Bloco(
        nome = request.nome.trim(),
        descricao = request.descricao,
        localizacao = request.localizacao,
        status = request.status,
        capacidade = request.capacidade,
        coberto = request.coberto,
    )

    /** Copia os dados do request para uma entidade ja existente (update). */
    fun atualizarEntidade(bloco: Bloco, request: BlocoRequest) {
        bloco.nome = request.nome.trim()
        bloco.descricao = request.descricao
        bloco.localizacao = request.localizacao
        bloco.status = request.status
        bloco.capacidade = request.capacidade
        bloco.coberto = request.coberto
    }

    fun paraResponse(bloco: Bloco, totalVagas: Int): BlocoResponse = BlocoResponse(
        id = bloco.id!!,
        nome = bloco.nome,
        descricao = bloco.descricao,
        localizacao = bloco.localizacao,
        status = bloco.status,
        capacidade = bloco.capacidade,
        coberto = bloco.coberto,
        totalVagas = totalVagas,
    )

    fun paraResponseComVagas(bloco: Bloco, vagas: List<Vaga>): BlocoComVagasResponse = BlocoComVagasResponse(
        id = bloco.id!!,
        nome = bloco.nome,
        descricao = bloco.descricao,
        localizacao = bloco.localizacao,
        status = bloco.status,
        vagas = vagas.map { vagaMapper.paraResumo(it) },
        livres = vagas.count { it.status == StatusVaga.LIVRE },
        ocupadas = vagas.count { it.status == StatusVaga.OCUPADA },
        manutencao = vagas.count { it.status == StatusVaga.MANUTENCAO },
    )
}
