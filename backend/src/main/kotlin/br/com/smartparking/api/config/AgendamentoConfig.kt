package br.com.smartparking.api.config

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling

/**
 * Liga as rotinas `@Scheduled` (monitor de dispositivos offline).
 *
 * Desligado com `smartparking.dispositivos.monitor-offline-habilitado=false`, como no
 * perfil de testes, para que nenhuma rotina altere os dados durante os testes.
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(
    prefix = "smartparking.dispositivos",
    name = ["monitor-offline-habilitado"],
    havingValue = "true",
    matchIfMissing = true,
)
class AgendamentoConfig
