package br.com.smartparking.api.domain

/** Situacao administrativa de um bloco do estacionamento. */
enum class StatusBloco { ATIVO, INATIVO }

/** Categoria da vaga, conforme secao 16 da especificacao. */
enum class TipoVaga { NORMAL, PCD, IDOSO, MOTO }

/**
 * Situacao da vaga.
 *
 * MANUTENCAO e um estado administrativo: enquanto a vaga estiver nele o sensor
 * nao altera o status (ver [br.com.smartparking.api.service.VagaService.aplicarLeituraDoSensor]).
 */
enum class StatusVaga { LIVRE, OCUPADA, MANUTENCAO }

/** Tipo de hardware do dispositivo IoT. */
enum class TipoDispositivo { ESP32, ESP8266, OUTRO }

/** Situacao de conectividade do dispositivo IoT. */
enum class StatusDispositivo { ONLINE, OFFLINE, MANUTENCAO }

/** Perfil de acesso usado na autorizacao (secao 20 da especificacao). */
enum class Role { ADMIN, USER }
