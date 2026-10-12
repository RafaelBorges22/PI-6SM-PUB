package br.com.smartparking.api.web.dto

import br.com.smartparking.api.domain.Role
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/** Corpo de `POST /api/auth/signin`. */
data class SigninRequest(
    @field:NotBlank(message = "email e obrigatorio")
    @field:Email(message = "email invalido")
    val email: String,

    @field:NotBlank(message = "senha e obrigatoria")
    @field:Size(max = 100, message = "senha deve ter no maximo 100 caracteres")
    val senha: String,
)

/** Corpo de `POST /api/auth/signup`: autocadastro, sempre com perfil USER. */
data class SignupRequest(
    @field:NotBlank(message = "nome e obrigatorio")
    @field:Size(max = 120, message = "nome deve ter no maximo 120 caracteres")
    val nome: String,

    @field:NotBlank(message = "email e obrigatorio")
    @field:Email(message = "email invalido")
    @field:Size(max = 150, message = "email deve ter no maximo 150 caracteres")
    val email: String,

    @field:NotBlank(message = "senha e obrigatoria")
    @field:Size(min = 6, max = 100, message = "senha deve ter entre 6 e 100 caracteres")
    val senha: String,
)

/**
 * Resposta do signin (secao 19 da especificacao).
 *
 * Alem de token e type, devolve os dados do usuario porque os frontends
 * precisam da role para decidir o que exibir no menu de administracao.
 */
data class SigninResponse(
    val token: String,
    val type: String = "Bearer",
    val expiraEmSegundos: Long,
    val usuarioId: Long,
    val nome: String,
    val email: String,
    val role: Role,
)
