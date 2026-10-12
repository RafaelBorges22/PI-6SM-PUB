package br.com.smartparking.app.model

import kotlinx.serialization.Serializable

/**
 * Modelos de autenticacao, Dispositivo, Usuario e Medicao. As datas trafegam como
 * texto ISO-8601 porque os frontends apenas as exibem.
 */

@Serializable
data class LoginRequest(val email: String, val senha: String)

/** Cadastro publico em POST /api/auth/signup (o perfil criado e sempre USER). */
@Serializable
data class SignupRequest(val nome: String, val email: String, val senha: String)

@Serializable
data class LoginResponse(
    val token: String,
    val type: String = "Bearer",
    val expiraEmSegundos: Long = 0,
    val usuarioId: Long = 0,
    val nome: String = "",
    val email: String = "",
    val role: String = "USER",
)

@Serializable
data class Dispositivo(
    val id: Long = 0,
    val nome: String = "",
    val macAddress: String = "",
    val tipo: String = "ESP32",
    val mqttTopic: String = "",
    val status: String = "OFFLINE",
    val ultimaComunicacao: String? = null,
    val vagaId: Long? = null,
    val vagaNumero: Int? = null,
    /** LocalDate no backend (yyyy-MM-dd). */
    val dataInstalacao: String? = null,
)

@Serializable
data class DispositivoRequest(
    val nome: String,
    val macAddress: String,
    val tipo: String = "ESP32",
    val mqttTopic: String,
    val status: String = "OFFLINE",
    val vagaId: Long? = null,
    val dataInstalacao: String? = null,
)

@Serializable
data class Usuario(
    val id: Long = 0,
    val nome: String = "",
    val email: String = "",
    val role: String = "USER",
    val ativo: Boolean = true,
    /** LocalDateTime no backend; preenchido pela API, nao vai no request. */
    val dataCadastro: String? = null,
)

@Serializable
data class UsuarioRequest(
    val nome: String,
    val email: String,
    val senha: String? = null,
    val role: String = "USER",
    val ativo: Boolean = true,
)

@Serializable
data class Medicao(
    val id: Long = 0,
    val dispositivoId: Long = 0,
    val dispositivoNome: String = "",
    val vagaId: Long? = null,
    val vagaNumero: Int? = null,
    val distancia: Double = 0.0,
    val status: String = "LIVRE",
    val dataHora: String = "",
)
