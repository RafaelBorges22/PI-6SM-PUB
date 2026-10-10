package br.com.smartparking.app.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import br.com.smartparking.app.model.Bloco
import br.com.smartparking.app.model.Dashboard
import br.com.smartparking.app.model.Dispositivo
import br.com.smartparking.app.model.Usuario
import br.com.smartparking.app.model.Vaga

/**
 * Estado da aplicacao compartilhado pelos tres frontends.
 *
 * Concentra a sessao (token e perfil), as listas carregadas da API e as
 * operacoes de CRUD, de modo que as telas Compose sejam apenas apresentacao.
 */
class AppStore {

    val api = SmartParkingApi(urlPadraoDaApi())

    // ------------------------------------------------------------ sessao ----

    var urlApi by mutableStateOf(urlPadraoDaApi())
    var autenticado by mutableStateOf(false)
        private set
    var nomeUsuario by mutableStateOf("")
        private set
    var emailUsuario by mutableStateOf("")
        private set
    var role by mutableStateOf("USER")
        private set

    val isAdmin: Boolean get() = role == "ADMIN"

    // ------------------------------------------------------------- dados ----

    var dashboard by mutableStateOf<Dashboard?>(null)
        private set
    val blocos = mutableStateListOf<Bloco>()
    val vagas = mutableStateListOf<Vaga>()
    val dispositivos = mutableStateListOf<Dispositivo>()
    val usuarios = mutableStateListOf<Usuario>()

    var carregando by mutableStateOf(false)
        private set
    var mensagemErro by mutableStateOf<String?>(null)
    var mensagemSucesso by mutableStateOf<String?>(null)

    // ---------------------------------------------------------- operacoes ---

    suspend fun entrar(email: String, senha: String): Boolean = executar {
        api.baseUrl = urlApi.trim()
        val resposta = api.login(email.trim(), senha)
        api.token = resposta.token
        nomeUsuario = resposta.nome
        emailUsuario = resposta.email
        role = resposta.role
        autenticado = true
        carregarTudo()
    }

    /** Cria a conta em /api/auth/signup (perfil USER) e ja entra com ela. */
    suspend fun cadastrarEEntrar(nome: String, email: String, senha: String): Boolean {
        val criado = executar {
            api.baseUrl = urlApi.trim()
            api.signup(nome.trim(), email.trim(), senha)
        }
        return criado && entrar(email, senha)
    }

    fun sair() {
        api.token = null
        autenticado = false
        nomeUsuario = ""
        emailUsuario = ""
        role = "USER"
        dashboard = null
        blocos.clear()
        vagas.clear()
        dispositivos.clear()
        usuarios.clear()
    }

    /**
     * Recarrega tudo o que o perfil atual pode ver.
     *
     * `silencioso` evita piscar o indicador de carregamento na atualizacao
     * automatica do dashboard.
     */
    suspend fun carregarTudo(silencioso: Boolean = false) {
        if (!autenticado) return
        if (!silencioso) carregando = true
        try {
            blocos.substituirPor(api.listarBlocos())
            vagas.substituirPor(api.listarVagas())
            // Dashboard, sensores e usuarios sao 403 para o perfil USER: so pede
            // o que o backend libera, senao a tela mostraria erro a cada polling.
            if (isAdmin) {
                dashboard = api.dashboard()
                dispositivos.substituirPor(api.listarDispositivos())
                usuarios.substituirPor(api.listarUsuarios())
            }
            if (!silencioso) mensagemErro = null
        } catch (e: Exception) {
            if (!silencioso) mensagemErro = e.message
        } finally {
            if (!silencioso) carregando = false
        }
    }

    /**
     * Executa uma operacao de escrita, recarrega os dados e traduz a excecao em
     * mensagem de erro. Devolve `true` quando deu certo.
     */
    suspend fun executar(sucesso: String? = null, bloco: suspend () -> Unit): Boolean {
        carregando = true
        return try {
            bloco()
            mensagemErro = null
            mensagemSucesso = sucesso
            true
        } catch (e: Exception) {
            mensagemErro = e.message ?: "Falha inesperada"
            false
        } finally {
            carregando = false
        }
    }

    suspend fun executarERecarregar(sucesso: String, bloco: suspend () -> Unit): Boolean {
        val ok = executar(sucesso, bloco)
        if (ok) carregarTudo(silencioso = true)
        return ok
    }

    private fun <T> androidx.compose.runtime.snapshots.SnapshotStateList<T>.substituirPor(novos: List<T>) {
        clear()
        addAll(novos)
    }
}
