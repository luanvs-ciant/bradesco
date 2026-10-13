package br.com.empresa.bff.exception;

import org.springframework.http.HttpStatusCode;

public class DownstreamIntegrationException extends RuntimeException {

    private final HttpStatusCode status;

    public DownstreamIntegrationException(HttpStatusCode status, String message) {
        this(status, message, null);
    }

    public DownstreamIntegrationException(HttpStatusCode status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public HttpStatusCode getStatus() {
        return status;
    }
}
