package br.com.smartparking.api.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/** Swagger UI em `/swagger-ui.html`, com o botao Authorize para colar o JWT. */
@Configuration
class OpenApiConfig {

    @Bean
    fun openApi(): OpenAPI = OpenAPI()
        .info(
            Info()
                .title("Smart Parking API")
                .version("1.0.0")
                .description(
                    "API REST do Smart Parking: CRUDs de blocos, vagas, dispositivos e usuarios, " +
                        "autenticacao JWT e consumo das leituras publicadas pelos ESP32 via MQTT.",
                ),
        )
        .components(
            Components().addSecuritySchemes(
                ESQUEMA_JWT,
                SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT"),
            ),
        )
        .addSecurityItem(SecurityRequirement().addList(ESQUEMA_JWT))

    private companion object {
        const val ESQUEMA_JWT = "bearerAuth"
    }
}
