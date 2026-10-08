package br.com.empresa.bff.exception;

public enum ErrorCode {

    VALIDATION_ERROR("Validation error"),
    INTEGRATION_ERROR("Integration with external service failed"),
    INTERNAL_ERROR("Error processing request");

    private final String defaultMessage;

    ErrorCode(String defaultMessage) {
        this.defaultMessage = defaultMessage;
    }

    public String defaultMessage() {
        return defaultMessage;
    }
}
