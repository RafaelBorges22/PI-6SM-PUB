package br.com.smartparking.api.web

import br.com.smartparking.api.service.DashboardService
import br.com.smartparking.api.web.dto.DashboardResponse
import br.com.smartparking.api.web.dto.MedicaoResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/** Dados agregados consumidos pelos dashboards de Web, Mobile e Desktop. */
@RestController
@RequestMapping("/api")
@Tag(name = "Dashboard", description = "Resumo de ocupacao e historico de medicoes")
class DashboardController(private val service: DashboardService) {

    @GetMapping("/dashboard")
    @Operation(summary = "Resumo de blocos, vagas, ocupacao e dispositivos")
    fun resumo(): DashboardResponse = service.resumo()

    @GetMapping("/medicoes")
    @Operation(summary = "Ultimas 100 medicoes recebidas via MQTT")
    fun medicoes(): List<MedicaoResponse> = service.ultimasMedicoes()

    @GetMapping("/medicoes/buscar")
    @Operation(summary = "Busca medicoes pelo nome parcial do dispositivo (JPQL + LIKE)")
    fun buscarMedicoes(@RequestParam dispositivo: String): List<MedicaoResponse> =
        service.buscarMedicoesPorDispositivo(dispositivo)
}
