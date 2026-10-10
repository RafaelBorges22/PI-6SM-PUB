package br.com.smartparking.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AzulPrincipal = Color(0xFF1565C0)
private val AzulClaro = Color(0xFF5E92F3)

/** Cores de status usadas nas listas e no dashboard (verde/vermelho/amarelo da secao 25). */
object CoresStatus {
    val livre = Color(0xFF2E7D32)
    val ocupada = Color(0xFFC62828)
    val manutencao = Color(0xFFF9A825)
    val online = Color(0xFF2E7D32)
    val offline = Color(0xFF757575)

    fun deVaga(status: String): Color = when (status) {
        "LIVRE" -> livre
        "OCUPADA" -> ocupada
        "MANUTENCAO" -> manutencao
        else -> offline
    }

    fun deDispositivo(status: String): Color = when (status) {
        "ONLINE" -> online
        "MANUTENCAO" -> manutencao
        else -> offline
    }
}

/**
 * Cores dos tipos de vaga, usadas como anel em volta do circulo no mapa do
 * estacionamento: o preenchimento carrega o status e o anel, o tipo.
 *
 * NORMAL nao tem anel - e a maioria das vagas, e marcar todas so criaria ruido.
 */
object CoresTipo {
    val pcd = Color(0xFF1565C0)
    val idoso = Color(0xFF6A1B9A)
    val moto = Color(0xFF00838F)

    fun deVaga(tipo: String): Color? = when (tipo) {
        "PCD" -> pcd
        "IDOSO" -> idoso
        "MOTO" -> moto
        else -> null
    }

    fun rotulo(tipo: String): String = when (tipo) {
        "PCD" -> "PCD"
        "IDOSO" -> "Idoso"
        "MOTO" -> "Moto"
        else -> "Normal"
    }
}

private val Claro = lightColorScheme(
    primary = AzulPrincipal,
    secondary = AzulClaro,
)

private val Escuro = darkColorScheme(
    primary = AzulClaro,
    secondary = AzulPrincipal,
)

@Composable
fun TemaSmartParking(escuro: Boolean = false, conteudo: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (escuro) Escuro else Claro,
        content = conteudo,
    )
}
