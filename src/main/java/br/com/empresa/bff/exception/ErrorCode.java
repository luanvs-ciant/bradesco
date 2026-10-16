package br.com.empresa.bff.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    VALIDATION_ERROR("Erro de validação"),
    DOWNSTREAM_TIMEOUT("Tempo limite do serviço excedido"),
    DOWNSTREAM_UNAVAILABLE("Serviço temporariamente indisponível"),
    DOWNSTREAM_ERROR("Erro na integração com o serviço"),
    INTERNAL_ERROR("Erro ao processar requisição");

    private final String defaultMessage;
}
