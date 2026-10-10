package br.com.smartparking.app.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.smartparking.app.data.AppStore
import br.com.smartparking.app.model.Bloco
import br.com.smartparking.app.model.Vaga
import br.com.smartparking.app.model.VagaRequest
import kotlinx.coroutines.launch

private val TIPOS = listOf(
    "NORMAL" to "Normal",
    "PCD" to "PCD",
    "IDOSO" to "Idoso",
    "MOTO" to "Moto",
)

private val STATUS = listOf(
    "LIVRE" to "Livre",
    "OCUPADA" to "Ocupada",
    "MANUTENCAO" to "Manutencao",
)

/** Diametro do circulo que representa uma vaga no mapa. */
private val TAMANHO_VAGA = 52.dp

/** A partir desta largura cabem dois blocos lado a lado (desktop e web). */
private val LARGURA_DUAS_COLUNAS = 900.dp

/**
 * CRUD 2 - Vagas, apresentado como mapa do estacionamento.
 *
 * Cada bloco e uma area delimitada e cada vaga, um circulo dentro dela. O
 * preenchimento do circulo carrega o status (verde/vermelho/amarelo) e o anel
 * em volta carrega o tipo (PCD, idoso, moto), de modo que a ocupacao seja
 * legivel de relance sem perder a informacao do tipo.
 */
