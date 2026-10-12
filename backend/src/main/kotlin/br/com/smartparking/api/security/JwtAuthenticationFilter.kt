package br.com.smartparking.api.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Le o cabecalho `Authorization: Bearer <token>` e popula o SecurityContext.
 *
 * Nao rejeita a requisicao por conta propria: quando nao ha autenticacao valida,
 * quem responde 401/403 e a cadeia do Spring Security.
 */
@Component
class JwtAuthenticationFilter(
    private val jwtUtil: JwtUtil,
    private val usuarioDetailsService: UsuarioDetailsService,
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val header = request.getHeader("Authorization")
        if (header == null || !header.startsWith(PREFIXO_BEARER)) {
            filterChain.doFilter(request, response)
            return
        }

        val token = header.removePrefix(PREFIXO_BEARER).trim()
        val email = jwtUtil.extrairUsuario(token)
        if (email != null && SecurityContextHolder.getContext().authentication == null) {
            try {
                val userDetails = usuarioDetailsService.loadUserByUsername(email)
                if (userDetails.isEnabled && jwtUtil.validarToken(token, userDetails)) {
                    val auth = UsernamePasswordAuthenticationToken(userDetails, null, userDetails.authorities)
                    auth.details = WebAuthenticationDetailsSource().buildDetails(request)
                    SecurityContextHolder.getContext().authentication = auth
                }
            } catch (e: UsernameNotFoundException) {
                logger.debug("Token valido para usuario inexistente: ${e.message}")
            }
        }

        filterChain.doFilter(request, response)
    }

    private companion object {
        const val PREFIXO_BEARER = "Bearer "
    }
}
