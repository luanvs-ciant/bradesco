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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.http.Fault;

/**
 * BFF real (HTTP -> controller -> service -> gateway -> Feign/OkHttp) contra o downstream simulado pelo WireMock.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class BloqueioCartaoIntegrationTest {

    private static final String API_PATH = "/api/v1/bloqueios-cartao";
    private static final String REQUEST_PATH = "/bff/operacoes/bloqueio-cartoes";
    private static final String API_KEY = "integration-test-api-key";
    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String CORRELATION_ID = "integration-test-123";
    private static final String DOWNSTREAM_DETAILS = "downstream internal details";
    // Folga para o aquecimento do OkHttp na primeira chamada da JVM, que pode passar de 1s.
    private static final int READ_TIMEOUT_MILLIS = 2000;

    private static final WireMockServer wireMock =
            new WireMockServer(WireMockConfiguration.options().dynamicPort());

    @Value("${local.server.port}")
    private int port;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @DynamicPropertySource
    static void configureDownstream(DynamicPropertyRegistry registry) {
        wireMock.start();
        registry.add("spring.cloud.openfeign.client.config.bloqueioCartao.url", wireMock::baseUrl);
        registry.add("spring.cloud.openfeign.client.config.bloqueioCartao.readTimeout", () -> READ_TIMEOUT_MILLIS);
        registry.add("spring.cloud.openfeign.client.config.bloqueioCartao.connectTimeout", () -> 1000);
        registry.add("bloqueio-cartao.path", () -> API_PATH);
        registry.add("bloqueio-cartao.api-key", () -> API_KEY);
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
        stubDownstream(acceptedResponse());

        HttpResponse<String> response = postBlockRequest(CORRELATION_ID);

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
    void shouldGenerateCorrelationIdAndPropagateItWhenClientOmitsHeader() throws Exception {
        stubDownstream(acceptedResponse());

        HttpResponse<String> response = postBlockRequest(null);

        assertThat(response.statusCode()).isEqualTo(HttpStatus.ACCEPTED.value());
        String generatedId = response.headers().firstValue(CORRELATION_ID_HEADER).orElseThrow();
        wireMock.verify(1, postRequestedFor(urlEqualTo(API_PATH))
                .withHeader(CORRELATION_ID_HEADER, equalTo(generatedId)));
    }

    @ParameterizedTest
    @CsvSource({
        "400,502,DOWNSTREAM_ERROR",
        "500,502,DOWNSTREAM_ERROR",
        "503,503,DOWNSTREAM_UNAVAILABLE",
        "504,504,DOWNSTREAM_TIMEOUT"})
    void shouldMapDownstreamHttpErrorsWithoutRetryOrLeakingDetails(
            int downstreamStatus, int expectedStatus, String expectedCode) throws Exception {
        stubDownstream(aResponse().withStatus(downstreamStatus).withBody(DOWNSTREAM_DETAILS));

        assertSafeError(postBlockRequest(CORRELATION_ID), expectedStatus, expectedCode);
    }

    @Test
    void shouldMapReadTimeoutToGatewayTimeout() throws Exception {
        stubDownstream(acceptedResponse().withFixedDelay(READ_TIMEOUT_MILLIS * 2));

        assertSafeError(postBlockRequest(CORRELATION_ID), HttpStatus.GATEWAY_TIMEOUT.value(), "DOWNSTREAM_TIMEOUT");
    }

    @Test
    void shouldMapConnectionFailureToServiceUnavailable() throws Exception {
        stubDownstream(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER));

        assertSafeError(postBlockRequest(CORRELATION_ID),
                HttpStatus.SERVICE_UNAVAILABLE.value(), "DOWNSTREAM_UNAVAILABLE");
    }

    @Test
    void shouldMapInvalidDownstreamPayloadToBadGateway() throws Exception {
        stubDownstream(aResponse()
                .withStatus(HttpStatus.ACCEPTED.value())
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .withBody(DOWNSTREAM_DETAILS));

        assertSafeError(postBlockRequest(CORRELATION_ID), HttpStatus.BAD_GATEWAY.value(), "DOWNSTREAM_ERROR");
    }

    private void assertSafeError(HttpResponse<String> response, int expectedStatus, String expectedCode)
            throws InterruptedException {
        assertThat(response.statusCode()).isEqualTo(expectedStatus);
        assertThat(response.headers().firstValue(CORRELATION_ID_HEADER)).contains(CORRELATION_ID);
        assertThat(response.body())
                .contains(expectedCode)
                .contains("\"traceId\":\"" + CORRELATION_ID + "\"")
                .doesNotContain(DOWNSTREAM_DETAILS);
        awaitRequestJournal();
        // Retryer.NEVER_RETRY: exatamente uma chamada ao downstream, mesmo em falha.
        wireMock.verify(1, postRequestedFor(urlEqualTo(API_PATH)));
    }

    // O WireMock pode registrar a requisicao depois de o cliente ja ter desistido (timeout/fault).
    private void awaitRequestJournal() throws InterruptedException {
        long deadline = System.currentTimeMillis() + 5000;
        while (wireMock.findAll(postRequestedFor(urlEqualTo(API_PATH))).isEmpty()
                && System.currentTimeMillis() < deadline) {
            Thread.sleep(50);
        }
    }

    private void stubDownstream(ResponseDefinitionBuilder response) {
        wireMock.stubFor(post(urlEqualTo(API_PATH)).willReturn(response));
    }

    private ResponseDefinitionBuilder acceptedResponse() {
        return aResponse()
                .withStatus(HttpStatus.ACCEPTED.value())
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .withBody("""
                        {"protocoloId":"protocol-123","status":"PROCESSING"}
                        """);
    }

    private HttpResponse<String> postBlockRequest(String correlationId) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + REQUEST_PATH))
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .POST(HttpRequest.BodyPublishers.ofString("""
                        {
                          "cartaoId": "123456",
                          "tipoBloqueio": "DEFINITIVO",
                          "motivo": "SOLICITACAO_CLIENTE",
                          "portadores": [
                            {"portadorId": "987654"},
                            {"portadorId": "456789"}
                          ]
                        }
                        """));
        if (correlationId != null) {
            request.header(CORRELATION_ID_HEADER, correlationId);
        }
        return httpClient.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }
}
