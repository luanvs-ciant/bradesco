package br.com.empresa.bff.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import feign.RequestInterceptor;

@Configuration(proxyBeanMethods = false)
public class BloqueioCartaoFeignConfig {

    @Bean
    RequestInterceptor bloqueioCartaoApiKeyInterceptor(
            @Value("${BLOQUEIO_SERVICE_API_KEY:dummy-key}") String apiKey) {
        return template -> template.header("X-Api-Key", apiKey);
    }
}