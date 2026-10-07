package br.com.empresa.bff.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public record BloqueioCartaoServiceProperties(
        @Value("${bloqueio-cartao.service.base-url}") String baseUrl,
        @Value("${bloqueio-cartao.service.path}") String path,
        @Value("${bloqueio-cartao.service.api-key}") String apiKey,
        @Value("${bloqueio-cartao.service.timeout}") int timeout,
        @Value("${bloqueio-cartao.service.connect-timeout}") int connectTimeout
) {
}
