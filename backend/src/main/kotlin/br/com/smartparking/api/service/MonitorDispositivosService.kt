package br.com.smartparking.api.service

import br.com.smartparking.api.domain.StatusDispositivo
import br.com.smartparking.api.repository.DispositivoRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * Detecta dispositivos que pararam de comunicar (secao 44 da especificacao).
 *
 * O [br.com.smartparking.api.mqtt.LeituraSensorService] marca o dispositivo ONLINE e
 * grava `ultimaComunicacao` a cada mensagem; esta rotina faz o caminho inverso e marca
 * OFFLINE quem esta ONLINE sem mensagem ha mais que `tempoLimiteOfflineSegundos`.
 *
 * Dispositivos em MANUTENCAO nao sao alterados (estado administrativo). Assim que um
 * dispositivo OFFLINE volta a publicar, o consumo MQTT o marca ONLINE de novo.
 *
 * O agendamento so roda com `smartparking.dispositivos.monitor-offline-habilitado=true`
 * (ver [br.com.smartparking.api.config.AgendamentoConfig]).
 */
@Service
class MonitorDispositivosService(
    private val dispositivoRepository: DispositivoRepository,
    private val properties: MonitorDispositivosProperties,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(
        fixedDelayString = "\${smartparking.dispositivos.intervalo-verificacao-ms:30000}",
        initialDelayString = "\${smartparking.dispositivos.intervalo-verificacao-ms:30000}",
    )
    fun verificarPeriodicamente() {
        marcarOfflineInativos()
    }

    /**
     * Marca OFFLINE os dispositivos ONLINE sem comunicacao desde
     * `agora - tempoLimiteOfflineSegundos`. Retorna quantos foram alterados.
     */
    @Transactional
    fun marcarOfflineInativos(agora: LocalDateTime = LocalDateTime.now()): Int {
        val limite = agora.minusSeconds(properties.tempoLimiteOfflineSegundos)
        val inativos = dispositivoRepository.buscarOnlineSemComunicacaoDesde(limite)
        if (inativos.isEmpty()) {
            return 0
        }

        inativos.forEach { it.status = StatusDispositivo.OFFLINE }
        dispositivoRepository.saveAll(inativos)

        log.warn(
            "{} dispositivo(s) marcado(s) OFFLINE por falta de comunicacao ha mais de {}s: {}",
            inativos.size,
            properties.tempoLimiteOfflineSegundos,
            inativos.joinToString { it.nome },
        )
        return inativos.size
    }
}
