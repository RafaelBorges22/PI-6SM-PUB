package br.com.smartparking.api.mqtt

import br.com.smartparking.api.domain.Dispositivo
import br.com.smartparking.api.domain.Medicao
import br.com.smartparking.api.domain.StatusDispositivo
import br.com.smartparking.api.domain.StatusVaga
import br.com.smartparking.api.repository.DispositivoRepository
import br.com.smartparking.api.repository.MedicaoRepository
import br.com.smartparking.api.repository.VagaRepository
import br.com.smartparking.api.service.VagaService
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * Regra de negocio do consumo MQTT, isolada do transporte.
 *
 * Recebe o par (topico, payload) vindo do broker, resolve o dispositivo e a vaga,
 * grava a medicao e atualiza o status da vaga. Como nao depende do cliente Paho,
 * pode ser testada sem broker.
 */
@Service
class LeituraSensorService(
    private val dispositivoRepository: DispositivoRepository,
    private val vagaRepository: VagaRepository,
    private val medicaoRepository: MedicaoRepository,
    private val vagaService: VagaService,
    private val objectMapper: ObjectMapper,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    /** Resultado do processamento, usado nos logs e nos testes. */
    data class Resultado(
        val processado: Boolean,
        val motivo: String,
        val vagaId: Long? = null,
        val statusAplicado: StatusVaga? = null,
        val statusMudou: Boolean = false,
    )

    @Transactional
    fun processar(topico: String, mensagemJson: String): Resultado {
        val payload = try {
            objectMapper.readValue<LeituraSensorPayload>(mensagemJson)
        } catch (e: Exception) {
            log.warn("Payload MQTT invalido no topico {}: {}", topico, e.message)
            return Resultado(false, "payload invalido")
        }

        val distancia = payload.distancia
        if (distancia == null || distancia.signum() < 0) {
            log.warn("Payload MQTT sem distancia valida no topico {}", topico)
            return Resultado(false, "distancia ausente ou negativa")
        }

        val status = when (payload.status?.uppercase()) {
            "OCUPADA" -> StatusVaga.OCUPADA
            "LIVRE" -> StatusVaga.LIVRE
            else -> {
                log.warn("Status desconhecido '{}' no topico {}", payload.status, topico)
                return Resultado(false, "status desconhecido")
            }
        }

        val dispositivo = resolverDispositivo(topico, payload)
        if (dispositivo == null) {
            log.warn("Nenhum dispositivo cadastrado para o topico {} / deviceId {}", topico, payload.deviceId)
            return Resultado(false, "dispositivo nao cadastrado")
        }

        val momento = payload.timestamp ?: LocalDateTime.now()

        dispositivo.status = StatusDispositivo.ONLINE
        dispositivo.ultimaComunicacao = momento
        dispositivoRepository.save(dispositivo)

        medicaoRepository.save(
            Medicao(
                dispositivo = dispositivo,
                distancia = distancia,
                status = status,
                dataHora = momento,
            ),
        )

        val vaga = vagaRepository.findByDispositivoId(dispositivo.id!!)
            ?: payload.vagaId?.let { vagaRepository.findById(it).orElse(null) }

        if (vaga == null) {
            log.warn("Dispositivo {} nao esta vinculado a nenhuma vaga; medicao registrada", dispositivo.nome)
            return Resultado(true, "medicao registrada sem vaga vinculada", statusAplicado = status)
        }

        val mudou = vagaService.aplicarLeituraDoSensor(vaga, status)
        log.info(
            "MQTT [{}] device={} distancia={}cm status={} vaga={} mudou={}",
            topico, dispositivo.nome, distancia, status, vaga.numero, mudou,
        )
        return Resultado(true, "ok", vaga.id, status, mudou)
    }

    /**
     * Ordem de resolucao: topico de chegada, depois `deviceId` (igual ao nome
     * cadastrado) e por fim a vaga informada no payload.
     */
    private fun resolverDispositivo(topico: String, payload: LeituraSensorPayload): Dispositivo? =
        dispositivoRepository.findByMqttTopic(topico)
            ?: payload.deviceId?.let { dispositivoRepository.findByNomeIgnoreCase(it) }
            ?: payload.vagaId?.let { vagaRepository.findById(it).orElse(null)?.dispositivo }
}
