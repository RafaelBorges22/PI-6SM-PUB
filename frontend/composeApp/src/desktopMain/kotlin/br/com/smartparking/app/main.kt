package br.com.smartparking.app

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

/** Entrada do aplicativo Desktop (JVM). */
fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Smart Parking - Monitoramento",
        state = rememberWindowState(size = DpSize(1200.dp, 820.dp)),
    ) {
        App()
    }
}
