package br.com.smartparking.api.security

import br.com.smartparking.api.common.ApiError
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.dao.DaoAuthenticationProvider
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

/**
 * Autenticacao stateless por JWT e autorizacao por role (secoes 19 e 20).
 *
 * Regra geral: leitura de blocos e vagas para qualquer usuario autenticado,
 * escrita apenas para ADMIN. Usuarios, dispositivos, dashboard e medicoes sao
 * inteiramente restritos a ADMIN, espelhando as secoes ocultas no frontend.
 */
@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
    private val usuarioDetailsService: UsuarioDetailsService,
    private val objectMapper: ObjectMapper,
) {

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun authenticationProvider(): DaoAuthenticationProvider {
        val provider = DaoAuthenticationProvider(usuarioDetailsService)
        provider.setPasswordEncoder(passwordEncoder())
        return provider
    }

    @Bean
    fun authenticationManager(config: AuthenticationConfiguration): AuthenticationManager =
        config.authenticationManager

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .cors { it.configurationSource(corsConfigurationSource()) }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authenticationProvider(authenticationProvider())
            .exceptionHandling {
                it.authenticationEntryPoint { request, response, _ ->
                    escrever(response, HttpStatus.UNAUTHORIZED, "Token ausente ou invalido", request.requestURI)
                }
                it.accessDeniedHandler { request, response, _ ->
                    escrever(response, HttpStatus.FORBIDDEN, "Acesso negado para este perfil", request.requestURI)
                }
            }
            .authorizeHttpRequests { auth ->
                auth
                    // Rotas publicas de autenticacao; todas as demais exigem token
                    .requestMatchers(HttpMethod.POST, "/api/auth/signin", "/api/auth/signup").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/auth/me").authenticated()
                    .requestMatchers("/actuator/health", "/error").permitAll()
                    .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                    // CRUD de usuarios: somente ADMIN
                    .requestMatchers("/api/usuarios/**").hasRole("ADMIN")
                    // Sensores, dashboard e leituras: somente ADMIN, inclusive na
                    // leitura (as telas equivalentes ficam ocultas para o USER)
                    .requestMatchers("/api/dispositivos/**").hasRole("ADMIN")
                    .requestMatchers("/api/dashboard", "/api/medicoes", "/api/medicoes/**").hasRole("ADMIN")
                    // Escrita nos demais recursos: somente ADMIN
                    .requestMatchers(HttpMethod.POST, "/api/**").hasRole("ADMIN")
                    .requestMatchers(HttpMethod.PUT, "/api/**").hasRole("ADMIN")
                    .requestMatchers(HttpMethod.PATCH, "/api/**").hasRole("ADMIN")
                    .requestMatchers(HttpMethod.DELETE, "/api/**").hasRole("ADMIN")
                    // Leitura: qualquer usuario autenticado (ADMIN ou USER)
                    .requestMatchers(HttpMethod.GET, "/api/**").authenticated()
                    .anyRequest().authenticated()
            }
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }

    /** Libera os frontends KMP (Web, Desktop e Mobile) em ambiente de desenvolvimento. */
    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val config = CorsConfiguration().apply {
            allowedOriginPatterns = listOf("*")
            allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
            allowedHeaders = listOf("*")
            allowCredentials = false
            maxAge = 3600
        }
        return UrlBasedCorsConfigurationSource().apply { registerCorsConfiguration("/**", config) }
    }

    private fun escrever(
        response: jakarta.servlet.http.HttpServletResponse,
        status: HttpStatus,
        mensagem: String,
        caminho: String,
    ) {
        response.status = status.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = "UTF-8"
        val corpo = ApiError(
            status = status.value(),
            erro = status.reasonPhrase,
            mensagem = mensagem,
            caminho = caminho,
        )
        response.writer.write(objectMapper.writeValueAsString(corpo))
    }
}
