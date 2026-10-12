package br.com.smartparking.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.smartparking.app.data.AppStore
import br.com.smartparking.app.model.Medicao
import kotlinx.coroutines.delay

/**
 * Historico das leituras recebidas via MQTT (secao 23 da especificacao).
 *
 * E a tela que evidencia, na apresentacao, que o dado saiu do ESP32 e chegou
 * ao banco atraves do broker.
 */
@Composable
fun TelaMedicoes(store: AppStore) {
    val medicoes = remember { mutableStateListOf<Medicao>() }
    var erro by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(store.autenticado) {
        while (store.autenticado) {
            try {
                val novas = store.api.medicoes()
                medicoes.clear()
                medicoes.addAll(novas)
                erro = null
            } catch (e: Exception) {
                erro = e.message
            }
            delay(3000)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Text("Leituras dos sensores", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                "Atualiza a cada 3 segundos com o que o backend recebeu do broker MQTT.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        erro?.let { item { Banner(it, CoresStatus.ocupada) { erro = null } } }
        if (medicoes.isEmpty()) {
            item { Text("Nenhuma leitura recebida ate agora.") }
        }
        items(medicoes) { medicao ->
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(medicao.dispositivoNome, fontWeight = FontWeight.Bold)
                        Text(
                            medicao.vagaNumero?.let { "Vaga $it" } ?: "Sem vaga vinculada",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Text(medicao.dataHora, style = MaterialTheme.typography.bodySmall)
                    }
                    Text("${medicao.distancia} cm", modifier = Modifier.padding(end = 12.dp))
                    ChipStatus(medicao.status, CoresStatus.deVaga(medicao.status))
                }
            }
        }
    }
}
