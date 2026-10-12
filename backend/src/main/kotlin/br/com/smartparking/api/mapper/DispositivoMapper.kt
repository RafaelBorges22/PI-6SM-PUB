package br.com.smartparking.api.mapper

import br.com.smartparking.api.domain.Dispositivo
import br.com.smartparking.api.domain.Medicao
import br.com.smartparking.api.web.dto.DispositivoRequest
import br.com.smartparking.api.web.dto.DispositivoResponse
import br.com.smartparking.api.web.dto.MedicaoResponse
import org.springframework.stereotype.Component

/** Conversoes entre [Dispositivo]/[Medicao] e seus DTOs. */
@Component
class DispositivoMapper {

    fun paraEntidade(request: DispositivoRequest): Dispositivo = Dispositivo(
        nome = request.nome.trim(),
        macAddress = request.macAddress.uppercase(),
        tipo = request.tipo,
        mqttTopic = request.mqttTopic,
        status = request.status,
        dataInstalacao = request.dataInstalacao,
    )

    fun atualizarEntidade(dispositivo: Dispositivo, request: DispositivoRequest) {
        dispositivo.nome = request.nome.trim()
        dispositivo.macAddress = request.macAddress.uppercase()
        dispositivo.tipo = request.tipo
        dispositivo.mqttTopic = request.mqttTopic
        dispositivo.status = request.status
        dispositivo.dataInstalacao = request.dataInstalacao
    }

    fun paraResponse(dispositivo: Dispositivo): DispositivoResponse = DispositivoResponse(
        id = dispositivo.id!!,
        nome = dispositivo.nome,
        macAddress = dispositivo.macAddress,
        tipo = dispositivo.tipo,
        mqttTopic = dispositivo.mqttTopic,
        status = dispositivo.status,
        dataInstalacao = dispositivo.dataInstalacao,
        ultimaComunicacao = dispositivo.ultimaComunicacao,
        vagaId = dispositivo.vaga?.id,
        vagaNumero = dispositivo.vaga?.numero,
    )

    fun paraMedicaoResponse(medicao: Medicao): MedicaoResponse = MedicaoResponse(
        id = medicao.id!!,
        dispositivoId = medicao.dispositivo.id!!,
        dispositivoNome = medicao.dispositivo.nome,
        vagaId = medicao.dispositivo.vaga?.id,
        vagaNumero = medicao.dispositivo.vaga?.numero,
        distancia = medicao.distancia,
        status = medicao.status,
        dataHora = medicao.dataHora,
    )
}
