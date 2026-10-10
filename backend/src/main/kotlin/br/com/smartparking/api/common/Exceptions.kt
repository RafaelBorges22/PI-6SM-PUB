package br.com.smartparking.api.common

/** HTTP 404: o recurso solicitado nao existe. */
class RecursoNaoEncontradoException(mensagem: String) : RuntimeException(mensagem)

/** HTTP 409: a operacao viola uma regra de negocio (duplicidade, vinculo obrigatorio, etc.). */
class RegraDeNegocioException(mensagem: String) : RuntimeException(mensagem)
