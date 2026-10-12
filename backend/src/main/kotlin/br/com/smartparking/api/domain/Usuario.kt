package br.com.smartparking.api.domain

import jakarta.persistence.*
import java.time.LocalDateTime

/** Usuario do sistema (CRUD 4). A senha e sempre persistida como hash BCrypt. */
@Entity
@Table(
    name = "users",
    uniqueConstraints = [UniqueConstraint(name = "uk_users_email", columnNames = ["email"])],
)
class Usuario(

    @Column(nullable = false, length = 120)
    var nome: String,

    @Column(nullable = false, length = 150)
    var email: String,

    @Column(name = "senha_hash", nullable = false, length = 100)
    var senhaHash: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var role: Role = Role.USER,

    @Column(nullable = false)
    var ativo: Boolean = true,

    @Column(name = "data_cadastro", nullable = false, updatable = false)
    var dataCadastro: LocalDateTime = LocalDateTime.now(),

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
)
