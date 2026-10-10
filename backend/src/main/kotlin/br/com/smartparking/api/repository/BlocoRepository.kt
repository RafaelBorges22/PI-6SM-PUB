package br.com.smartparking.api.repository

import br.com.smartparking.api.domain.*
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface BlocoRepository : JpaRepository<Bloco, Long> {
    fun existsByNomeIgnoreCase(nome: String): Boolean
    fun findByNomeIgnoreCase(nome: String): Bloco?

    /** Busca parcial pelo nome do bloco (JPQL + LIKE). */
    @Query("select b from Bloco b where lower(b.nome) like lower(concat('%', :nome, '%')) order by b.nome asc")
    fun buscarPorNome(@Param("nome") nome: String): List<Bloco>
}
