package br.com.smartparking.app.data

import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.okhttp.OkHttp

actual fun httpEngineFactory(): HttpClientEngineFactory<*> = OkHttp

// 10.0.2.2 e o host da maquina visto de dentro do emulador Android.
// Em celular fisico, trocar pelo IP da maquina que roda o backend.
actual fun urlPadraoDaApi(): String = "http://10.0.2.2:8085"

actual fun nomeDaPlataforma(): String = "Android"
