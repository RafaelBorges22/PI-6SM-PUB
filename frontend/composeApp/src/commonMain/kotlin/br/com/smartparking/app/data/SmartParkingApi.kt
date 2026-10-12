package br.com.smartparking.app.data

import br.com.smartparking.app.model.ApiError
import br.com.smartparking.app.model.ApiException
import br.com.smartparking.app.model.Bloco
import br.com.smartparking.app.model.BlocoComVagas
import br.com.smartparking.app.model.BlocoRequest
import br.com.smartparking.app.model.Dashboard
import br.com.smartparking.app.model.Dispositivo
import br.com.smartparking.app.model.DispositivoRequest
import br.com.smartparking.app.model.LoginRequest
import br.com.smartparking.app.model.LoginResponse
import br.com.smartparking.app.model.Medicao
import br.com.smartparking.app.model.SignupRequest
import br.com.smartparking.app.model.Usuario
import br.com.smartparking.app.model.UsuarioRequest
import br.com.smartparking.app.model.Vaga
import br.com.smartparking.app.model.VagaRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Cliente unico da API REST, compartilhado por Mobile, Web e Desktop.
 *
 * O token JWT fica em [token] e e enviado no cabecalho `Authorization` de todas
 * as chamadas autenticadas.
 */
class SmartParkingApi(var baseUrl: String) {

    var token: String? = null

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }

    private val client: HttpClient = HttpClient(httpEngineFactory()) {
        expectSuccess = false
        install(ContentNegotiation) { json(json) }
    }

    private fun url(caminho: String) = "${baseUrl.trimEnd('/')}$caminho"

    // ------------------------------------------------------------- auth -----

    suspend fun login(email: String, senha: String): LoginResponse {
        val resposta = client.post(url("/api/auth/signin")) {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(email, senha))
        }
        return tratar(resposta)
    }

    /** Cadastro publico (sem token): devolve o usuario criado. */
    suspend fun signup(nome: String, email: String, senha: String): Usuario = tratar(
        client.post(url("/api/auth/signup")) {
            contentType(ContentType.Application.Json)
            setBody(SignupRequest(nome, email, senha))
        },
    )

    // ------------------------------------------------------------ blocos ----

    suspend fun listarBlocos(): List<Bloco> = tratar(client.get(url("/api/blocos")) { autenticar() })

    suspend fun vagasDoBloco(id: Long): BlocoComVagas =
        tratar(client.get(url("/api/blocos/$id/vagas")) { autenticar() })

    suspend fun criarBloco(request: BlocoRequest): Bloco = tratar(
        client.post(url("/api/blocos")) {
            autenticar()
            contentType(ContentType.Application.Json)
            setBody(request)
        },
    )

    suspend fun atualizarBloco(id: Long, request: BlocoRequest): Bloco = tratar(
        client.put(url("/api/blocos/$id")) {
            autenticar()
            contentType(ContentType.Application.Json)
            setBody(request)
        },
    )

    suspend fun excluirBloco(id: Long) = semConteudo(client.delete(url("/api/blocos/$id")) { autenticar() })

    // ------------------------------------------------------------- vagas ----

    suspend fun listarVagas(status: String? = null, blocoId: Long? = null, tipo: String? = null): List<Vaga> = tratar(
        client.get(url("/api/vagas")) {
            autenticar()
            status?.let { parameter("status", it) }
            blocoId?.let { parameter("blocoId", it) }
            tipo?.let { parameter("tipo", it) }
        },
    )

    suspend fun criarVaga(request: VagaRequest): Vaga = tratar(
        client.post(url("/api/vagas")) {
            autenticar()
            contentType(ContentType.Application.Json)
            setBody(request)
        },
    )

    suspend fun atualizarVaga(id: Long, request: VagaRequest): Vaga = tratar(
        client.put(url("/api/vagas/$id")) {
            autenticar()
            contentType(ContentType.Application.Json)
            setBody(request)
        },
    )

    suspend fun excluirVaga(id: Long) = semConteudo(client.delete(url("/api/vagas/$id")) { autenticar() })

    // ------------------------------------------------------ dispositivos ----

    suspend fun listarDispositivos(): List<Dispositivo> =
        tratar(client.get(url("/api/dispositivos")) { autenticar() })

    suspend fun criarDispositivo(request: DispositivoRequest): Dispositivo = tratar(
        client.post(url("/api/dispositivos")) {
            autenticar()
            contentType(ContentType.Application.Json)
            setBody(request)
        },
    )

    suspend fun atualizarDispositivo(id: Long, request: DispositivoRequest): Dispositivo = tratar(
        client.put(url("/api/dispositivos/$id")) {
            autenticar()
            contentType(ContentType.Application.Json)
            setBody(request)
        },
    )

    suspend fun excluirDispositivo(id: Long) =
        semConteudo(client.delete(url("/api/dispositivos/$id")) { autenticar() })

    // ---------------------------------------------------------- usuarios ----

    suspend fun listarUsuarios(): List<Usuario> = tratar(client.get(url("/api/usuarios")) { autenticar() })

    suspend fun criarUsuario(request: UsuarioRequest): Usuario = tratar(
        client.post(url("/api/usuarios")) {
            autenticar()
            contentType(ContentType.Application.Json)
            setBody(request)
        },
    )

    suspend fun atualizarUsuario(id: Long, request: UsuarioRequest): Usuario = tratar(
        client.put(url("/api/usuarios/$id")) {
            autenticar()
            contentType(ContentType.Application.Json)
            setBody(request)
        },
    )

    suspend fun excluirUsuario(id: Long) = semConteudo(client.delete(url("/api/usuarios/$id")) { autenticar() })

    // --------------------------------------------------------- dashboard ----

    suspend fun dashboard(): Dashboard = tratar(client.get(url("/api/dashboard")) { autenticar() })

    suspend fun medicoes(): List<Medicao> = tratar(client.get(url("/api/medicoes")) { autenticar() })

    // ----------------------------------------------------------- apoio ------

    private fun io.ktor.client.request.HttpRequestBuilder.autenticar() {
        token?.let { header("Authorization", "Bearer $it") }
    }

    private suspend inline fun <reified T> tratar(resposta: HttpResponse): T {
        if (resposta.status.isSuccess()) {
            return resposta.body()
        }
        throw erro(resposta)
    }

    private suspend fun semConteudo(resposta: HttpResponse) {
        if (!resposta.status.isSuccess()) {
            throw erro(resposta)
        }
    }

    /** Converte o corpo ApiError da API na mensagem que sera exibida ao usuario. */
    private suspend fun erro(resposta: HttpResponse): ApiException {
        val texto = runCatching { resposta.bodyAsText() }.getOrDefault("")
        val mensagem = runCatching { json.decodeFromString<ApiError>(texto).mensagem }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
            ?: when (resposta.status) {
                HttpStatusCode.Unauthorized -> "Sessao expirada ou credenciais invalidas"
                HttpStatusCode.Forbidden -> "Seu perfil nao tem permissao para esta acao"
                else -> "Falha na requisicao (${resposta.status.value})"
            }
        return ApiException(resposta.status.value, mensagem)
    }

    private fun HttpStatusCode.isSuccess() = value in 200..299
}
