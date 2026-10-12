package br.com.smartparking.api.web

import br.com.smartparking.api.service.UsuarioService
import br.com.smartparking.api.web.dto.UsuarioRequest
import br.com.smartparking.api.web.dto.UsuarioResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.net.URI

/** CRUD 4 - Usuarios. Toda a rota exige perfil ADMIN (ver SecurityConfig). */
@RestController
@RequestMapping("/api/usuarios")
@Tag(name = "Usuarios", description = "CRUD 4 - usuarios e perfis de acesso")
class UsuarioController(private val service: UsuarioService) {

    @GetMapping
    @Operation(summary = "Lista todos os usuarios (ADMIN)")
    fun listar(): List<UsuarioResponse> = service.listar()

    @GetMapping("/buscar")
    @Operation(summary = "Busca parcial de usuarios pelo nome (JPQL + LIKE) (ADMIN)")
    fun buscarPorNome(@RequestParam nome: String): List<UsuarioResponse> = service.buscarPorNome(nome)

    @GetMapping("/{id}")
    @Operation(summary = "Busca um usuario por id (ADMIN)")
    fun buscarPorId(@PathVariable id: Long): UsuarioResponse = service.buscarPorId(id)

    @PostMapping
    @Operation(summary = "Cria um usuario (ADMIN)")
    fun criar(@Valid @RequestBody request: UsuarioRequest): ResponseEntity<UsuarioResponse> {
        val criado = service.criar(request)
        return ResponseEntity.created(URI.create("/api/usuarios/${criado.id}")).body(criado)
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza um usuario; senha em branco mantem a atual (ADMIN)")
    fun atualizar(@PathVariable id: Long, @Valid @RequestBody request: UsuarioRequest): UsuarioResponse =
        service.atualizar(id, request)

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Exclui um usuario (ADMIN)")
    fun excluir(@PathVariable id: Long) = service.excluir(id)
}
