package br.com.smartparking.api.repository

import br.com.smartparking.api.domain.*
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface UsuarioRepository : JpaRepository<Usuario, Long> {
    fun findByEmailIgnoreCase(email: String): Usuario?
    fun existsByEmailIgnoreCase(email: String): Boolean

    /** Busca parcial pelo nome do usuario (JPQL + LIKE). */
    @Query("select u from Usuario u where lower(u.nome) like lower(concat('%', :nome, '%')) order by u.nome asc")
    fun buscarPorNome(@Param("nome") nome: String): List<Usuario>
}
