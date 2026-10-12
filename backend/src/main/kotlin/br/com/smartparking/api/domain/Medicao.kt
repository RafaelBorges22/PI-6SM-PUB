package br.com.smartparking.api.domain

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Leitura enviada por um dispositivo IoT (secao 23 da especificacao).
 *
 * Nao e um dos CRUDs obrigatorios: e alimentada exclusivamente pelo consumidor
 * MQTT e serve de historico de ocupacao.
 */
@Entity
@Table(name = "medicoes")
class Medicao(

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dispositivo_id", nullable = false)
    var dispositivo: Dispositivo,

    @Column(nullable = false, precision = 8, scale = 2)
    var distancia: BigDecimal,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: StatusVaga,

    @Column(name = "data_hora", nullable = false)
    var dataHora: LocalDateTime = LocalDateTime.now(),

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
)
