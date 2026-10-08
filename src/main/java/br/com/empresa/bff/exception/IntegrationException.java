package br.com.empresa.bff.exception;

public class IntegrationException extends RuntimeException {

    private final int downstreamStatus;

    public IntegrationException(int downstreamStatus, String message) {
        super(message);
        this.downstreamStatus = downstreamStatus;
    }

    public int downstreamStatus() {
        return downstreamStatus;
    }
}