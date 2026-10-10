package br.com.empresa.bff.exception;

public enum ErrorCode {

    VALIDATION_ERROR("Erro de validação"),
    DOWNSTREAM_TIMEOUT("Tempo limite do serviço excedido"),
    DOWNSTREAM_UNAVAILABLE("Serviço temporariamente indisponível"),
    DOWNSTREAM_ERROR("Erro na integração com o serviço"),
    INTERNAL_ERROR("Erro ao processar requisição");

    private final String defaultMessage;

    ErrorCode(String defaultMessage) {
        this.defaultMessage = defaultMessage;
    }

    public String defaultMessage() {
        return defaultMessage;
    }
}
