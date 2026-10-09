package br.com.empresa.bff.exception;

import org.springframework.http.HttpStatusCode;

public class DownstreamIntegrationException extends RuntimeException {

    private final HttpStatusCode status;

    public DownstreamIntegrationException(HttpStatusCode status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatusCode getStatus() {
        return status;
    }
}