@Composable
fun TelaVagas(store: AppStore) {
    var emEdicao by remember { mutableStateOf<Vaga?>(null) }
    var criando by remember { mutableStateOf(false) }
    var excluindo by remember { mutableStateOf<Vaga?>(null) }
    var detalhando by remember { mutableStateOf<Vaga?>(null) }
    var destaqueStatus by remember { mutableStateOf("TODOS") }
    var filtroBloco by remember { mutableStateOf(0L) }
    val escopo = rememberCoroutineScope()

    val blocosVisiveis = store.blocos.filter { filtroBloco == 0L || it.id == filtroBloco }
    val vagasPorBloco = store.vagas.groupBy { it.blocoId }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val colunas = if (maxWidth >= LARGURA_DUAS_COLUNAS) 2 else 1

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Column {
                    Text("Vagas", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(
                        "${store.vagas.count { it.status == "LIVRE" }} livres de ${store.vagas.size} vagas",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            item {
                Column {
                    Seletor(
                        rotulo = "Bloco",
                        opcoes = listOf("0" to "Todos") + store.blocos.map { it.id.toString() to it.nome },
                        selecionada = filtroBloco.toString(),
                        aoSelecionar = { filtroBloco = it.toLong() },
                    )
                    // Destacar em vez de filtrar: tirar circulos do mapa abriria
                    // buracos e o desenho deixaria de representar o estacionamento.
                    Seletor(
                        rotulo = "Destacar status",
                        opcoes = listOf("TODOS" to "Todos") + STATUS,
                        selecionada = destaqueStatus,
                        aoSelecionar = { destaqueStatus = it },
                    )
                }
            }

            item { Legenda() }

            if (blocosVisiveis.isEmpty()) {
                item { Text("Nenhum bloco cadastrado.") }
            }

            items(blocosVisiveis.chunked(colunas)) { linha ->
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    linha.forEach { bloco ->
                        CartaoBloco(
                            bloco = bloco,
                            vagas = vagasPorBloco[bloco.id].orEmpty(),
                            destaqueStatus = destaqueStatus,
                            modifier = Modifier.weight(1f),
                            aoAbrirVaga = { detalhando = it },
                        )
                    }
                    // Mantem o ultimo bloco da linha com a mesma largura dos demais.
                    repeat(colunas - linha.size) { Spacer(Modifier.weight(1f)) }
                }
            }

            item { Spacer(Modifier.height(72.dp)) }
        }

        if (store.isAdmin) {
            ExtendedFloatingActionButton(
                onClick = { criando = true },
                modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp),
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Nova vaga") },
            )
        }
    }

    detalhando?.let { alvo ->
        DetalhesVaga(
            store = store,
            vaga = alvo,
            aoFechar = { detalhando = null },
            aoEditar = { detalhando = null; emEdicao = alvo },
            aoExcluir = { detalhando = null; excluindo = alvo },
        )
    }

    if (criando || emEdicao != null) {
        FormularioVaga(
            store = store,
            inicial = emEdicao,
            aoCancelar = { criando = false; emEdicao = null },
            aoSalvar = { request ->
                val alvo = emEdicao
                escopo.launch {
                    val ok = store.executarERecarregar(if (alvo == null) "Vaga criada" else "Vaga atualizada") {
                        if (alvo == null) store.api.criarVaga(request) else store.api.atualizarVaga(alvo.id, request)
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
            descricao = "a vaga ${alvo.numero}",
            aoConfirmar = {
                escopo.launch {
                    store.executarERecarregar("Vaga excluida") { store.api.excluirVaga(alvo.id) }
                    excluindo = null
                }
            },
            aoCancelar = { excluindo = null },
        )
    }
}

/** Area delimitada de um bloco, com as vagas distribuidas dentro dela. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CartaoBloco(
    bloco: Bloco,
    vagas: List<Vaga>,
    destaqueStatus: String,
    modifier: Modifier = Modifier,
    aoAbrirVaga: (Vaga) -> Unit,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        bloco.nome.uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "${vagas.count { it.status == "LIVRE" }} livres de ${vagas.size}" +
                            " - capacidade ${bloco.capacidade}${if (bloco.coberto) " - coberto" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                if (bloco.status != "ATIVO") {
                    ChipStatus(bloco.status, CoresStatus.offline)
                }
            }

            Spacer(Modifier.height(16.dp))

            if (vagas.isEmpty()) {
                Text(
                    "Nenhuma vaga cadastrada neste bloco.",
                    style = MaterialTheme.typography.bodySmall,
                )
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    vagas.sortedBy { it.numero }.forEach { vaga ->
                        CirculoVaga(
                            vaga = vaga,
                            atenuada = destaqueStatus != "TODOS" && vaga.status != destaqueStatus,
                            aoClicar = { aoAbrirVaga(vaga) },
                        )
                    }
                }
            }
        }
    }
}

/**
 * Uma vaga no mapa: preenchimento pelo status, anel pelo tipo e o numero sempre
 * visivel no centro. Cresce de leve sob o cursor e abre o tooltip com o resumo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CirculoVaga(vaga: Vaga, atenuada: Boolean, aoClicar: () -> Unit) {
    val interacao = remember { MutableInteractionSource() }
    val sobre by interacao.collectIsHoveredAsState()
    val escala by animateFloatAsState(if (sobre) 1.12f else 1f, tween(140))
    val corTipo = CoresTipo.deVaga(vaga.tipo)

    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = { PlainTooltip { Text(resumoDaVaga(vaga)) } },
        state = rememberTooltipState(),
    ) {
        Box(
            modifier = Modifier
                .size(TAMANHO_VAGA)
                .scale(escala)
                .alpha(if (atenuada) 0.25f else 1f)
                .clip(CircleShape)
                .border(3.dp, corTipo ?: Color.Transparent, CircleShape)
                .hoverable(interacao)
                .clickable(interactionSource = interacao, indication = null, onClick = aoClicar),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(5.dp)
                    .clip(CircleShape)
                    .background(CoresStatus.deVaga(vaga.status)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    vaga.numero.toString().padStart(2, '0'),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

/** Texto do tooltip: so o que o sistema realmente sabe sobre a vaga. */
private fun resumoDaVaga(vaga: Vaga): String = buildString {
    append("Vaga ${vaga.numero.toString().padStart(2, '0')} - ${CoresTipo.rotulo(vaga.tipo)}")
    append("\nStatus: ${rotuloStatus(vaga.status)}")
    append("\nBloco: ${vaga.blocoNome}")
    append("\nValor/hora: R$ ${formatarDecimal(vaga.valorHora)}")
    append("\n${vaga.dispositivoNome?.let { "Sensor: $it" } ?: "Sem sensor vinculado"}")
}

private fun rotuloStatus(status: String): String =
    STATUS.firstOrNull { it.first == status }?.second ?: status

/** Explica as cores do mapa: preenchimento = status, anel = tipo. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Legenda() {
    Card(shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "Cor do circulo = status",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ItemLegenda(preenchimento = CoresStatus.livre, texto = "Disponivel")
                ItemLegenda(preenchimento = CoresStatus.ocupada, texto = "Ocupada")
                ItemLegenda(preenchimento = CoresStatus.manutencao, texto = "Manutencao")
            }

            Spacer(Modifier.height(12.dp))
            Text(
                "Anel em volta = tipo",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ItemLegenda(anel = CoresTipo.pcd, texto = "PCD")
                ItemLegenda(anel = CoresTipo.idoso, texto = "Idoso")
                ItemLegenda(anel = CoresTipo.moto, texto = "Moto")
                ItemLegenda(texto = "Normal (sem anel)")
            }
        }
    }
}

@Composable
private fun ItemLegenda(preenchimento: Color? = null, anel: Color? = null, texto: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .border(2.dp, anel ?: Color.Transparent, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(3.dp)
                    .clip(CircleShape)
                    .background(preenchimento ?: CoresStatus.offline.copy(alpha = 0.45f)),
            )
        }
        Text(texto, style = MaterialTheme.typography.bodySmall)
    }
}

/** Detalhes abertos ao clicar numa vaga; ADMIN ainda edita e exclui por aqui. */
@Composable
private fun DetalhesVaga(
    store: AppStore,
    vaga: Vaga,
    aoFechar: () -> Unit,
    aoEditar: () -> Unit,
    aoExcluir: () -> Unit,
) {
    // Carregado apenas para ADMIN: o USER ve so o nome que ja vem na vaga.
    val dispositivo = store.dispositivos.firstOrNull { it.id == vaga.dispositivoId }

    AlertDialog(
        onDismissRequest = aoFechar,
        title = { Text("Vaga ${vaga.numero.toString().padStart(2, '0')}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Status", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    ChipStatus(rotuloStatus(vaga.status), CoresStatus.deVaga(vaga.status))
                }
                LinhaDetalhe("Tipo", CoresTipo.rotulo(vaga.tipo))
                LinhaDetalhe("Bloco", vaga.blocoNome)
                LinhaDetalhe("Valor/hora", "R$ ${formatarDecimal(vaga.valorHora)}")
                vaga.observacao?.let { LinhaDetalhe("Observacao", it) }
                LinhaDetalhe("Sensor", vaga.dispositivoNome ?: "Nenhum vinculado")
                dispositivo?.let { sensor ->
                    LinhaDetalhe("Sinal do sensor", sensor.status)
                    LinhaDetalhe(
                        "Ultima leitura",
                        sensor.ultimaComunicacao?.replace('T', ' ')?.take(19) ?: "Sem leituras",
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = aoFechar) { Text("Fechar") } },
        dismissButton = {
            if (store.isAdmin) {
                Row {
                    TextButton(onClick = aoEditar) { Text("Editar") }
                    TextButton(onClick = aoExcluir) { Text("Excluir", color = CoresStatus.ocupada) }
                }
            }
        },
    )
}

@Composable
private fun LinhaDetalhe(rotulo: String, valor: String) {
    Row {
        Text(rotulo, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Text(valor, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun FormularioVaga(
    store: AppStore,
    inicial: Vaga?,
    aoCancelar: () -> Unit,
    aoSalvar: (VagaRequest) -> Unit,
) {
    val blocoPadrao = store.blocos.firstOrNull()?.id ?: 0L
    var numero by remember { mutableStateOf(inicial?.numero?.toString() ?: "") }
    var tipo by remember { mutableStateOf(inicial?.tipo ?: "NORMAL") }
    var status by remember { mutableStateOf(inicial?.status ?: "LIVRE") }
    var blocoId by remember { mutableStateOf(inicial?.blocoId ?: blocoPadrao) }
    var dispositivoId by remember { mutableStateOf(inicial?.dispositivoId ?: 0L) }
    var valorHora by remember { mutableStateOf(inicial?.valorHora?.let { formatarDecimal(it) } ?: "") }
    var observacao by remember { mutableStateOf(inicial?.observacao ?: "") }

    // Um dispositivo so pode estar em uma vaga: a lista oferece os livres e o ja vinculado.
    val dispositivosDisponiveis = store.dispositivos.filter {
        it.vagaId == null || it.id == inicial?.dispositivoId
    }
    val valorValido = valorHora.isBlank() || textoParaDouble(valorHora)?.let { it >= 0 } == true

    DialogoFormulario(
        titulo = if (inicial == null) "Nova vaga" else "Editar vaga",
        gravarHabilitado = numero.toIntOrNull() != null && blocoId != 0L && valorValido,
        aoGravar = {
            aoSalvar(
                VagaRequest(
                    numero = numero.toIntOrNull() ?: 0,
                    tipo = tipo,
                    status = status,
                    blocoId = blocoId,
                    dispositivoId = dispositivoId.takeIf { it != 0L },
                    valorHora = textoParaDouble(valorHora) ?: 0.0,
                    observacao = observacao.trim().ifBlank { null },
                ),
            )
        },
        aoLimpar = {
            numero = ""
            tipo = "NORMAL"
            status = "LIVRE"
            blocoId = blocoPadrao
            dispositivoId = 0L
            valorHora = ""
            observacao = ""
        },
        aoCancelar = aoCancelar,
    ) {
        Campo("Numero", numero, { entrada -> numero = entrada.filter { it.isDigit() } })
        Seletor("Tipo", TIPOS, tipo, { tipo = it })
        Seletor("Status", STATUS, status, { status = it })
        Seletor(
            rotulo = "Bloco",
            opcoes = store.blocos.map { it.id.toString() to it.nome },
            selecionada = blocoId.toString(),
            aoSelecionar = { blocoId = it.toLong() },
        )
        Seletor(
            rotulo = "Sensor",
            opcoes = listOf("0" to "Nenhum") + dispositivosDisponiveis.map { it.id.toString() to it.nome },
            selecionada = dispositivoId.toString(),
            aoSelecionar = { dispositivoId = it.toLong() },
        )
        Campo(
            rotulo = "Valor por hora (R$)",
            valor = valorHora,
            aoMudar = { entrada -> valorHora = entrada.filter { it.isDigit() || it == ',' || it == '.' } },
            apoio = if (valorValido) "Ex.: 5,50" else "Valor invalido",
        )
        Campo("Observacao", observacao, { observacao = it })
    }
}
