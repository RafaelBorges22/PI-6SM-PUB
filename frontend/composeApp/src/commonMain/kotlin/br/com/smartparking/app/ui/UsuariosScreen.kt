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
import br.com.smartparking.app.model.Usuario
import br.com.smartparking.app.model.UsuarioRequest
import kotlinx.coroutines.launch

private val ROLES = listOf("ADMIN" to "Administrador", "USER" to "Operador")

private val SITUACOES = listOf("true" to "Ativo", "false" to "Inativo")

/** CRUD 4 - Usuarios. A tela so aparece para o perfil ADMIN. */
@Composable
fun TelaUsuarios(store: AppStore) {
    var emEdicao by remember { mutableStateOf<Usuario?>(null) }
    var criando by remember { mutableStateOf(false) }
    var excluindo by remember { mutableStateOf<Usuario?>(null) }
    val escopo = rememberCoroutineScope()

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Text("Usuarios", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
            items(store.usuarios) { usuario ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(usuario.nome, fontWeight = FontWeight.Bold)
                            Text(usuario.email, style = MaterialTheme.typography.bodySmall)
                            usuario.dataCadastro?.let {
                                Text(
                                    "Cadastrado em: ${formatarDataHora(it)}",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                        ChipStatus(
                            usuario.role,
                            if (usuario.role == "ADMIN") MaterialTheme.colorScheme.primary else CoresStatus.offline,
                        )
                        Spacer(Modifier.padding(4.dp))
                        ChipStatus(
                            if (usuario.ativo) "ATIVO" else "INATIVO",
                            if (usuario.ativo) CoresStatus.livre else CoresStatus.offline,
                        )
                        AcoesLinha(aoEditar = { emEdicao = usuario }, aoExcluir = { excluindo = usuario })
                    }
                }
            }
            item { Spacer(Modifier.padding(32.dp)) }
        }

        ExtendedFloatingActionButton(
            onClick = { criando = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp),
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            text = { Text("Novo usuario") },
        )
    }

    if (criando || emEdicao != null) {
        FormularioUsuario(
            inicial = emEdicao,
            aoCancelar = { criando = false; emEdicao = null },
            aoSalvar = { request ->
                val alvo = emEdicao
                escopo.launch {
                    val ok = store.executarERecarregar(
                        if (alvo == null) "Usuario criado" else "Usuario atualizado",
                    ) {
                        if (alvo == null) {
                            store.api.criarUsuario(request)
                        } else {
                            store.api.atualizarUsuario(alvo.id, request)
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
            descricao = "o usuario ${alvo.nome}",
            aoConfirmar = {
                escopo.launch {
                    store.executarERecarregar("Usuario excluido") { store.api.excluirUsuario(alvo.id) }
                    excluindo = null
                }
            },
            aoCancelar = { excluindo = null },
        )
    }
}

@Composable
private fun FormularioUsuario(inicial: Usuario?, aoCancelar: () -> Unit, aoSalvar: (UsuarioRequest) -> Unit) {
    var nome by remember { mutableStateOf(inicial?.nome ?: "") }
    var email by remember { mutableStateOf(inicial?.email ?: "") }
    var senha by remember { mutableStateOf("") }
    var role by remember { mutableStateOf(inicial?.role ?: "USER") }
    var ativo by remember { mutableStateOf(inicial?.ativo ?: true) }

    DialogoFormulario(
        titulo = if (inicial == null) "Novo usuario" else "Editar usuario",
        gravarHabilitado = nome.isNotBlank() && email.isNotBlank() &&
            (inicial != null || senha.length >= 6),
        aoGravar = {
            aoSalvar(
                UsuarioRequest(
                    nome = nome.trim(),
                    email = email.trim(),
                    senha = senha.ifBlank { null },
                    role = role,
                    ativo = ativo,
                ),
            )
        },
        aoLimpar = {
            nome = ""
            email = ""
            senha = ""
            role = "USER"
            ativo = true
        },
        aoCancelar = aoCancelar,
    ) {
        Campo("Nome", nome, { nome = it })
        Campo("Email", email, { email = it })
        Campo(
            rotulo = "Senha",
            valor = senha,
            aoMudar = { senha = it },
            senha = true,
            apoio = if (inicial == null) "Minimo de 6 caracteres" else "Deixe em branco para manter a atual",
        )
        Seletor("Perfil", ROLES, role, { role = it })
        Seletor("Situacao", SITUACOES, ativo.toString(), { ativo = it.toBoolean() })
    }
}
