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
import br.com.smartparking.app.model.Bloco
import br.com.smartparking.app.model.BlocoRequest
import kotlinx.coroutines.launch

/** CRUD 1 - Blocos. */
@Composable
fun TelaBlocos(store: AppStore) {
    var emEdicao by remember { mutableStateOf<Bloco?>(null) }
    var criando by remember { mutableStateOf(false) }
    var excluindo by remember { mutableStateOf<Bloco?>(null) }
    val escopo = rememberCoroutineScope()

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Text("Blocos", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
            if (store.blocos.isEmpty()) {
                item { Text("Nenhum bloco cadastrado.") }
            }
            items(store.blocos) { bloco ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(bloco.nome, fontWeight = FontWeight.Bold)
                            bloco.descricao?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                            Text(
                                "${bloco.localizacao ?: "sem localizacao"} - ${bloco.totalVagas} vaga(s)",
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Text(
                                "Capacidade: ${bloco.capacidade} - ${if (bloco.coberto) "Coberto" else "Descoberto"}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        ChipStatus(bloco.status, if (bloco.status == "ATIVO") CoresStatus.livre else CoresStatus.offline)
                        if (store.isAdmin) {
                            AcoesLinha(aoEditar = { emEdicao = bloco }, aoExcluir = { excluindo = bloco })
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
                text = { Text("Novo bloco") },
            )
        }
    }

    if (criando || emEdicao != null) {
        FormularioBloco(
            inicial = emEdicao,
            aoCancelar = { criando = false; emEdicao = null },
            aoSalvar = { request ->
                val alvo = emEdicao
                escopo.launch {
                    val ok = store.executarERecarregar(if (alvo == null) "Bloco criado" else "Bloco atualizado") {
                        if (alvo == null) store.api.criarBloco(request) else store.api.atualizarBloco(alvo.id, request)
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
            descricao = "o bloco ${alvo.nome}",
            aoConfirmar = {
                escopo.launch {
                    store.executarERecarregar("Bloco excluido") { store.api.excluirBloco(alvo.id) }
                    excluindo = null
                }
            },
            aoCancelar = { excluindo = null },
        )
    }
}

@Composable
private fun FormularioBloco(inicial: Bloco?, aoCancelar: () -> Unit, aoSalvar: (BlocoRequest) -> Unit) {
    var nome by remember { mutableStateOf(inicial?.nome ?: "") }
    var descricao by remember { mutableStateOf(inicial?.descricao ?: "") }
    var localizacao by remember { mutableStateOf(inicial?.localizacao ?: "") }
    var status by remember { mutableStateOf(inicial?.status ?: "ATIVO") }
    var capacidade by remember { mutableStateOf(inicial?.capacidade?.toString() ?: "") }
    var coberto by remember { mutableStateOf(inicial?.coberto ?: false) }

    DialogoFormulario(
        titulo = if (inicial == null) "Novo bloco" else "Editar bloco",
        gravarHabilitado = nome.isNotBlank() && capacidade.toIntOrNull() != null,
        aoGravar = {
            aoSalvar(
                BlocoRequest(
                    nome = nome.trim(),
                    descricao = descricao.trim().ifBlank { null },
                    localizacao = localizacao.trim().ifBlank { null },
                    status = status,
                    capacidade = capacidade.toIntOrNull() ?: 0,
                    coberto = coberto,
                ),
            )
        },
        aoLimpar = {
            nome = ""
            descricao = ""
            localizacao = ""
            status = "ATIVO"
            capacidade = ""
            coberto = false
        },
        aoCancelar = aoCancelar,
    ) {
        Campo("Nome", nome, { nome = it })
        Campo("Descricao", descricao, { descricao = it })
        Campo("Localizacao", localizacao, { localizacao = it })
        Campo(
            rotulo = "Capacidade",
            valor = capacidade,
            aoMudar = { entrada -> capacidade = entrada.filter { it.isDigit() } },
            apoio = "Quantidade maxima de vagas do bloco",
        )
        CampoInterruptor("Coberto", coberto, { coberto = it })
        Seletor(
            rotulo = "Status",
            opcoes = listOf("ATIVO" to "Ativo", "INATIVO" to "Inativo"),
            selecionada = status,
            aoSelecionar = { status = it },
        )
    }
}
