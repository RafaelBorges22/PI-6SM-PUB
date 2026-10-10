package br.com.smartparking.api.web

import br.com.smartparking.api.domain.StatusVaga
import br.com.smartparking.api.domain.TipoVaga
import br.com.smartparking.api.service.VagaService
import br.com.smartparking.api.web.dto.VagaRequest
import br.com.smartparking.api.web.dto.VagaResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.net.URI

/** CRUD 2 - Vagas. */
@RestController
@RequestMapping("/api/vagas")
@Tag(name = "Vagas", description = "CRUD 2 - vagas monitoradas pelos sensores")
class VagaController(private val service: VagaService) {

    @GetMapping
    @Operation(summary = "Lista vagas, opcionalmente filtrando por status, bloco e tipo")
    fun listar(
        @RequestParam(required = false) status: StatusVaga?,
        @RequestParam(required = false) blocoId: Long?,
        @RequestParam(required = false) tipo: TipoVaga?,
    ): List<VagaResponse> = service.listar(status, tipo, blocoId)

    @GetMapping("/buscar")
    @Operation(summary = "Busca parcial de vagas pela observacao (JPQL + LIKE)")
    fun buscarPorObservacao(@RequestParam observacao: String): List<VagaResponse> =
        service.buscarPorObservacao(observacao)

    @GetMapping("/{id}")
    @Operation(summary = "Busca uma vaga por id")
    fun buscarPorId(@PathVariable id: Long): VagaResponse = service.buscarPorId(id)

    @PostMapping
    @Operation(summary = "Cria uma vaga (ADMIN)")
    fun criar(@Valid @RequestBody request: VagaRequest): ResponseEntity<VagaResponse> {
        val criada = service.criar(request)
        return ResponseEntity.created(URI.create("/api/vagas/${criada.id}")).body(criada)
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza uma vaga (ADMIN)")
    fun atualizar(@PathVariable id: Long, @Valid @RequestBody request: VagaRequest): VagaResponse =
        service.atualizar(id, request)

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Exclui uma vaga (ADMIN)")
    fun excluir(@PathVariable id: Long) = service.excluir(id)
}
