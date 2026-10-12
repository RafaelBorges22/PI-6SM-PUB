package br.com.smartparking.api.security

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Configuracao do JWT (`smartparking.jwt.*` no application.yml).
 *
 * O segredo precisa ter no minimo 32 bytes (HMAC-SHA); segredos maiores fazem a
 * jjwt usar automaticamente HS384 ou HS512.
 */
@ConfigurationProperties(prefix = "smartparking.jwt")
data class JwtProperties(
    val secret: String,
    val expiracaoSegundos: Long = 28_800, // 8 horas
    val emissor: String = "smart-parking",
)
