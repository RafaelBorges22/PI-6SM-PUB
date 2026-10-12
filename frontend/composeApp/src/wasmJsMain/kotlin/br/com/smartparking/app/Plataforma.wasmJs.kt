package br.com.smartparking.app.data

import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.js.Js

actual fun httpEngineFactory(): HttpClientEngineFactory<*> = Js

actual fun urlPadraoDaApi(): String = "http://localhost:8085"

actual fun nomeDaPlataforma(): String = "Web"
