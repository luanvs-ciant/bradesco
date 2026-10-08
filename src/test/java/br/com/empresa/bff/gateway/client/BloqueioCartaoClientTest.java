package br.com.empresa.bff.gateway.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import br.com.empresa.bff.domain.gateway.BloqueioCartaoGateway;
import br.com.empresa.bff.gateway.dto.BloqueioCartaoDownstreamRequest;
import br.com.empresa.bff.gateway.dto.BloqueioCartaoDownstreamRequest.PortadorDownstreamRequest;
import br.com.empresa.bff.gateway.dto.BloqueioCartaoDownstreamResponse;

@SpringBootTest
class BloqueioCartaoClientTest {

        private static final String PATH = "/api/v1/bloqueios-cartao";
        private static final String API_KEY = "test-api-key";
        private static final String CARTAO_ID = "123456";
        private static final String TIPO_BLOQUEIO_DEFINITIVO = "DEFINITIVO";
        private static final String MOTIVO = "SOLICITACAO_CLIENTE";
        private static final String PORTADOR_ID = "987654";
        private static final String PROTOCOLO_ID = "8f6d2c10";
        private static final String STATUS_PROCESSING = "PROCESSING";
        private static final String API_KEY_HEADER = "X-Api-Key";
        private static final String RESPONSE_BODY = """
                        {"protocoloId": "%s", "status": "%s"}
                        """.formatted(PROTOCOLO_ID, STATUS_PROCESSING);

        private static final AtomicReference<RecordedRequest> LAST_REQUEST = new AtomicReference<>();
        private static final HttpServer SERVER = startServer();

        @Autowired
        private BloqueioCartaoClient client;

        private final ObjectMapper objectMapper = new ObjectMapper();

        @MockitoBean
        private BloqueioCartaoGateway gateway;

        @DynamicPropertySource
        static void registerProperties(DynamicPropertyRegistry registry) {
                registry.add("spring.cloud.openfeign.client.config.bloqueioCartao.url",
                                () -> "http://localhost:" + SERVER.getAddress().getPort());
                registry.add("bloqueio-cartao.path", () -> PATH);
                registry.add("BLOQUEIO_SERVICE_API_KEY", () -> API_KEY);
        }

        @BeforeEach
        void setUp() {
                LAST_REQUEST.set(null);
        }

        @AfterAll
        static void stopServer() {
                SERVER.stop(0);
        }

        @Test
        void deveEnviarPostComPayloadEApiKeyEDesserializarResposta() throws Exception {
                BloqueioCartaoDownstreamRequest request = new BloqueioCartaoDownstreamRequest(
                                CARTAO_ID,
                                TIPO_BLOQUEIO_DEFINITIVO,
                                MOTIVO,
                                List.of(new PortadorDownstreamRequest(PORTADOR_ID))
                );

                BloqueioCartaoDownstreamResponse response = client.bloquear(request);
                RecordedRequest recordedRequest = LAST_REQUEST.get();
                assertThat(recordedRequest).isNotNull();
                JsonNode payload = objectMapper.readTree(recordedRequest.body());

                assertThat(response.protocoloId()).isEqualTo(PROTOCOLO_ID);
                assertThat(response.status()).isEqualTo(STATUS_PROCESSING);
                assertThat(recordedRequest.method()).isEqualTo("POST");
                assertThat(recordedRequest.path()).isEqualTo(PATH);
                assertThat(recordedRequest.apiKey()).isEqualTo(API_KEY);
                assertThat(recordedRequest.contentType()).contains("application/json");
                assertThat(payload.get("cartaoId").asText()).isEqualTo(CARTAO_ID);
                assertThat(payload.get("tipoBloqueio").asText()).isEqualTo(TIPO_BLOQUEIO_DEFINITIVO);
                assertThat(payload.get("motivo").asText()).isEqualTo(MOTIVO);
                assertThat(payload.get("portadores").get(0).get("portadorId").asText()).isEqualTo(PORTADOR_ID);
        }

        private static HttpServer startServer() {
                try {
                        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
                        server.createContext(PATH, BloqueioCartaoClientTest::handleRequest);
                        server.start();
                        return server;
                } catch (IOException exception) {
                        throw new ExceptionInInitializerError(exception);
                }
        }

        private static void handleRequest(HttpExchange exchange) throws IOException {
                LAST_REQUEST.set(new RecordedRequest(
                                exchange.getRequestMethod(),
                                exchange.getRequestURI().getPath(),
                                exchange.getRequestHeaders().getFirst(API_KEY_HEADER),
                                exchange.getRequestHeaders().getFirst("Content-Type"),
                                new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)));

                byte[] response = RESPONSE_BODY.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.length);
                try (OutputStream responseBody = exchange.getResponseBody()) {
                        responseBody.write(response);
                }
        }

        private record RecordedRequest(String method, String path, String apiKey, String contentType, String body) {
        }
}
