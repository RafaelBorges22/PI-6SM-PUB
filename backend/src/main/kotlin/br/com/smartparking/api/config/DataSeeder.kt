package br.com.smartparking.api.config

import br.com.smartparking.api.domain.Role
import br.com.smartparking.api.domain.Usuario
import br.com.smartparking.api.repository.UsuarioRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Profile
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * Cria os usuarios iniciais na primeira execucao.
 *
 * Fica aqui, e nao numa migration, porque o hash BCrypt precisa ser gerado pelo
 * mesmo `PasswordEncoder` usado na autenticacao. Nada e feito se ja existir
 * algum usuario cadastrado.
 */
@Component
@Profile("!test")
class DataSeeder(
    private val usuarioRepository: UsuarioRepository,
    private val passwordEncoder: PasswordEncoder,
) : CommandLineRunner {

    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    override fun run(vararg args: String) {
        if (usuarioRepository.count() > 0L) return

        usuarioRepository.saveAll(
            listOf(
                Usuario(
                    nome = "Administrador",
                    email = "admin@smartparking.com",
                    senhaHash = passwordEncoder.encode(SENHA_PADRAO),
                    role = Role.ADMIN,
                ),
                Usuario(
                    nome = "Operador",
                    email = "user@smartparking.com",
                    senhaHash = passwordEncoder.encode(SENHA_PADRAO),
                    role = Role.USER,
                ),
            ),
        )
        log.warn(
            "Usuarios iniciais criados: admin@smartparking.com e user@smartparking.com (senha {}). " +
                "Troque as senhas antes de qualquer uso fora do laboratorio.",
            SENHA_PADRAO,
        )
    }

    private companion object {
        const val SENHA_PADRAO = "123456"
    }
}
