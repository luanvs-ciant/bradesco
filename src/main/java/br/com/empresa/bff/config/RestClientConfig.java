package br.com.empresa.bff.config;

import java.time.Duration;

import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestClient;

import br.com.empresa.bff.observability.CorrelationIdInterceptor;

@Configuration
public class RestClientConfig {

    @Bean
        public RestClient bloqueioCartaoRestClient(BloqueioCartaoServiceProperties properties,
            CorrelationIdInterceptor correlationIdInterceptor) {
        HttpClientSettings settings = HttpClientSettings.defaults()
                .withConnectTimeout(Duration.ofMillis(properties.connectTimeout()))
                .withReadTimeout(Duration.ofMillis(properties.timeout()));

        return RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(ClientHttpRequestFactoryBuilder.detect().build(settings))
                .requestInterceptor(authorizationInterceptor(properties))
                .requestInterceptor(correlationIdInterceptor)
                .build();
    }

    private ClientHttpRequestInterceptor authorizationInterceptor(BloqueioCartaoServiceProperties properties) {
        return (request, body, execution) -> {
            request.getHeaders().set("X-Api-Key", properties.apiKey());
            return execution.execute(request, body);
        };
    }

}
