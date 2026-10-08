package br.com.empresa.bff.gateway.client;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import br.com.empresa.bff.config.BloqueioCartaoServiceProperties;
import br.com.empresa.bff.gateway.dto.BloqueioCartaoDownstreamRequest;
import br.com.empresa.bff.gateway.dto.BloqueioCartaoDownstreamResponse;

@Component
public class BloqueioCartaoClient {

    private final RestClient restClient;
    private final BloqueioCartaoServiceProperties properties;

    public BloqueioCartaoClient(RestClient bloqueioCartaoRestClient, BloqueioCartaoServiceProperties properties) {
        this.restClient = bloqueioCartaoRestClient;
        this.properties = properties;
    }

    public BloqueioCartaoDownstreamResponse bloquear(BloqueioCartaoDownstreamRequest request) {
        return restClient.post()
                .uri(properties.path())
                .body(request)
                .retrieve()
                .body(BloqueioCartaoDownstreamResponse.class);
    }
}
