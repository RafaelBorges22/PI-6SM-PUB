package br.com.smartparking.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Etiqueta colorida de status (LIVRE / OCUPADA / ONLINE / ...). */
@Composable
fun ChipStatus(texto: String, cor: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(cor.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = texto,
            color = cor,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}

/** Campo de texto padrao dos formularios. */
@Composable
fun Campo(
    rotulo: String,
    valor: String,
    aoMudar: (String) -> Unit,
    modifier: Modifier = Modifier,
    senha: Boolean = false,
    apoio: String? = null,
) {
    OutlinedTextField(
        value = valor,
        onValueChange = aoMudar,
        label = { Text(rotulo) },
        singleLine = true,
        supportingText = apoio?.let { { Text(it) } },
        visualTransformation = if (senha) {
            androidx.compose.ui.text.input.PasswordVisualTransformation()
        } else {
            androidx.compose.ui.text.input.VisualTransformation.None
        },
        modifier = modifier.fillMaxWidth().padding(vertical = 4.dp),
    )
}

/**
 * Selecao de uma opcao a partir de uma lista fixa (enums da API).
 *
 * Usa DropdownMenu simples em vez de ExposedDropdownMenuBox para manter o mesmo
 * comportamento nos tres alvos.
 */
@Composable
fun Seletor(
    rotulo: String,
    opcoes: List<Pair<String, String>>,
    selecionada: String,
    aoSelecionar: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var aberto by remember { mutableStateOf(false) }
    val rotuloSelecionado = opcoes.firstOrNull { it.first == selecionada }?.second ?: "Selecione"

    Box(modifier = modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        OutlinedButton(onClick = { aberto = true }, modifier = Modifier.fillMaxWidth()) {
            Text("$rotulo: $rotuloSelecionado", modifier = Modifier.weight(1f))
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(expanded = aberto, onDismissRequest = { aberto = false }) {
            opcoes.forEach { (valor, texto) ->
                DropdownMenuItem(
                    text = { Text(texto) },
                    onClick = {
                        aoSelecionar(valor)
                        aberto = false
                    },
                )
            }
        }
    }
}

/** Faixa de mensagem (erro ou sucesso) com botao de fechar. */
@Composable
fun Banner(texto: String, cor: Color, aoFechar: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .background(cor.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(texto, color = cor, modifier = Modifier.weight(1f))
        IconButton(onClick = aoFechar) {
            Icon(Icons.Default.Close, contentDescription = "Fechar", tint = cor)
        }
    }
}

/** Botoes de editar e excluir usados nas listas dos CRUDs (visiveis apenas para ADMIN). */
@Composable
fun AcoesLinha(aoEditar: () -> Unit, aoExcluir: () -> Unit) {
    Row {
        IconButton(onClick = aoEditar) {
            Icon(Icons.Default.Edit, contentDescription = "Editar")
        }
        IconButton(onClick = aoExcluir) {
            Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = CoresStatus.ocupada)
        }
    }
}

/** Dialogo de confirmacao de exclusao. */
@Composable
fun ConfirmarExclusao(descricao: String, aoConfirmar: () -> Unit, aoCancelar: () -> Unit) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = aoCancelar,
        title = { Text("Confirmar exclusao") },
        text = { Text("Deseja realmente excluir $descricao?") },
        confirmButton = {
            Button(onClick = aoConfirmar) { Text("Excluir") }
        },
        dismissButton = {
            TextButton(onClick = aoCancelar) { Text("Cancelar") }
        },
    )
}

/** Linha com rotulo e Switch, para campos booleanos dos formularios. */
@Composable
fun CampoInterruptor(rotulo: String, marcado: Boolean, aoMudar: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(rotulo, modifier = Modifier.weight(1f))
        Switch(checked = marcado, onCheckedChange = aoMudar)
    }
}

/**
 * Tela de formulario padrao dos CRUDs: campos rolaveis e os botoes
 * Gravar / Limpar Campos / Cancelar, iguais nas quatro entidades.
 */
@Composable
fun DialogoFormulario(
    titulo: String,
    gravarHabilitado: Boolean,
    aoGravar: () -> Unit,
    aoLimpar: () -> Unit,
    aoCancelar: () -> Unit,
    campos: @Composable () -> Unit,
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = aoCancelar,
        title = { Text(titulo) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) { campos() }
        },
        confirmButton = {
            Button(onClick = aoGravar, enabled = gravarHabilitado) { Text("Gravar") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = aoCancelar) { Text("Cancelar") }
                OutlinedButton(onClick = aoLimpar) { Text("Limpar Campos") }
            }
        },
    )
}

/** Converte texto digitado em Double aceitando virgula ou ponto como separador. */
fun textoParaDouble(texto: String): Double? = texto.trim().replace(',', '.').toDoubleOrNull()

/** Valida data no formato yyyy-MM-dd (o LocalDate que a API espera). */
fun dataIsoValida(texto: String): Boolean {
    val partes = Regex("""^(\d{4})-(\d{2})-(\d{2})$""").find(texto.trim())?.groupValues ?: return false
    val ano = partes[1].toInt()
    val mes = partes[2].toInt()
    val dia = partes[3].toInt()
    if (mes !in 1..12 || dia < 1) return false
    val bissexto = (ano % 4 == 0 && ano % 100 != 0) || ano % 400 == 0
    val diasNoMes = when (mes) {
        2 -> if (bissexto) 29 else 28
        4, 6, 9, 11 -> 30
        else -> 31
    }
    return dia <= diasNoMes
}

/** Exibe data/hora ISO da API como "yyyy-MM-dd HH:mm". */
fun formatarDataHora(iso: String): String = iso.replace('T', ' ').take(16)

/** Valor decimal com duas casas e virgula (ex.: 5,50), sem depender de String.format. */
fun formatarDecimal(valor: Double): String {
    val centavos = kotlin.math.round(valor * 100).toLong()
    val sinal = if (centavos < 0) "-" else ""
    val absoluto = kotlin.math.abs(centavos)
    return "$sinal${absoluto / 100},${(absoluto % 100).toString().padStart(2, '0')}"
}
