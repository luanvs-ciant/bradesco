package br.com.empresa.bff.gateway;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import br.com.empresa.bff.config.BloqueioCartaoServiceProperties;
import br.com.empresa.bff.domain.model.BloqueioCartao;
import br.com.empresa.bff.domain.model.Portador;
import br.com.empresa.bff.domain.model.ResultadoBloqueioCartao;
import br.com.empresa.bff.domain.model.TipoBloqueio;
import br.com.empresa.bff.exception.DownstreamIntegrationException;
import br.com.empresa.bff.gateway.mapper.BloqueioCartaoGatewayMapper;

class BloqueioCartaoGatewayTest {

    private static final String BASE_URL = "http://downstream.local";
    private static final String PATH = "/api/v1/bloqueios-cartao";
    private static final String API_KEY = "test-api-key";
    private static final String CORRELATION_ID = "corr-123";

    private MockRestServiceServer mockServer;
    private BloqueioCartaoGateway gateway;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL)
                .requestInterceptor((request, body, execution) -> {
                    request.getHeaders().set("X-Api-Key", API_KEY);
                    request.getHeaders().set("X-Correlation-ID", MDC.get("correlationId"));
                    return execution.execute(request, body);
                });
        mockServer = MockRestServiceServer.bindTo(builder).build();

        var properties = new BloqueioCartaoServiceProperties(BASE_URL, PATH, API_KEY, 3000, 1000);
        gateway = new BloqueioCartaoGateway(builder.build(), new BloqueioCartaoGatewayMapper(), properties);
    }

    @Test
    void mapsDomainToDownstreamRequestAndPostsToConfiguredEndpoint() {
        MDC.put("correlationId", CORRELATION_ID);
        mockServer.expect(requestTo(BASE_URL + PATH))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Api-Key", API_KEY))
                .andExpect(header("X-Correlation-ID", CORRELATION_ID))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {
                          "cartaoId": "123456",
                          "tipoBloqueio": "DEFINITIVO",
                          "motivo": "SOLICITACAO_CLIENTE",
                          "portadores": [{"portadorId": "987654"}, {"portadorId": "456789"}]
                        }
                        """))
                .andRespond(withSuccess("""
                        {"protocoloId": "abc123", "status": "PROCESSING"}
                        """, MediaType.APPLICATION_JSON));

        var domain = new BloqueioCartao(
                "123456",
                TipoBloqueio.DEFINITIVO,
                "SOLICITACAO_CLIENTE",
                List.of(new Portador("987654"), new Portador("456789")));

        var result = gateway.bloquear(domain);

        assertEquals(new ResultadoBloqueioCartao("abc123", "PROCESSING"), result);
        mockServer.verify();
    }

    @Test
    void propagatesDownstreamIntegrationFailures() {
        mockServer.expect(requestTo(BASE_URL + PATH))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withServerError().body("servico indisponivel"));

        var domain = new BloqueioCartao(
                "123456",
                TipoBloqueio.TEMPORARIO,
                null,
                List.of(new Portador("987654")));

        var exception = assertThrows(DownstreamIntegrationException.class,
                () -> gateway.bloquear(domain));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatus());
        assertEquals("servico indisponivel", exception.getMessage());
        mockServer.verify();
    }
}
