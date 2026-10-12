package br.com.smartparking.api.service

import br.com.smartparking.api.common.RecursoNaoEncontradoException
import br.com.smartparking.api.common.RegraDeNegocioException
import br.com.smartparking.api.domain.Role
import br.com.smartparking.api.domain.Usuario
import br.com.smartparking.api.mapper.UsuarioMapper
import br.com.smartparking.api.repository.UsuarioRepository
import br.com.smartparking.api.web.dto.SignupRequest
import br.com.smartparking.api.web.dto.UsuarioRequest
import br.com.smartparking.api.web.dto.UsuarioResponse
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** CRUD 4 - Usuarios (secao 18 da especificacao). */
@Service
@Transactional(readOnly = true)
class UsuarioService(
    private val usuarioRepository: UsuarioRepository,
    private val passwordEncoder: PasswordEncoder,
    private val mapper: UsuarioMapper,
) {

    fun listar(): List<UsuarioResponse> = usuarioRepository.findAll()
        .sortedBy { it.nome }
        .map { mapper.paraResponse(it) }

    /** Busca parcial pelo nome (consulta JPQL com LIKE). */
    fun buscarPorNome(nome: String): List<UsuarioResponse> =
        usuarioRepository.buscarPorNome(nome.trim()).map { mapper.paraResponse(it) }

    fun buscarPorId(id: Long): UsuarioResponse = mapper.paraResponse(obter(id))

    fun buscarPorEmail(email: String): UsuarioResponse = mapper.paraResponse(
        usuarioRepository.findByEmailIgnoreCase(email)
            ?: throw RecursoNaoEncontradoException("Usuario $email nao encontrado"),
    )

    @Transactional
    fun criar(request: UsuarioRequest): UsuarioResponse {
        if (usuarioRepository.existsByEmailIgnoreCase(request.email)) {
            throw RegraDeNegocioException("Ja existe um usuario com o email ${request.email}")
        }
        val senha = request.senha?.takeIf { it.isNotBlank() }
            ?: throw RegraDeNegocioException("senha e obrigatoria na criacao do usuario")
        val usuario = mapper.paraEntidade(request, passwordEncoder.encode(senha))
        return mapper.paraResponse(usuarioRepository.save(usuario))
    }

    /** Autocadastro publico (`POST /api/auth/signup`): o novo usuario recebe sempre o perfil USER. */
    @Transactional
    fun cadastrar(request: SignupRequest): UsuarioResponse {
        if (usuarioRepository.existsByEmailIgnoreCase(request.email)) {
            throw RegraDeNegocioException("Ja existe um usuario com o email ${request.email}")
        }
        val usuario = mapper.paraEntidade(request, passwordEncoder.encode(request.senha))
        return mapper.paraResponse(usuarioRepository.save(usuario))
    }

    @Transactional
    fun atualizar(id: Long, request: UsuarioRequest): UsuarioResponse {
        val usuario = obter(id)
        usuarioRepository.findByEmailIgnoreCase(request.email)?.let {
            if (it.id != id) throw RegraDeNegocioException("Ja existe um usuario com o email ${request.email}")
        }
        val perdeAcessoAdmin = usuario.role == Role.ADMIN && (request.role != Role.ADMIN || !request.ativo)
        if (perdeAcessoAdmin && contarAdminsAtivos() <= 1) {
            throw RegraDeNegocioException("O sistema precisa de pelo menos um administrador ativo")
        }
        mapper.atualizarEntidade(usuario, request)
        // Senha em branco no update significa "manter a senha atual".
        request.senha?.takeIf { it.isNotBlank() }?.let { usuario.senhaHash = passwordEncoder.encode(it) }
        return mapper.paraResponse(usuarioRepository.save(usuario))
    }

    @Transactional
    fun excluir(id: Long) {
        val usuario = obter(id)
        if (usuario.role == Role.ADMIN && usuario.ativo && contarAdminsAtivos() <= 1) {
            throw RegraDeNegocioException("O sistema precisa de pelo menos um administrador ativo")
        }
        usuarioRepository.delete(usuario)
    }

    fun obter(id: Long): Usuario = usuarioRepository.findById(id)
        .orElseThrow { RecursoNaoEncontradoException("Usuario $id nao encontrado") }

    private fun contarAdminsAtivos(): Int =
        usuarioRepository.findAll().count { it.role == Role.ADMIN && it.ativo }
}
