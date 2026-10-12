package br.com.smartparking.api.service

import br.com.smartparking.api.mapper.UsuarioMapper
import br.com.smartparking.api.repository.UsuarioRepository
import br.com.smartparking.api.security.JwtUtil
import br.com.smartparking.api.web.dto.SigninRequest
import br.com.smartparking.api.web.dto.SigninResponse
import br.com.smartparking.api.web.dto.SignupRequest
import br.com.smartparking.api.web.dto.UsuarioResponse
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.DisabledException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.AuthenticationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** Autenticacao por email/senha com emissao de JWT (secao 19 da especificacao). */
@Service
@Transactional(readOnly = true)
class AuthService(
    private val authenticationManager: AuthenticationManager,
    private val usuarioRepository: UsuarioRepository,
    private val usuarioService: UsuarioService,
    private val usuarioMapper: UsuarioMapper,
    private val jwtUtil: JwtUtil,
) {

    /**
     * Valida as credenciais pelo [AuthenticationManager] do Spring Security
     * (DaoAuthenticationProvider + PasswordEncoder) e, se forem validas, emite o JWT.
     */
    fun signin(request: SigninRequest): SigninResponse {
        try {
            authenticationManager.authenticate(UsernamePasswordAuthenticationToken(request.email, request.senha))
        } catch (e: DisabledException) {
            throw BadCredentialsException("Usuario inativo")
        } catch (e: AuthenticationException) {
            throw BadCredentialsException(CREDENCIAIS_INVALIDAS)
        }

        val usuario = usuarioRepository.findByEmailIgnoreCase(request.email)
            ?: throw BadCredentialsException(CREDENCIAIS_INVALIDAS)
        return usuarioMapper.paraSigninResponse(usuario, jwtUtil.gerarToken(usuario), jwtUtil.expiracaoSegundos)
    }

    @Transactional
    fun signup(request: SignupRequest): UsuarioResponse = usuarioService.cadastrar(request)

    private companion object {
        // Mensagem unica para email inexistente e senha errada, para nao revelar quais emails existem.
        const val CREDENCIAIS_INVALIDAS = "Email ou senha invalidos"
    }
}
