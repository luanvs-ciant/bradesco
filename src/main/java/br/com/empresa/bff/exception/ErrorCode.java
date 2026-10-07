package br.com.empresa.bff.exception;

public enum ErrorCode {

    VALIDATION_ERROR("Erro de validação"),
    INTERNAL_ERROR("Erro ao processar requisição");

    private final String defaultMessage;

    ErrorCode(String defaultMessage) {
        this.defaultMessage = defaultMessage;
    }

    public String defaultMessage() {
        return defaultMessage;
    }
}
