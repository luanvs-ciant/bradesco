package br.com.empresa.bff.observability;

public final class CorrelationId {

    public static final String HEADER = "X-Correlation-ID";
    public static final String MDC_KEY = "correlationId";

    private CorrelationId() {
    }
}
