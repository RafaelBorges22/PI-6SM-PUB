package br.com.smartparking.api.web

import br.com.smartparking.api.service.DashboardService
import br.com.smartparking.api.service.DispositivoService
import br.com.smartparking.api.web.dto.DispositivoRequest
import br.com.smartparking.api.web.dto.DispositivoResponse
import br.com.smartparking.api.web.dto.MedicaoResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.net.URI

/** CRUD 3 - Dispositivos IoT. */
@RestController
@RequestMapping("/api/dispositivos")
@Tag(name = "Dispositivos", description = "CRUD 3 - dispositivos ESP32 instalados nas vagas")
class DispositivoController(
    private val service: DispositivoService,
    private val dashboardService: DashboardService,
) {

    @GetMapping
    @Operation(summary = "Lista todos os dispositivos")
    fun listar(): List<DispositivoResponse> = service.listar()

    @GetMapping("/buscar")
    @Operation(summary = "Busca parcial de dispositivos pelo nome (JPQL + LIKE)")
    fun buscarPorNome(@RequestParam nome: String): List<DispositivoResponse> = service.buscarPorNome(nome)

    @GetMapping("/{id}")
    @Operation(summary = "Busca um dispositivo por id")
    fun buscarPorId(@PathVariable id: Long): DispositivoResponse = service.buscarPorId(id)

    @GetMapping("/{id}/medicoes")
    @Operation(summary = "Ultimas 50 medicoes recebidas do dispositivo via MQTT")
    fun medicoes(@PathVariable id: Long): List<MedicaoResponse> = dashboardService.medicoesDoDispositivo(id)

    @PostMapping
    @Operation(summary = "Cadastra um dispositivo (ADMIN)")
    fun criar(@Valid @RequestBody request: DispositivoRequest): ResponseEntity<DispositivoResponse> {
        val criado = service.criar(request)
        return ResponseEntity.created(URI.create("/api/dispositivos/${criado.id}")).body(criado)
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza um dispositivo (ADMIN)")
    fun atualizar(
        @PathVariable id: Long,
        @Valid @RequestBody request: DispositivoRequest,
    ): DispositivoResponse = service.atualizar(id, request)

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Exclui um dispositivo (ADMIN)")
    fun excluir(@PathVariable id: Long) = service.excluir(id)
}
