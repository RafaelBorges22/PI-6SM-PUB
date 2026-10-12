package br.com.smartparking.api.security

import br.com.smartparking.api.domain.Usuario
import br.com.smartparking.api.repository.UsuarioRepository
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** Carrega o usuario do banco para o Spring Security a partir do email. */
@Service
class UsuarioDetailsService(private val usuarioRepository: UsuarioRepository) : UserDetailsService {

    @Transactional(readOnly = true)
    override fun loadUserByUsername(username: String): UserDetails {
        val usuario = usuarioRepository.findByEmailIgnoreCase(username)
            ?: throw UsernameNotFoundException("Usuario nao encontrado: $username")
        return paraUserDetails(usuario)
    }

    fun paraUserDetails(usuario: Usuario): UserDetails = User.builder()
        .username(usuario.email)
        .password(usuario.senhaHash)
        .disabled(!usuario.ativo)
        .authorities(listOf(SimpleGrantedAuthority("ROLE_${usuario.role.name}")))
        .build()
}
