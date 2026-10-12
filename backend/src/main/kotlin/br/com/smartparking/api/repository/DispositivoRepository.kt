package br.com.smartparking.api.repository

import br.com.smartparking.api.domain.*
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface DispositivoRepository : JpaRepository<Dispositivo, Long> {
    fun existsByMacAddressIgnoreCase(macAddress: String): Boolean
    fun existsByMqttTopic(mqttTopic: String): Boolean
    fun existsByNomeIgnoreCase(nome: String): Boolean
    fun findByMqttTopic(mqttTopic: String): Dispositivo?
    fun findByNomeIgnoreCase(nome: String): Dispositivo?
    fun findByMacAddressIgnoreCase(macAddress: String): Dispositivo?
    fun countByStatus(status: StatusDispositivo): Long

    /** Busca parcial pelo nome do dispositivo (JPQL + LIKE). */
    @Query("select d from Dispositivo d where lower(d.nome) like lower(concat('%', :nome, '%')) order by d.nome asc")
    fun buscarPorNome(@Param("nome") nome: String): List<Dispositivo>
}
