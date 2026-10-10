package br.com.smartparking.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.smartparking.app.data.AppStore
import br.com.smartparking.app.model.Vaga

/**
 * Dashboard das secoes 25 e 26 da especificacao: totais, taxa de ocupacao e o
 * mapa de vagas por bloco.
 */
@Composable
fun TelaDashboard(store: AppStore) {
    val dashboard = store.dashboard

    LazyColumn(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("Visao geral", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }

        if (dashboard == null) {
            item { Text("Carregando os dados do estacionamento...") }
            return@LazyColumn
        }

        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Indicador("Blocos", dashboard.totalBlocos.toString(), MaterialTheme.colorScheme.primary)
                Indicador("Total de vagas", dashboard.totalVagas.toString(), MaterialTheme.colorScheme.primary)
                Indicador("Livres", dashboard.livres.toString(), CoresStatus.livre)
                Indicador("Ocupadas", dashboard.ocupadas.toString(), CoresStatus.ocupada)
                Indicador("Manutencao", dashboard.manutencao.toString(), CoresStatus.manutencao)
                Indicador("Sensores online", dashboard.dispositivosOnline.toString(), CoresStatus.online)
                Indicador("Sensores offline", dashboard.dispositivosOffline.toString(), CoresStatus.offline)
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Ocupacao: ${dashboard.taxaOcupacaoPercentual}%", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { dashboard.taxaOcupacaoPercentual / 100f },
                        modifier = Modifier.fillMaxWidth().height(10.dp),
                    )
                }
            }
        }

        item {
            Text("Vagas por bloco", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        items(dashboard.blocos) { bloco ->
            val vagasDoBloco = store.vagas.filter { it.blocoId == bloco.blocoId }.sortedBy { it.numero }
            CartaoBloco(
                nome = bloco.nome,
                livres = bloco.livres,
                ocupadas = bloco.ocupadas,
                manutencao = bloco.manutencao,
                vagas = vagasDoBloco,
            )
        }
    }
}

@Composable
private fun Indicador(rotulo: String, valor: String, cor: Color) {
    ElevatedCard(modifier = Modifier.widthIn(min = 140.dp).padding(vertical = 4.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(rotulo, style = MaterialTheme.typography.labelMedium)
            Text(valor, style = MaterialTheme.typography.headlineMedium, color = cor, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun CartaoBloco(nome: String, livres: Int, ocupadas: Int, manutencao: Int, vagas: List<Vaga>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(nome, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text("$livres livres / $ocupadas ocupadas / $manutencao manutencao")
            }
            Spacer(Modifier.height(10.dp))
            if (vagas.isEmpty()) {
                Text("Nenhuma vaga cadastrada neste bloco.")
            } else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    vagas.forEach { vaga ->
                        ChipStatus(
                            texto = "Vaga ${vaga.numero} - ${vaga.status}",
                            cor = CoresStatus.deVaga(vaga.status),
                            modifier = Modifier.padding(vertical = 4.dp),
                        )
                    }
                }
            }
        }
    }
}
