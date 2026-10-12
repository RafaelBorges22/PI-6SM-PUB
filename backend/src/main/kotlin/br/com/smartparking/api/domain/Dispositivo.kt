package br.com.smartparking.api.domain

import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Dispositivo IoT instalado no estacionamento (CRUD 3).
 *
 * O campo [mqttTopic] e a chave usada para localizar o dispositivo quando uma
 * mensagem chega do broker MQTT.
 */
@Entity
@Table(
    name = "dispositivos",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_dispositivos_mac", columnNames = ["mac_address"]),
        UniqueConstraint(name = "uk_dispositivos_topic", columnNames = ["mqtt_topic"]),
        UniqueConstraint(name = "uk_dispositivos_nome", columnNames = ["nome"]),
    ],
)
class Dispositivo(

    @Column(nullable = false, length = 100)
    var nome: String,

    @Column(name = "mac_address", nullable = false, length = 17)
    var macAddress: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var tipo: TipoDispositivo = TipoDispositivo.ESP32,

    @Column(name = "mqtt_topic", nullable = false, length = 150)
    var mqttTopic: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: StatusDispositivo = StatusDispositivo.OFFLINE,

    /** Momento da ultima mensagem MQTT recebida; usado para detectar dispositivos offline. */
    @Column(name = "ultima_comunicacao")
    var ultimaComunicacao: LocalDateTime? = null,

    @Column(name = "data_instalacao")
    var dataInstalacao: LocalDate? = null,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
) {
    /** Lado inverso do vinculo: a coluna fica em `vagas.dispositivo_id`. */
    @OneToOne(mappedBy = "dispositivo", fetch = FetchType.LAZY)
    var vaga: Vaga? = null
}
