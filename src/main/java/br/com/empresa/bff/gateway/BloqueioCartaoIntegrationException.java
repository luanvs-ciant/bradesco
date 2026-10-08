package br.com.empresa.bff.gateway;

import org.springframework.http.HttpStatusCode;

public class BloqueioCartaoIntegrationException extends RuntimeException {

    private final HttpStatusCode status;

    public BloqueioCartaoIntegrationException(HttpStatusCode status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatusCode getStatus() {
        return status;
    }
}
