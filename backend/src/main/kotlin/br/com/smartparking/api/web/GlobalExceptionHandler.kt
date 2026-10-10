package br.com.smartparking.api.web

import br.com.smartparking.api.common.ApiError
import br.com.smartparking.api.common.RecursoNaoEncontradoException
import br.com.smartparking.api.common.RegraDeNegocioException
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.DisabledException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

/** Traduz as excecoes da aplicacao para o corpo padrao [ApiError]. */
@RestControllerAdvice
class GlobalExceptionHandler {

    private val log = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(RecursoNaoEncontradoException::class)
    fun naoEncontrado(e: RecursoNaoEncontradoException, req: HttpServletRequest) =
        resposta(HttpStatus.NOT_FOUND, e.message ?: "Recurso nao encontrado", req)

    @ExceptionHandler(RegraDeNegocioException::class)
    fun regraDeNegocio(e: RegraDeNegocioException, req: HttpServletRequest) =
        resposta(HttpStatus.CONFLICT, e.message ?: "Operacao invalida", req)

    @ExceptionHandler(BadCredentialsException::class, DisabledException::class)
    fun credenciais(e: Exception, req: HttpServletRequest) =
        resposta(HttpStatus.UNAUTHORIZED, e.message ?: "Credenciais invalidas", req)

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun validacao(e: MethodArgumentNotValidException, req: HttpServletRequest): ResponseEntity<ApiError> {
        val campos = e.bindingResult.fieldErrors.associate { it.field to (it.defaultMessage ?: "valor invalido") }
        return resposta(HttpStatus.BAD_REQUEST, "Dados invalidos", req, campos)
    }

    @ExceptionHandler(HttpMessageNotReadableException::class, MethodArgumentTypeMismatchException::class)
    fun corpoInvalido(e: Exception, req: HttpServletRequest) =
        resposta(HttpStatus.BAD_REQUEST, "Requisicao malformada: ${e.message?.take(200)}", req)

    /** Sem este handler o catch-all abaixo transformaria um 403 legitimo em 500. */
    @ExceptionHandler(AccessDeniedException::class)
    fun acessoNegado(e: AccessDeniedException, req: HttpServletRequest) =
        resposta(HttpStatus.FORBIDDEN, e.message ?: "Acesso negado para este perfil", req)

    @ExceptionHandler(Exception::class)
    fun erroInterno(e: Exception, req: HttpServletRequest): ResponseEntity<ApiError> {
        log.error("Erro nao tratado em ${req.requestURI}", e)
        return resposta(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno no servidor", req)
    }

    private fun resposta(
        status: HttpStatus,
        mensagem: String,
        req: HttpServletRequest,
        campos: Map<String, String> = emptyMap(),
    ): ResponseEntity<ApiError> = ResponseEntity.status(status).body(
        ApiError(
            status = status.value(),
            erro = status.reasonPhrase,
            mensagem = mensagem,
            caminho = req.requestURI,
            campos = campos,
        ),
    )
}
