package br.com.empresa.bff.config.feign;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

@Validated
@ConfigurationProperties("bloqueio-cartao")
public record BloqueioCartaoProperties(
        @NotBlank String path,
        @NotBlank(message = "Defina a variavel de ambiente BLOQUEIO_SERVICE_API_KEY") String apiKey,
        OkHttp okhttp) {

    public record OkHttp(
            int writeTimeoutMillis,
            boolean retryOnConnectionFailure,
            ConnectionPool connectionPool,
            Dispatcher dispatcher) {
    }

    public record ConnectionPool(int maxIdleConnections, long keepAliveDurationSegundos) {
    }

    public record Dispatcher(int maxRequests, int maxRequestsPerHost) {
    }
}
