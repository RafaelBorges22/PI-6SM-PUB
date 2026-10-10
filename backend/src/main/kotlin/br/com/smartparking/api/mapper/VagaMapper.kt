package br.com.smartparking.api.mapper

import br.com.smartparking.api.domain.Bloco
import br.com.smartparking.api.domain.Dispositivo
import br.com.smartparking.api.domain.Vaga
import br.com.smartparking.api.web.dto.VagaRequest
import br.com.smartparking.api.web.dto.VagaResponse
import br.com.smartparking.api.web.dto.VagaResumoResponse
import org.springframework.stereotype.Component

/**
 * Conversoes entre a entidade [Vaga] e seus DTOs.
 *
 * O bloco e o dispositivo chegam ja resolvidos pelo servico, que e quem
 * consulta o banco e aplica as regras do vinculo.
 */
@Component
class VagaMapper {

    fun paraEntidade(request: VagaRequest, bloco: Bloco, dispositivo: Dispositivo?): Vaga = Vaga(
        numero = request.numero,
        tipo = request.tipo,
        status = request.status,
        valorHora = request.valorHora,
        observacao = request.observacao,
        bloco = bloco,
        dispositivo = dispositivo,
    )

    fun atualizarEntidade(vaga: Vaga, request: VagaRequest, bloco: Bloco, dispositivo: Dispositivo?) {
        vaga.numero = request.numero
        vaga.tipo = request.tipo
        vaga.status = request.status
        vaga.valorHora = request.valorHora
        vaga.observacao = request.observacao
        vaga.bloco = bloco
        vaga.dispositivo = dispositivo
    }

    fun paraResponse(vaga: Vaga): VagaResponse = VagaResponse(
        id = vaga.id!!,
        numero = vaga.numero,
        tipo = vaga.tipo,
        status = vaga.status,
        valorHora = vaga.valorHora,
        observacao = vaga.observacao,
        blocoId = vaga.bloco.id!!,
        blocoNome = vaga.bloco.nome,
        dispositivoId = vaga.dispositivo?.id,
        dispositivoNome = vaga.dispositivo?.nome,
    )

    fun paraResumo(vaga: Vaga): VagaResumoResponse = VagaResumoResponse(
        id = vaga.id!!,
        numero = vaga.numero,
        tipo = vaga.tipo,
        status = vaga.status,
        dispositivoId = vaga.dispositivo?.id,
    )
}
