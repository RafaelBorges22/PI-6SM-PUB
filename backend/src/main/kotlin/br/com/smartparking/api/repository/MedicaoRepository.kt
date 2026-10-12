package br.com.smartparking.api.repository

import br.com.smartparking.api.domain.*
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface MedicaoRepository : JpaRepository<Medicao, Long> {
    fun findTop50ByDispositivoIdOrderByDataHoraDesc(dispositivoId: Long): List<Medicao>
    fun findTop100ByOrderByDataHoraDesc(): List<Medicao>

    /** Busca parcial pelo nome do dispositivo que enviou a medicao (JPQL + LIKE). */
    @Query(
        """
        select m from Medicao m
        where lower(m.dispositivo.nome) like lower(concat('%', :nome, '%'))
        order by m.dataHora desc
        """,
    )
    fun buscarPorNomeDoDispositivo(@Param("nome") nome: String): List<Medicao>
}
