package br.com.smartparking.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.smartparking.app.data.AppStore
import br.com.smartparking.app.model.Dispositivo
import br.com.smartparking.app.model.DispositivoRequest
import kotlinx.coroutines.launch

private val TIPOS = listOf("ESP32" to "ESP32", "ESP8266" to "ESP8266", "OUTRO" to "Outro")

private const val TOPICO_PADRAO = "smart-parking/vaga/"

private val STATUS = listOf(
    "ONLINE" to "Online",
    "OFFLINE" to "Offline",
    "MANUTENCAO" to "Manutencao",
)

/** CRUD 3 - Dispositivos IoT. */
@Composable
fun TelaDispositivos(store: AppStore) {
    var emEdicao by remember { mutableStateOf<Dispositivo?>(null) }
    var criando by remember { mutableStateOf(false) }
    var excluindo by remember { mutableStateOf<Dispositivo?>(null) }
    val escopo = rememberCoroutineScope()

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Text("Dispositivos", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
            if (store.dispositivos.isEmpty()) {
                item { Text("Nenhum dispositivo cadastrado.") }
            }
            items(store.dispositivos) { dispositivo ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("${dispositivo.nome} (${dispositivo.tipo})", fontWeight = FontWeight.Bold)
                            Text(dispositivo.macAddress, style = MaterialTheme.typography.bodySmall)
                            Text(dispositivo.mqttTopic, style = MaterialTheme.typography.bodySmall)
                            Text(
                                dispositivo.vagaNumero?.let { "Vaga $it" } ?: "Sem vaga vinculada",
                                style = MaterialTheme.typography.bodySmall,
                            )
                            dispositivo.dataInstalacao?.let {
                                Text("Instalado em: $it", style = MaterialTheme.typography.bodySmall)
                            }
                            dispositivo.ultimaComunicacao?.let {
                                Text("Ultima leitura: $it", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        ChipStatus(dispositivo.status, CoresStatus.deDispositivo(dispositivo.status))
                        if (store.isAdmin) {
                            AcoesLinha(
                                aoEditar = { emEdicao = dispositivo },
                                aoExcluir = { excluindo = dispositivo },
                            )
                        }
                    }
                }
            }
            item { Spacer(Modifier.padding(32.dp)) }
        }

        if (store.isAdmin) {
            ExtendedFloatingActionButton(
                onClick = { criando = true },
                modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp),
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Novo dispositivo") },
            )
        }
    }

    if (criando || emEdicao != null) {
        FormularioDispositivo(
            store = store,
            inicial = emEdicao,
            aoCancelar = { criando = false; emEdicao = null },
            aoSalvar = { request ->
                val alvo = emEdicao
                escopo.launch {
                    val ok = store.executarERecarregar(
                        if (alvo == null) "Dispositivo cadastrado" else "Dispositivo atualizado",
                    ) {
                        if (alvo == null) {
                            store.api.criarDispositivo(request)
                        } else {
                            store.api.atualizarDispositivo(alvo.id, request)
                        }
                    }
                    if (ok) {
                        criando = false
                        emEdicao = null
                    }
                }
            },
        )
    }

    excluindo?.let { alvo ->
        ConfirmarExclusao(
            descricao = "o dispositivo ${alvo.nome}",
            aoConfirmar = {
                escopo.launch {
                    store.executarERecarregar("Dispositivo excluido") { store.api.excluirDispositivo(alvo.id) }
                    excluindo = null
                }
            },
            aoCancelar = { excluindo = null },
        )
    }
}

@Composable
private fun FormularioDispositivo(
    store: AppStore,
    inicial: Dispositivo?,
    aoCancelar: () -> Unit,
    aoSalvar: (DispositivoRequest) -> Unit,
) {
    var nome by remember { mutableStateOf(inicial?.nome ?: "") }
    var mac by remember { mutableStateOf(inicial?.macAddress ?: "") }
    var tipo by remember { mutableStateOf(inicial?.tipo ?: "ESP32") }
    var topico by remember { mutableStateOf(inicial?.mqttTopic ?: TOPICO_PADRAO) }
    var status by remember { mutableStateOf(inicial?.status ?: "OFFLINE") }
    var vagaId by remember { mutableStateOf(inicial?.vagaId ?: 0L) }
    var dataInstalacao by remember { mutableStateOf(inicial?.dataInstalacao ?: "") }

    val vagasDisponiveis = store.vagas.filter { it.dispositivoId == null || it.id == inicial?.vagaId }
    val macValido = Regex("^([0-9A-Fa-f]{2}:){5}[0-9A-Fa-f]{2}$").matches(mac)
    // Data opcional: em branco vai como null para a API.
    val dataValida = dataInstalacao.isBlank() || dataIsoValida(dataInstalacao)

    DialogoFormulario(
        titulo = if (inicial == null) "Novo dispositivo" else "Editar dispositivo",
        gravarHabilitado = nome.isNotBlank() && macValido && topico.isNotBlank() && dataValida,
        aoGravar = {
            aoSalvar(
                DispositivoRequest(
                    nome = nome.trim(),
                    macAddress = mac.trim(),
                    tipo = tipo,
                    mqttTopic = topico.trim(),
                    status = status,
                    vagaId = vagaId.takeIf { it != 0L },
                    dataInstalacao = dataInstalacao.trim().ifBlank { null },
                ),
            )
        },
        aoLimpar = {
            nome = ""
            mac = ""
            tipo = "ESP32"
            topico = TOPICO_PADRAO
            status = "OFFLINE"
            vagaId = 0L
            dataInstalacao = ""
        },
        aoCancelar = aoCancelar,
    ) {
        Campo("Nome (deviceId)", nome, { nome = it }, apoio = "Deve ser igual ao DEVICE_ID do firmware")
        Campo(
            rotulo = "MAC Address",
            valor = mac,
            aoMudar = { mac = it.uppercase() },
            apoio = if (mac.isBlank() || macValido) "Formato AA:BB:CC:DD:EE:FF" else "MAC invalido",
        )
        Seletor("Tipo", TIPOS, tipo, { tipo = it })
        Campo("Topico MQTT", topico, { topico = it }, apoio = "Ex.: smart-parking/vaga/01")
        Seletor("Status", STATUS, status, { status = it })
        Seletor(
            rotulo = "Vaga",
            opcoes = listOf("0" to "Nenhuma") +
                vagasDisponiveis.map { it.id.toString() to "Vaga ${it.numero} (${it.blocoNome})" },
            selecionada = vagaId.toString(),
            aoSelecionar = { vagaId = it.toLong() },
        )
        Campo(
            rotulo = "Data de instalacao",
            valor = dataInstalacao,
            aoMudar = { entrada -> dataInstalacao = entrada.filter { it.isDigit() || it == '-' }.take(10) },
            apoio = if (dataValida) "Formato aaaa-mm-dd (opcional)" else "Data invalida",
        )
    }
}
