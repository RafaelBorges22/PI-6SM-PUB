package br.com.smartparking.api.web

import br.com.smartparking.api.service.AuthService
import br.com.smartparking.api.service.UsuarioService
import br.com.smartparking.api.web.dto.SigninRequest
import br.com.smartparking.api.web.dto.SigninResponse
import br.com.smartparking.api.web.dto.SignupRequest
import br.com.smartparking.api.web.dto.UsuarioResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.net.URI

/** Autenticacao JWT (secao 19 da especificacao). `/signin` e `/signup` sao publicas. */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticacao", description = "Signin, signup e emissao de token JWT")
class AuthController(
    private val authService: AuthService,
    private val usuarioService: UsuarioService,
) {

    @PostMapping("/signin")
    @Operation(summary = "Autentica por email/senha e devolve o token JWT")
    fun signin(@Valid @RequestBody request: SigninRequest): SigninResponse = authService.signin(request)

    @PostMapping("/signup")
    @Operation(summary = "Cadastra um novo usuario com perfil USER")
    fun signup(@Valid @RequestBody request: SignupRequest): ResponseEntity<UsuarioResponse> {
        val criado = authService.signup(request)
        return ResponseEntity.created(URI.create("/api/usuarios/${criado.id}")).body(criado)
    }

    @GetMapping("/me")
    @Operation(summary = "Dados do usuario dono do token informado")
    fun me(@AuthenticationPrincipal userDetails: UserDetails): UsuarioResponse =
        usuarioService.buscarPorEmail(userDetails.username)
}
