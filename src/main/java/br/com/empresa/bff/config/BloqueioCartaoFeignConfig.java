package br.com.empresa.bff.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import feign.RequestInterceptor;
import feign.codec.ErrorDecoder;
import br.com.empresa.bff.exception.IntegrationException;

@Configuration(proxyBeanMethods = false)
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
}