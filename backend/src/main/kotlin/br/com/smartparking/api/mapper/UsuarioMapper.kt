package br.com.smartparking.api.mapper

import br.com.smartparking.api.domain.Role
import br.com.smartparking.api.domain.Usuario
import br.com.smartparking.api.web.dto.SignupRequest
import br.com.smartparking.api.web.dto.SigninResponse
import br.com.smartparking.api.web.dto.UsuarioRequest
import br.com.smartparking.api.web.dto.UsuarioResponse
import org.springframework.stereotype.Component

/**
 * Conversoes entre a entidade [Usuario] e seus DTOs.
 *
 * Recebe a senha ja convertida em hash: o mapper nao conhece o PasswordEncoder.
 */
@Component
class UsuarioMapper {

    fun paraEntidade(request: UsuarioRequest, senhaHash: String): Usuario = Usuario(
        nome = request.nome.trim(),
        email = request.email.lowercase(),
        senhaHash = senhaHash,
        role = request.role,
        ativo = request.ativo,
    )

    /** Autocadastro pelo `/signup`: sempre USER e ativo. */
    fun paraEntidade(request: SignupRequest, senhaHash: String): Usuario = Usuario(
        nome = request.nome.trim(),
        email = request.email.lowercase(),
        senhaHash = senhaHash,
        role = Role.USER,
        ativo = true,
    )

    /** A senha e tratada pelo servico, pois em branco significa "manter a atual". */
    fun atualizarEntidade(usuario: Usuario, request: UsuarioRequest) {
        usuario.nome = request.nome.trim()
        usuario.email = request.email.lowercase()
        usuario.role = request.role
        usuario.ativo = request.ativo
    }

    fun paraResponse(usuario: Usuario): UsuarioResponse = UsuarioResponse(
        id = usuario.id!!,
        nome = usuario.nome,
        email = usuario.email,
        role = usuario.role,
        ativo = usuario.ativo,
        dataCadastro = usuario.dataCadastro,
    )

    fun paraSigninResponse(usuario: Usuario, token: String, expiraEmSegundos: Long): SigninResponse = SigninResponse(
        token = token,
        expiraEmSegundos = expiraEmSegundos,
        usuarioId = usuario.id!!,
        nome = usuario.nome,
        email = usuario.email,
        role = usuario.role,
    )
}
