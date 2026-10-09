package br.com.empresa.bff.config.feign;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatusCode;

import br.com.empresa.bff.exception.DownstreamIntegrationException;
import feign.Client;
import feign.RequestInterceptor;
import feign.Response;
import feign.Retryer;
import feign.codec.ErrorDecoder;
import okhttp3.ConnectionPool;
import okhttp3.Dispatcher;

public class BloqueioCartaoFeignConfig {

    private final String apiKey;
    private final int writeTimeout;
    private final boolean retryOnConnectionFailure;
    private final int maxIdleConnections;
    private final long keepAliveDurationSeconds;
    private final int maxRequests;
    private final int maxRequestsPerHost;

    public BloqueioCartaoFeignConfig(
            @Value("${bloqueio-cartao.api-key}") String apiKey,
            @Value("${okhttp-configuracao-geral.write-timeout-millis}") int writeTimeout,
            @Value("${okhttp-configuracao-geral.retryOnConnectionFailure}") boolean retryOnConnectionFailure,
            @Value("${okhttp-connection-pool.max-idle-connections}") int maxIdleConnections,
            @Value("${okhttp-connection-pool.keep-alive-duration-segundos}") long keepAliveDurationSeconds,
            @Value("${okhttp-dispatcher.max-requests}") int maxRequests,
            @Value("${okhttp-dispatcher.max-requests-per-host}") int maxRequestsPerHost) {
        this.apiKey = apiKey;
        this.writeTimeout = writeTimeout;
        this.retryOnConnectionFailure = retryOnConnectionFailure;
        this.maxIdleConnections = maxIdleConnections;
        this.keepAliveDurationSeconds = keepAliveDurationSeconds;
        this.maxRequests = maxRequests;
        this.maxRequestsPerHost = maxRequestsPerHost;
    }

    @Bean
    public okhttp3.OkHttpClient bloqueioCartaoOkHttpClient() {
        Dispatcher dispatcher = new Dispatcher();
        dispatcher.setMaxRequests(maxRequests);
        dispatcher.setMaxRequestsPerHost(maxRequestsPerHost);
        return new okhttp3.OkHttpClient.Builder()
                .writeTimeout(writeTimeout, TimeUnit.MILLISECONDS)
                .retryOnConnectionFailure(retryOnConnectionFailure)
                .connectionPool(new ConnectionPool(maxIdleConnections, keepAliveDurationSeconds, TimeUnit.SECONDS))
                .dispatcher(dispatcher)
                .build();
    }

    @Bean
    public Client bloqueioCartaoFeignClient(okhttp3.OkHttpClient bloqueioCartaoOkHttpClient) {
        return new feign.okhttp.OkHttpClient(bloqueioCartaoOkHttpClient);
    }

    @Bean
    public Retryer bloqueioCartaoRetryer() {
        return Retryer.NEVER_RETRY;
    }

    @Bean
    public RequestInterceptor bloqueioCartaoRequestInterceptor() {
        return template -> {
            template.header("X-Api-Key", apiKey);
            String correlationId = MDC.get("correlationId");
            if (correlationId != null) {
                template.header("X-Correlation-ID", correlationId);
            }
        };
    }

    @Bean
    public ErrorDecoder bloqueioCartaoErrorDecoder() {
        return (methodKey, response) -> new DownstreamIntegrationException(
                HttpStatusCode.valueOf(response.status()), readErrorBody(response));
    }

    private String readErrorBody(Response response) {
        if (response.body() == null) {
            return "";
        }
        try (var body = response.body(); var stream = body.asInputStream()) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            return "Erro ao comunicar com o servi\u00e7o de bloqueio";
        }
    }
}