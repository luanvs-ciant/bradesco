package br.com.empresa.bff.exception;

import org.springframework.http.HttpStatusCode;

import lombok.Getter;

@Getter
public class DownstreamIntegrationException extends RuntimeException {

    private final HttpStatusCode status;

    public DownstreamIntegrationException(HttpStatusCode status, String message) {
        this(status, message, null);
    }

    public DownstreamIntegrationException(HttpStatusCode status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }
}
