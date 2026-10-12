package br.com.smartparking.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import br.com.smartparking.app.data.nomeDaPlataforma
import kotlinx.coroutines.launch

/**
 * Tela de login: obtem o token JWT em POST /api/auth/signin. Tambem permite
 * criar uma conta (POST /api/auth/signup) e entrar em seguida.
 */
@Composable
fun TelaLogin(store: AppStore) {
    var email by remember { mutableStateOf("admin@smartparking.com") }
    var senha by remember { mutableStateOf("123456") }
    var nome by remember { mutableStateOf("") }
    var criandoConta by remember { mutableStateOf(false) }
    val escopo = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Card(modifier = Modifier.widthIn(max = 420.dp).padding(24.dp)) {
            Column(
                modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "Smart Parking",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "Sistema Inteligente de Estacionamento",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(20.dp))

                Campo(
                    rotulo = "Endereco da API",
                    valor = store.urlApi,
                    aoMudar = { store.urlApi = it },
                    apoio = "Backend Spring Boot (${nomeDaPlataforma()})",
                )
                if (criandoConta) {
                    Campo("Nome", nome, { nome = it })
                }
                Campo("Email", email, { email = it })
                Campo("Senha", senha, { senha = it }, senha = true)

                Spacer(Modifier.height(12.dp))

                Button(
                    onClick = {
                        escopo.launch {
                            if (criandoConta) store.cadastrarEEntrar(nome, email, senha) else store.entrar(email, senha)
                        }
                    },
                    enabled = !store.carregando && email.isNotBlank() && senha.isNotBlank() &&
                        (!criandoConta || (nome.isNotBlank() && senha.length >= 6)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        when {
                            store.carregando -> "Aguarde..."
                            criandoConta -> "Criar conta e entrar"
                            else -> "Entrar"
                        },
                    )
                }
                TextButton(onClick = { criandoConta = !criandoConta }) {
                    Text(if (criandoConta) "Ja tenho conta" else "Criar conta")
                }

                if (store.carregando) {
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                store.mensagemErro?.let {
                    Spacer(Modifier.height(8.dp))
                    Banner(it, CoresStatus.ocupada) { store.mensagemErro = null }
                }

                Spacer(Modifier.height(16.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text("Usuarios de demonstracao", style = MaterialTheme.typography.labelLarge)
                    Text("admin@smartparking.com / 123456 (ADMIN)", style = MaterialTheme.typography.bodySmall)
                    Text("user@smartparking.com / 123456 (USER)", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
