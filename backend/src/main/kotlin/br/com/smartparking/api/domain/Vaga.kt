package br.com.smartparking.api.domain

import jakarta.persistence.*

/**
 * Vaga de estacionamento (CRUD 2).
 *
 * Relacionamentos: N Vagas ----- 1 Bloco e Vaga 1 ----- 1 Dispositivo.
 *
 * A vaga e o lado dono do vinculo com o dispositivo (coluna `vagas.dispositivo_id`,
 * com restricao UNIQUE). O dispositivo enxerga o vinculo pelo lado inverso, o que
 * evita duas colunas de chave estrangeira apontando uma para a outra.
 */
@Entity
@Table(
    name = "vagas",
    uniqueConstraints = [UniqueConstraint(name = "uk_vagas_numero", columnNames = ["numero"])],
)
class Vaga(

    @Column(nullable = false)
    var numero: Int,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var tipo: TipoVaga = TipoVaga.NORMAL,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: StatusVaga = StatusVaga.LIVRE,

    /** Valor cobrado por hora de permanencia na vaga. */
    @Column(name = "valor_hora", nullable = false)
    var valorHora: Double = 0.0,

    @Column(length = 255)
    var observacao: String? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bloco_id", nullable = false)
    var bloco: Bloco,

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dispositivo_id", unique = true)
    var dispositivo: Dispositivo? = null,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
)
