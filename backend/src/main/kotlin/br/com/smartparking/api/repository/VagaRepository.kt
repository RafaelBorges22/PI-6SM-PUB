package br.com.smartparking.api.repository

import br.com.smartparking.api.domain.*
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface VagaRepository : JpaRepository<Vaga, Long> {

    fun existsByNumero(numero: Int): Boolean

    fun findByNumero(numero: Int): Vaga?

    fun findByBlocoIdOrderByNumeroAsc(blocoId: Long): List<Vaga>

    fun findByDispositivoId(dispositivoId: Long): Vaga?

    fun countByBlocoId(blocoId: Long): Long

    /** Filtro combinado usado por `GET /api/vagas?status=&blocoId=&tipo=`; parametros nulos sao ignorados. */
    @Query(
        """
        select v from Vaga v
        where (:status is null or v.status = :status)
          and (:tipo is null or v.tipo = :tipo)
          and (:blocoId is null or v.bloco.id = :blocoId)
        order by v.numero asc
        """,
    )
    fun buscarComFiltros(
        @Param("status") status: StatusVaga?,
        @Param("tipo") tipo: TipoVaga?,
        @Param("blocoId") blocoId: Long?,
    ): List<Vaga>

    fun countByStatus(status: StatusVaga): Long

    /** Busca parcial pela observacao da vaga (JPQL + LIKE). */
    @Query(
        "select v from Vaga v where lower(v.observacao) like lower(concat('%', :observacao, '%')) order by v.numero asc",
    )
    fun buscarPorObservacao(@Param("observacao") observacao: String): List<Vaga>
}
