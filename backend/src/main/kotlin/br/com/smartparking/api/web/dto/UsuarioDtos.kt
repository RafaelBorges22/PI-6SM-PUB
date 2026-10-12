package br.com.smartparking.api.web.dto

import br.com.smartparking.api.domain.Role
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

data class UsuarioRequest(
    @field:NotBlank(message = "nome e obrigatorio")
    @field:Size(max = 120, message = "nome deve ter no maximo 120 caracteres")
    val nome: String,

    @field:NotBlank(message = "email e obrigatorio")
    @field:Email(message = "email invalido")
    @field:Size(max = 150, message = "email deve ter no maximo 150 caracteres")
    val email: String,

    /** Obrigatoria na criacao; no update, nula ou vazia mantem a senha atual. */
    @field:Size(min = 6, max = 100, message = "senha deve ter entre 6 e 100 caracteres")
    val senha: String? = null,

    val role: Role = Role.USER,

    val ativo: Boolean = true,
)

data class UsuarioResponse(
    val id: Long,
    val nome: String,
    val email: String,
    val role: Role,
    val ativo: Boolean,
    val dataCadastro: LocalDateTime,
)
