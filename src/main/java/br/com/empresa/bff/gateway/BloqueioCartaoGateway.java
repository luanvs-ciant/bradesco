package br.com.empresa.bff.gateway;

import java.nio.charset.StandardCharsets;

import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import br.com.empresa.bff.config.BloqueioCartaoServiceProperties;
import br.com.empresa.bff.domain.model.BloqueioCartao;
import br.com.empresa.bff.domain.model.ResultadoBloqueioCartao;
import br.com.empresa.bff.gateway.dto.BloqueioCartaoDownstreamResponse;
import br.com.empresa.bff.gateway.mapper.BloqueioCartaoGatewayMapper;

@Component
public class BloqueioCartaoGateway
        implements br.com.empresa.bff.domain.gateway.BloqueioCartaoGateway {

    private final RestClient restClient;
    private final BloqueioCartaoGatewayMapper mapper;
    private final BloqueioCartaoServiceProperties properties;

    public BloqueioCartaoGateway(
            RestClient bloqueioCartaoRestClient,
            BloqueioCartaoGatewayMapper mapper,
            BloqueioCartaoServiceProperties properties) {
        this.restClient = bloqueioCartaoRestClient;
        this.mapper = mapper;
        this.properties = properties;
    }

    @Override
    public ResultadoBloqueioCartao bloquear(BloqueioCartao bloqueio) {
        var request = mapper.toDownstreamRequest(bloqueio);

        try {
            var response = restClient.post()
                    .uri(properties.path())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (httpRequest, httpResponse) -> {
                        throw new BloqueioCartaoIntegrationException(
                                httpResponse.getStatusCode(),
                                readErrorBody(httpResponse));
                    })
                    .body(BloqueioCartaoDownstreamResponse.class);

            return mapper.toResult(response);
        } catch (RuntimeException exception) {
            if (exception instanceof BloqueioCartaoIntegrationException) {
                throw exception;
            }
            throw new BloqueioCartaoIntegrationException(
                    HttpStatusCode.valueOf(502),
                    "Serviço de bloqueio indisponível");
        }
    }

    private String readErrorBody(org.springframework.http.client.ClientHttpResponse response) {
        try (var body = response.getBody()) {
            return new String(body.readAllBytes(), StandardCharsets.UTF_8);
        } catch (java.io.IOException exception) {
            return "Erro ao comunicar com o serviço de bloqueio";
        }
    }
}
