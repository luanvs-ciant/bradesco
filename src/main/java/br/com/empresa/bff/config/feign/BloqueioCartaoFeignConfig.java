package br.com.empresa.bff.config.feign;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import org.slf4j.MDC;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatusCode;

import br.com.empresa.bff.exception.DownstreamIntegrationException;
import br.com.empresa.bff.observability.CorrelationId;
import feign.Client;
import feign.RequestInterceptor;
import feign.Response;
import feign.Retryer;
import feign.codec.ErrorDecoder;
import lombok.RequiredArgsConstructor;
import okhttp3.ConnectionPool;
import okhttp3.Dispatcher;

@EnableConfigurationProperties(BloqueioCartaoProperties.class)
@RequiredArgsConstructor
public class BloqueioCartaoFeignConfig {

    private final BloqueioCartaoProperties properties;

    @Bean
    public okhttp3.OkHttpClient bloqueioCartaoOkHttpClient() {
        var okhttp = properties.okhttp();
        var pool = okhttp.connectionPool();
        Dispatcher dispatcher = new Dispatcher();
        dispatcher.setMaxRequests(okhttp.dispatcher().maxRequests());
        dispatcher.setMaxRequestsPerHost(okhttp.dispatcher().maxRequestsPerHost());
        return new okhttp3.OkHttpClient.Builder()
                .writeTimeout(okhttp.writeTimeoutMillis(), TimeUnit.MILLISECONDS)
                .retryOnConnectionFailure(okhttp.retryOnConnectionFailure())
                .connectionPool(new ConnectionPool(pool.maxIdleConnections(), pool.keepAliveDurationSegundos(),
                        TimeUnit.SECONDS))
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
            template.header("X-Api-Key", properties.apiKey());
            String correlationId = MDC.get(CorrelationId.MDC_KEY);
            if (correlationId != null) {
                template.header(CorrelationId.HEADER, correlationId);
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
