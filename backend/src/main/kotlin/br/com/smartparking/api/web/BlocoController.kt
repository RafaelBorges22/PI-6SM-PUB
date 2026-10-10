package br.com.smartparking.api.web

import br.com.smartparking.api.service.BlocoService
import br.com.smartparking.api.web.dto.BlocoComVagasResponse
import br.com.smartparking.api.web.dto.BlocoRequest
import br.com.smartparking.api.web.dto.BlocoResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.net.URI

/** CRUD 1 - Blocos. */
@RestController
@RequestMapping("/api/blocos")
@Tag(name = "Blocos", description = "CRUD 1 - areas/setores do estacionamento")
class BlocoController(private val service: BlocoService) {

    @GetMapping
    @Operation(summary = "Lista todos os blocos")
    fun listar(): List<BlocoResponse> = service.listar()

    @GetMapping("/buscar")
    @Operation(summary = "Busca parcial de blocos pelo nome (JPQL + LIKE)")
    fun buscarPorNome(@RequestParam nome: String): List<BlocoResponse> = service.buscarPorNome(nome)

    @GetMapping("/{id}")
    @Operation(summary = "Busca um bloco por id")
    fun buscarPorId(@PathVariable id: Long): BlocoResponse = service.buscarPorId(id)

    @GetMapping("/{id}/vagas")
    @Operation(summary = "Lista as vagas do bloco com o resumo de ocupacao")
    fun buscarComVagas(@PathVariable id: Long): BlocoComVagasResponse = service.buscarComVagas(id)

    @PostMapping
    @Operation(summary = "Cria um bloco (ADMIN)")
    fun criar(@Valid @RequestBody request: BlocoRequest): ResponseEntity<BlocoResponse> {
        val criado = service.criar(request)
        return ResponseEntity.created(URI.create("/api/blocos/${criado.id}")).body(criado)
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza um bloco (ADMIN)")
    fun atualizar(@PathVariable id: Long, @Valid @RequestBody request: BlocoRequest): BlocoResponse =
        service.atualizar(id, request)

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Exclui um bloco sem vagas vinculadas (ADMIN)")
    fun excluir(@PathVariable id: Long) = service.excluir(id)
}
