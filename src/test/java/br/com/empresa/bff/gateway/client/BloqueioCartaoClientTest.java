package br.com.empresa.bff.gateway.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import br.com.empresa.bff.config.BloqueioCartaoServiceProperties;
import br.com.empresa.bff.gateway.BloqueioCartaoGateway;
import br.com.empresa.bff.gateway.dto.BloqueioCartaoDownstreamRequest;
import br.com.empresa.bff.gateway.dto.BloqueioCartaoDownstreamRequest.PortadorDownstreamRequest;
import br.com.empresa.bff.gateway.dto.BloqueioCartaoDownstreamResponse;
import br.com.empresa.bff.gateway.mapper.BloqueioCartaoGatewayMapper;

class BloqueioCartaoClientTest {

    private static final String BASE_URL = "http://downstream.local";
    private static final String PATH = "/api/v1/bloqueios-cartao";
    private static final String API_KEY = "test-api-key";

    private static final String CARTAO_ID = "123456";
    private static final String TIPO_BLOQUEIO_DEFINITIVO = "DEFINITIVO";
    private static final String TIPO_BLOQUEIO_TEMPORARIO = "TEMPORARIO";
    private static final String MOTIVO = "SOLICITACAO_CLIENTE";
    private static final String PORTADOR_ID = "987654";
    private static final String PROTOCOLO_ID = "8f6d2c10";
    private static final String STATUS_PROCESSING = "PROCESSING";

    private static final String CORRELATION_ID_MDC_KEY = "correlationId";
    private static final String USUARIO_ID_MDC_KEY = "usuarioId";
    private static final String CORRELATION_ID_VALUE = "corr-123";
    private static final String USUARIO_ID_VALUE = "user-456";

    private static final String API_KEY_HEADER = "X-Api-Key";
    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String USUARIO_ID_HEADER = "X-Usuario-Id";

    private static final String OUTRO_PROTOCOLO_ID = "abc";

    private MockRestServiceServer mockServer;
    private BloqueioCartaoGateway gateway;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        this.mockServer = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();

        BloqueioCartaoServiceProperties properties =
                new BloqueioCartaoServiceProperties(BASE_URL, PATH, API_KEY, 5000, 3000);

        this.gateway = new BloqueioCartaoGateway(restClient, new BloqueioCartaoGatewayMapper(), properties);
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void deveEnviarMetodoRotaPayloadEDesserializarResposta() {
        mockServer.expect(requestTo(BASE_URL + PATH))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {
                          "cartaoId": "%s",
                          "tipoBloqueio": "%s",
                          "motivo": "%s",
                          "portadores": [
                            {"portadorId": "%s"}
                          ]
                        }
                        """.formatted(CARTAO_ID, TIPO_BLOQUEIO_DEFINITIVO, MOTIVO, PORTADOR_ID)))
                .andRespond(withSuccess("""
                        {
                          "protocoloId": "%s",
                          "status": "%s"
                        }
                        """.formatted(PROTOCOLO_ID, STATUS_PROCESSING), MediaType.APPLICATION_JSON));

        BloqueioCartaoDownstreamRequest request = new BloqueioCartaoDownstreamRequest(
                CARTAO_ID,
                TIPO_BLOQUEIO_DEFINITIVO,
                MOTIVO,
                List.of(new PortadorDownstreamRequest(PORTADOR_ID))
        );

        var response = gateway.bloquear(
                new br.com.empresa.bff.domain.model.BloqueioCartao(
                        CARTAO_ID,
                        br.com.empresa.bff.domain.model.TipoBloqueio.valueOf(TIPO_BLOQUEIO_DEFINITIVO),
                        MOTIVO,
                        List.of(new br.com.empresa.bff.domain.model.Portador(PORTADOR_ID))));

        assertThat(response.protocoloId()).isEqualTo(PROTOCOLO_ID);
        assertThat(response.status()).isEqualTo(STATUS_PROCESSING);
        mockServer.verify();
    }

    @Test
    void devePropagarHeadersDeAutenticacaoETrace() {
        MDC.put(CORRELATION_ID_MDC_KEY, CORRELATION_ID_VALUE);
        MDC.put(USUARIO_ID_MDC_KEY, USUARIO_ID_VALUE);

        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL)
                .requestInterceptor((req, body, exec) -> {
                    req.getHeaders().set(API_KEY_HEADER, API_KEY);
                    req.getHeaders().set(CORRELATION_ID_HEADER, MDC.get(CORRELATION_ID_MDC_KEY));
                    req.getHeaders().set(USUARIO_ID_HEADER, MDC.get(USUARIO_ID_MDC_KEY));
                    return exec.execute(req, body);
                });
        MockRestServiceServer interceptedServer = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();

        BloqueioCartaoServiceProperties properties =
                new BloqueioCartaoServiceProperties(BASE_URL, PATH, API_KEY, 5000, 3000);
        BloqueioCartaoGateway interceptedGateway = new BloqueioCartaoGateway(restClient, new BloqueioCartaoGatewayMapper(), properties);

        interceptedServer.expect(requestTo(BASE_URL + PATH))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(API_KEY_HEADER, API_KEY))
                .andExpect(header(CORRELATION_ID_HEADER, CORRELATION_ID_VALUE))
                .andExpect(header(USUARIO_ID_HEADER, USUARIO_ID_VALUE))
                .andRespond(withSuccess("""
                        {"protocoloId": "%s", "status": "%s"}
                        """.formatted(OUTRO_PROTOCOLO_ID, STATUS_PROCESSING), MediaType.APPLICATION_JSON));

        BloqueioCartaoDownstreamRequest request = new BloqueioCartaoDownstreamRequest(
                CARTAO_ID, TIPO_BLOQUEIO_TEMPORARIO, null, List.of(new PortadorDownstreamRequest(PORTADOR_ID)));

        interceptedGateway.bloquear(new br.com.empresa.bff.domain.model.BloqueioCartao(CARTAO_ID, br.com.empresa.bff.domain.model.TipoBloqueio.valueOf(TIPO_BLOQUEIO_TEMPORARIO), null, List.of(new br.com.empresa.bff.domain.model.Portador(PORTADOR_ID))));

        interceptedServer.verify();
    }
}
