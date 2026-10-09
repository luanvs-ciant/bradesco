package br.com.empresa.bff.integration;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class BloqueioCartaoIntegrationTest {

    private static final String API_PATH = "/api/v1/bloqueios-cartao";
    private static final String REQUEST_PATH = "/bff/operacoes/bloqueio-cartoes";
    private static final String API_KEY = "integration-test-api-key";
    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String CORRELATION_ID = "integration-test-123";

    private static final WireMockServer wireMock =
            new WireMockServer(WireMockConfiguration.options().dynamicPort());

    @Value("${local.server.port}")
    private int port;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @DynamicPropertySource
    static void configureDownstream(DynamicPropertyRegistry registry) {
        wireMock.start();
        registry.add("bloqueio-cartao.service.base-url", wireMock::baseUrl);
        registry.add("bloqueio-cartao.service.api-key", () -> API_KEY);
        registry.add("bloqueio-cartao.service.timeout", () -> 200);
        registry.add("bloqueio-cartao.service.connect-timeout", () -> 1000);
    }

    @AfterAll
    static void stopWireMock() {
        wireMock.stop();
    }

    @BeforeEach
    void resetWireMock() {
        wireMock.resetAll();
    }

    @Test
    void shouldSendRequestToDownstreamAndReturnAcceptedOperation() throws Exception {
        wireMock.stubFor(post(urlEqualTo(API_PATH))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.ACCEPTED.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""
                                {"protocoloId":"protocol-123","status":"PROCESSING"}
                                """)));

        HttpResponse<String> response = postBlockRequest(validRequest(), CORRELATION_ID);

        assertThat(response.statusCode()).isEqualTo(HttpStatus.ACCEPTED.value());
        assertThat(response.headers().firstValue(CORRELATION_ID_HEADER)).contains(CORRELATION_ID);
        assertThat(response.body())
                .contains("\"protocoloId\":\"protocol-123\"")
                .contains("\"status\":\"PROCESSING\"");

        wireMock.verify(1, postRequestedFor(urlEqualTo(API_PATH))
                .withHeader("X-Api-Key", equalTo(API_KEY))
                .withHeader(CORRELATION_ID_HEADER, equalTo(CORRELATION_ID))
                .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON_VALUE))
                .withRequestBody(equalToJson("""
                        {
                          "cartaoId": "123456",
                          "tipoBloqueio": "DEFINITIVO",
                          "motivo": "SOLICITACAO_CLIENTE",
                          "portadores": [
                            {"portadorId": "987654"},
                            {"portadorId": "456789"}
                          ]
                        }
                        """)));
    }

    @Test
    void shouldMapDownstreamUnavailableResponseToServiceUnavailable() throws Exception {
        wireMock.stubFor(post(urlEqualTo(API_PATH))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.SERVICE_UNAVAILABLE.value())
                        .withBody("downstream internal details")));

        HttpResponse<String> response = postBlockRequest(validRequest(), CORRELATION_ID);

        assertThat(response.statusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE.value());
        assertThat(response.headers().firstValue(CORRELATION_ID_HEADER)).contains(CORRELATION_ID);
        assertThat(response.body()).contains("DOWNSTREAM_UNAVAILABLE")
                .doesNotContain("downstream internal details");
        wireMock.verify(postRequestedFor(urlEqualTo(API_PATH)));
    }

    @Test
    void shouldMapDownstreamTimeoutToGatewayTimeout() throws Exception {
        wireMock.stubFor(post(urlEqualTo(API_PATH))
                .willReturn(aResponse()
                        .withFixedDelay(1000)
                        .withStatus(HttpStatus.ACCEPTED.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""
                                {"protocoloId":"protocol-123","status":"PROCESSING"}
                                """)));

        HttpResponse<String> response = postBlockRequest(validRequest(), CORRELATION_ID);

        assertThat(response.statusCode()).isEqualTo(HttpStatus.GATEWAY_TIMEOUT.value());
        assertThat(response.headers().firstValue(CORRELATION_ID_HEADER)).contains(CORRELATION_ID);
        assertThat(response.body()).contains("DOWNSTREAM_TIMEOUT");
        wireMock.verify(postRequestedFor(urlEqualTo(API_PATH)));
    }

    private HttpResponse<String> postBlockRequest(String requestBody, String correlationId)
            throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + REQUEST_PATH))
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .header(CORRELATION_ID_HEADER, correlationId)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private String validRequest() {
        return """
                {
                  "cartaoId": "123456",
                  "tipoBloqueio": "DEFINITIVO",
                  "motivo": "SOLICITACAO_CLIENTE",
                  "portadores": [
                    {"portadorId": "987654"},
                    {"portadorId": "456789"}
                  ]
                }
                """;
    }
}
