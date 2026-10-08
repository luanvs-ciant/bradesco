package br.com.empresa.bff.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.TimeUnit;

import feign.Client;
import feign.Retryer;
import feign.RequestInterceptor;
import feign.codec.ErrorDecoder;
import feign.okhttp.OkHttpClient;
import okhttp3.ConnectionPool;
import okhttp3.Dispatcher;
import br.com.empresa.bff.exception.IntegrationException;

public class BloqueioCartaoFeignConfig {

    @Bean
    RequestInterceptor bloqueioCartaoApiKeyInterceptor(
            @Value("${BLOQUEIO_SERVICE_API_KEY:dummy-key}") String apiKey) {
        return template -> template.header("X-Api-Key", apiKey);
    }

    @Bean
    ErrorDecoder bloqueioCartaoErrorDecoder() {
        return (methodKey, response) -> new IntegrationException(
            response.status(), "Failed to communicate with the card blocking service");
    }

    @Bean
    Client bloqueioCartaoOkHttpClient(
            @Value("${okhttp-configuracao-geral.write-timeout-millis}") long writeTimeoutMillis,
            @Value("${okhttp-configuracao-geral.retryOnConnectionFailure}") boolean retryOnConnectionFailure,
            @Value("${okhttp-connection-pool.max-idle-connections}") int maxIdleConnections,
            @Value("${okhttp-connection-pool.keep-alive-duration-segundos}") int keepAliveDurationSeconds,
            @Value("${okhttp-dispatcher.max-requests}") int maxRequests,
            @Value("${okhttp-dispatcher.max-requests-per-host}") int maxRequestsPerHost) {
        Dispatcher dispatcher = new Dispatcher();
        dispatcher.setMaxRequests(maxRequests);
        dispatcher.setMaxRequestsPerHost(maxRequestsPerHost);

        okhttp3.OkHttpClient httpClient = new okhttp3.OkHttpClient.Builder()
                .dispatcher(dispatcher)
                .connectionPool(new ConnectionPool(maxIdleConnections, keepAliveDurationSeconds, TimeUnit.SECONDS))
                .writeTimeout(writeTimeoutMillis, TimeUnit.MILLISECONDS)
                .retryOnConnectionFailure(retryOnConnectionFailure)
                .build();

        return new OkHttpClient(httpClient);
    }

    @Bean
    Retryer bloqueioCartaoRetryer() {
        return Retryer.NEVER_RETRY;
    }
}