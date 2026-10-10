package br.com.smartparking.app.data

import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.cio.CIO

actual fun httpEngineFactory(): HttpClientEngineFactory<*> = CIO

actual fun urlPadraoDaApi(): String = "http://localhost:8085"

actual fun nomeDaPlataforma(): String = "Desktop"
