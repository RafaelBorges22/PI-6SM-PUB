package br.com.smartparking.api.repository

import br.com.smartparking.api.domain.*
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface DispositivoRepository : JpaRepository<Dispositivo, Long> {
    fun existsByMacAddressIgnoreCase(macAddress: String): Boolean
    fun existsByMqttTopic(mqttTopic: String): Boolean
    fun existsByNomeIgnoreCase(nome: String): Boolean
    fun findByMqttTopic(mqttTopic: String): Dispositivo?
    fun findByNomeIgnoreCase(nome: String): Dispositivo?
    fun findByMacAddressIgnoreCase(macAddress: String): Dispositivo?
    fun countByStatus(status: StatusDispositivo): Long

    /**
     * Dispositivos ONLINE sem comunicacao desde [limite]. Inclui os que ficaram ONLINE
     * sem nunca ter comunicado (status alterado manualmente pelo CRUD).
     */
    @Query(
        "select d from Dispositivo d where d.status = br.com.smartparking.api.domain.StatusDispositivo.ONLINE " +
            "and (d.ultimaComunicacao is null or d.ultimaComunicacao < :limite)",
    )
    fun buscarOnlineSemComunicacaoDesde(@Param("limite") limite: LocalDateTime): List<Dispositivo>

    /** Busca parcial pelo nome do dispositivo (JPQL + LIKE). */
    @Query("select d from Dispositivo d where lower(d.nome) like lower(concat('%', :nome, '%')) order by d.nome asc")
    fun buscarPorNome(@Param("nome") nome: String): List<Dispositivo>
}
