package br.com.smartparking.api.security

import br.com.smartparking.api.domain.Usuario
import io.jsonwebtoken.Claims
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.slf4j.LoggerFactory
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.util.Date
import javax.crypto.SecretKey

/**
 * Biblioteca utilitaria de JWT: gera tokens, valida tokens e le o usuario
 * (email, no `subject`) de um token validado.
 *
 * O algoritmo HMAC e escolhido pela jjwt conforme o tamanho do segredo:
 * >= 32 bytes -> HS256, >= 48 -> HS384, >= 64 -> HS512.
 */
@Component
class JwtUtil(private val properties: JwtProperties) {

    private val log = LoggerFactory.getLogger(javaClass)

    private val chave: SecretKey = Keys.hmacShaKeyFor(properties.secret.toByteArray(StandardCharsets.UTF_8))

    val expiracaoSegundos: Long get() = properties.expiracaoSegundos

    fun gerarToken(usuario: Usuario): String {
        val agora = System.currentTimeMillis()
        return Jwts.builder()
            .subject(usuario.email)
            .issuer(properties.emissor)
            .claim("uid", usuario.id)
            .claim("nome", usuario.nome)
            .claim("role", usuario.role.name)
            .issuedAt(Date(agora))
            .expiration(Date(agora + properties.expiracaoSegundos * 1000))
            .signWith(chave)
            .compact()
    }

    /** Devolve as claims do token, ou `null` se ele for invalido, expirado ou adulterado. */
    fun extrairClaims(token: String): Claims? = try {
        Jwts.parser()
            .verifyWith(chave)
            .requireIssuer(properties.emissor)
            .build()
            .parseSignedClaims(token)
            .payload
    } catch (e: JwtException) {
        log.debug("Token JWT rejeitado: {}", e.message)
        null
    } catch (e: IllegalArgumentException) {
        log.debug("Token JWT malformado: {}", e.message)
        null
    }

    /** Token com assinatura, emissor e validade corretos. */
    fun validarToken(token: String): Boolean = extrairClaims(token) != null

    /** Token valido e pertencente ao usuario informado. */
    fun validarToken(token: String, userDetails: UserDetails): Boolean =
        extrairUsuario(token)?.equals(userDetails.username, ignoreCase = true) == true

    /** Le o usuario (email) de um token valido; `null` se o token for invalido. */
    fun extrairUsuario(token: String): String? = extrairClaims(token)?.subject
}
