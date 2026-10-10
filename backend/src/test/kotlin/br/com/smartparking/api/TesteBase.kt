package br.com.smartparking.api

import br.com.smartparking.api.domain.Role
import br.com.smartparking.api.domain.Usuario
import br.com.smartparking.api.repository.BlocoRepository
import br.com.smartparking.api.repository.DispositivoRepository
import br.com.smartparking.api.repository.MedicaoRepository
import br.com.smartparking.api.repository.UsuarioRepository
import br.com.smartparking.api.repository.VagaRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc

/**
 * Base dos testes de integracao: sobe o contexto completo sobre H2, limpa as
 * tabelas e recria um ADMIN e um USER antes de cada teste.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
abstract class TesteBase {

    @Autowired protected lateinit var mockMvc: MockMvc
    @Autowired protected lateinit var objectMapper: ObjectMapper
    @Autowired protected lateinit var usuarioRepository: UsuarioRepository
    @Autowired protected lateinit var blocoRepository: BlocoRepository
    @Autowired protected lateinit var vagaRepository: VagaRepository
    @Autowired protected lateinit var dispositivoRepository: DispositivoRepository
    @Autowired protected lateinit var medicaoRepository: MedicaoRepository
    @Autowired protected lateinit var passwordEncoder: PasswordEncoder

    protected lateinit var tokenAdmin: String
    protected lateinit var tokenUser: String

    @BeforeEach
    fun prepararBase() {
        // deleteAllInBatch: DELETE direto, sem carregar as entidades no contexto de
        // persistencia (limpeza entre testes nao precisa passar pelas cascatas do JPA).
        medicaoRepository.deleteAllInBatch()
        vagaRepository.deleteAllInBatch()
        dispositivoRepository.deleteAllInBatch()
        blocoRepository.deleteAllInBatch()
        usuarioRepository.deleteAllInBatch()

        usuarioRepository.saveAll(
            listOf(
                Usuario(
                    nome = "Administrador",
                    email = EMAIL_ADMIN,
                    senhaHash = passwordEncoder.encode(SENHA),
                    role = Role.ADMIN,
                ),
                Usuario(
                    nome = "Operador",
                    email = EMAIL_USER,
                    senhaHash = passwordEncoder.encode(SENHA),
                    role = Role.USER,
                ),
            ),
        )

        tokenAdmin = autenticar(EMAIL_ADMIN)
        tokenUser = autenticar(EMAIL_USER)
    }

    protected fun autenticar(email: String, senha: String = SENHA): String {
        val corpo = objectMapper.writeValueAsString(mapOf("email" to email, "senha" to senha))
        val resposta = mockMvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/auth/signin")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content(corpo),
        ).andReturn().response.contentAsString
        return objectMapper.readTree(resposta).get("token").asText()
    }

    protected fun bearer(token: String) = "Bearer $token"

    protected fun json(valor: Any): String = objectMapper.writeValueAsString(valor)

    companion object {
        const val EMAIL_ADMIN = "admin@smartparking.com"
        const val EMAIL_USER = "user@smartparking.com"
        const val SENHA = "123456"
    }
}
