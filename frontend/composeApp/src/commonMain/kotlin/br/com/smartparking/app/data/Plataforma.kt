package br.com.smartparking.app.data

import io.ktor.client.engine.HttpClientEngineFactory

/** Engine HTTP do Ktor especifica de cada alvo (OkHttp, CIO e o fetch do navegador). */
expect fun httpEngineFactory(): HttpClientEngineFactory<*>

/** Endereco padrao da API sugerido em cada plataforma. */
expect fun urlPadraoDaApi(): String

/** Nome do alvo, exibido no rodape da aplicacao. */
expect fun nomeDaPlataforma(): String
