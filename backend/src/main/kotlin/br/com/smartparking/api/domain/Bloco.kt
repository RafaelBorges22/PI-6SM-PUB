package br.com.smartparking.api.domain

import jakarta.persistence.*

/**
 * Bloco: area ou setor do estacionamento (CRUD 1).
 *
 * Relacionamento: Bloco 1 ----- N Vagas.
 */
@Entity
@Table(name = "blocos")
class Bloco(

    @Column(nullable = false, length = 100)
    var nome: String,

    @Column(length = 255)
    var descricao: String? = null,

    @Column(length = 150)
    var localizacao: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: StatusBloco = StatusBloco.ATIVO,

    /** Quantidade maxima de vagas prevista para o bloco. */
    @Column(nullable = false)
    var capacidade: Int = 0,

    /** Indica se o bloco e coberto (garagem) ou a ceu aberto. */
    @Column(nullable = false)
    var coberto: Boolean = false,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
) {
    // Sem cascade: BlocoService impede excluir um bloco que ainda tenha vagas.
    @OneToMany(mappedBy = "bloco")
    @OrderBy("numero ASC")
    var vagas: MutableList<Vaga> = mutableListOf()
}
