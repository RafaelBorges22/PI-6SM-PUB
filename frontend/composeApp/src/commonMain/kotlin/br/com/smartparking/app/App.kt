package br.com.smartparking.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import br.com.smartparking.app.data.AppStore
import br.com.smartparking.app.data.nomeDaPlataforma
import br.com.smartparking.app.ui.Banner
import br.com.smartparking.app.ui.CoresStatus
import br.com.smartparking.app.ui.TelaBlocos
import br.com.smartparking.app.ui.TelaDashboard
import br.com.smartparking.app.ui.TelaDispositivos
import br.com.smartparking.app.ui.TelaLogin
import br.com.smartparking.app.ui.TelaMedicoes
import br.com.smartparking.app.ui.TelaUsuarios
import br.com.smartparking.app.ui.TelaVagas
import br.com.smartparking.app.ui.TemaSmartParking
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class Seccao(val titulo: String, val icone: ImageVector, val somenteAdmin: Boolean = false) {
    DASHBOARD("Dashboard", Icons.Default.Dashboard, somenteAdmin = true),
    BLOCOS("Blocos", Icons.Default.ViewModule),
    VAGAS("Vagas", Icons.Default.DirectionsCar),
    DISPOSITIVOS("Sensores", Icons.Default.DeveloperBoard, somenteAdmin = true),
    MEDICOES("Leituras", Icons.Default.Sensors, somenteAdmin = true),
    USUARIOS("Usuarios", Icons.Default.Group, somenteAdmin = true),
}

/** Secoes liberadas para o perfil: USER enxerga apenas Blocos e Vagas. */
private fun seccoesDe(isAdmin: Boolean): List<Seccao> =
    Seccao.entries.filter { !it.somenteAdmin || isAdmin }

/** Intervalo do polling que mantem o dashboard sincronizado com o backend. */
private const val INTERVALO_ATUALIZACAO_MS = 5000L

/**
 * Raiz compartilhada por Mobile, Web e Desktop.
 *
 * Sem token exibe o login; autenticado exibe a navegacao filtrada pelo perfil:
 * o ADMIN ve tudo, enquanto o USER fica restrito a Blocos e Vagas (Dashboard,
 * Sensores, Leituras e Usuarios ficam ocultos).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    val store = remember { AppStore() }
    var seccao by remember { mutableStateOf(Seccao.DASHBOARD) }

    TemaSmartParking {
        if (!store.autenticado) {
            TelaLogin(store)
            return@TemaSmartParking
        }

        // Enquanto a sessao estiver ativa, recarrega periodicamente para refletir
        // as mudancas de status publicadas pelos ESP32.
        LaunchedEffect(store.autenticado) {
            while (store.autenticado) {
                delay(INTERVALO_ATUALIZACAO_MS)
                store.carregarTudo(silencioso = true)
            }
        }

        val escopo = androidx.compose.runtime.rememberCoroutineScope()
        val seccoes = seccoesDe(store.isAdmin)
        // Se a secao lembrada nao pertence ao perfil atual (troca de ADMIN para
        // USER na mesma sessao), cai na primeira liberada.
        val seccaoAtual = if (seccao in seccoes) seccao else seccoes.first()

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Smart Parking - ${seccaoAtual.titulo}") },
                    actions = {
                        IconButton(onClick = { escopo.launch { store.carregarTudo() } }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Atualizar")
                        }
                        IconButton(onClick = { store.sair() }) {
                            Icon(Icons.Default.ExitToApp, contentDescription = "Sair")
                        }
                    },
                )
            },
            bottomBar = {
                NavigationBar {
                    seccoes.forEach { alvo ->
                        NavigationBarItem(
                            selected = seccaoAtual == alvo,
                            onClick = { seccao = alvo },
                            icon = { Icon(alvo.icone, contentDescription = alvo.titulo) },
                            label = { Text(alvo.titulo) },
                        )
                    }
                }
            },
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding)) {
                if (store.carregando) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                }
                store.mensagemErro?.let {
                    Banner(it, CoresStatus.ocupada) { store.mensagemErro = null }
                }
                store.mensagemSucesso?.let {
                    Banner(it, CoresStatus.livre) { store.mensagemSucesso = null }
                }

                Box(Modifier.weight(1f)) {
                    when (seccaoAtual) {
                        Seccao.DASHBOARD -> TelaDashboard(store)
                        Seccao.BLOCOS -> TelaBlocos(store)
                        Seccao.VAGAS -> TelaVagas(store)
                        Seccao.DISPOSITIVOS -> TelaDispositivos(store)
                        Seccao.MEDICOES -> TelaMedicoes(store)
                        Seccao.USUARIOS -> TelaUsuarios(store)
                    }
                }

                Text(
                    "${store.nomeUsuario} (${store.role}) - ${nomeDaPlataforma()} - ${store.urlApi}",
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}
