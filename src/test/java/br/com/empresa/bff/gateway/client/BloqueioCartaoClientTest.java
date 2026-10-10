package br.com.empresa.bff.gateway.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.cloud.openfeign.FeignClientFactory;
import org.springframework.cloud.openfeign.FeignClientProperties;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import br.com.empresa.bff.config.feign.BloqueioCartaoFeignConfig;
import br.com.empresa.bff.domain.model.BloqueioCartao;
import br.com.empresa.bff.domain.model.Portador;
import br.com.empresa.bff.domain.model.TipoBloqueio;
import br.com.empresa.bff.exception.DownstreamIntegrationException;
import br.com.empresa.bff.exception.GlobalExceptionHandler;
import br.com.empresa.bff.gateway.BloqueioCartaoGatewayImpl;
import feign.Client;
import feign.Request;
import feign.RequestTemplate;
import feign.Response;
import feign.Retryer;
import feign.codec.ErrorDecoder;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okhttp3.mockwebserver.SocketPolicy;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
class BloqueioCartaoClientTest {

        private static final MockWebServer SERVER = new MockWebServer();
        private static final String PATH = "/test/bloqueios-cartao";
        private static final String API_KEY = "test-api-key";
        private static final String CARTAO_ID = "123456";
        private static final String MOTIVO = "SOLICITACAO_CLIENTE";
        private static final String PORTADOR_ID = "987654";
        private static final String PROTOCOLO_ID = "8f6d2c10";
        private static final String STATUS_PROCESSING = "PROCESSING";
        private static final String CORRELATION_ID_VALUE = "corr-123";
        private static final int READ_TIMEOUT = 500;
        private static final int CONNECT_TIMEOUT = 300;

        @Autowired
        private BloqueioCartaoGatewayImpl gateway;

        @Autowired
        private FeignClientFactory clientFactory;

        @Autowired
        private ApplicationContext applicationContext;

        @Autowired
        private JsonMapper jsonMapper;

        @Autowired
        private GlobalExceptionHandler handler;

        @DynamicPropertySource
        static void properties(DynamicPropertyRegistry registry) {
                registry.add("spring.cloud.openfeign.client.config.bloqueioCartao.url", () -> SERVER.url("/").toString());
                registry.add("spring.cloud.openfeign.client.config.bloqueioCartao.readTimeout", () -> READ_TIMEOUT);
                registry.add("spring.cloud.openfeign.client.config.bloqueioCartao.connectTimeout", () -> CONNECT_TIMEOUT);
                registry.add("bloqueio-cartao.path", () -> PATH);
                registry.add("bloqueio-cartao.api-key", () -> API_KEY);
                registry.add("okhttp-configuracao-geral.write-timeout-millis", () -> 321);
                registry.add("okhttp-dispatcher.max-requests", () -> 12);
                registry.add("okhttp-dispatcher.max-requests-per-host", () -> 2);
                registry.add("okhttp-connection-pool.max-idle-connections", () -> 1);
        }

        @AfterAll
        static void stopServer() throws IOException {
                SERVER.shutdown();
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
        void deveEnviarMetodoRotaPayloadEDesserializarResposta() throws InterruptedException {
                SERVER.enqueue(success());

                var response = gateway.bloquear(bloqueio());
                var request = takeRequest();

                assertThat(request.getMethod()).isEqualTo("POST");
                assertThat(request.getPath()).isEqualTo(PATH);
                assertThat(request.getHeader("Content-Type")).startsWith("application/json");
                assertThat(jsonMapper.readTree(request.getBody().readUtf8())).isEqualTo(jsonMapper.readTree("""
                        {
                          "cartaoId": "%s",
                                                  "tipoBloqueio": "DEFINITIVO",
                          "motivo": "%s",
                                                  "portadores": [{"portadorId": "%s"}]
                        }
                                                """.formatted(CARTAO_ID, MOTIVO, PORTADOR_ID)));
        assertThat(response.protocoloId()).isEqualTo(PROTOCOLO_ID);
        assertThat(response.status()).isEqualTo(STATUS_PROCESSING);
    }

    @Test
        void devePropagarHeadersDeAutenticacaoETrace() throws InterruptedException {
                MDC.put("correlationId", CORRELATION_ID_VALUE);
                MDC.put("usuarioId", "user-456");
                SERVER.enqueue(success());

                gateway.bloquear(bloqueio());

                var request = takeRequest();
                assertThat(request.getHeader("X-Api-Key")).isEqualTo(API_KEY);
                assertThat(request.getHeader("X-Correlation-ID")).isEqualTo(CORRELATION_ID_VALUE);
                assertThat(request.getHeader("X-Usuario-Id")).isNull();
        }

        @Test
        void deveOmitirCorrelacaoAusente() throws InterruptedException {
                MDC.clear();
                SERVER.enqueue(success());

                gateway.bloquear(bloqueio());

                var request = takeRequest();
                assertThat(request.getHeader("X-Api-Key")).isEqualTo(API_KEY);
                assertThat(request.getHeader("X-Correlation-ID")).isNull();
        }

        @Test
        void deveUsarOkHttpEConfiguracoesSomenteNoClient() {
                assertThat(clientFactory.getInstance("bloqueioCartao", Client.class))
                                .isInstanceOf(feign.okhttp.OkHttpClient.class);
                var transport = clientFactory.getInstance("bloqueioCartao", okhttp3.OkHttpClient.class);
                assertThat(transport.writeTimeoutMillis()).isEqualTo(321);
                assertThat(transport.dispatcher().getMaxRequests()).isEqualTo(12);
                assertThat(transport.dispatcher().getMaxRequestsPerHost()).isEqualTo(2);
                assertThat(transport.retryOnConnectionFailure()).isFalse();
                var properties = applicationContext.getBean(FeignClientProperties.class)
                                .getConfig().get("bloqueioCartao");
                assertThat(properties.getConnectTimeout()).isEqualTo(CONNECT_TIMEOUT);
                assertThat(properties.getReadTimeout()).isEqualTo(READ_TIMEOUT);
                assertThat(properties.getUrl()).isEqualTo(SERVER.url("/").toString());
                assertThat(clientFactory.getInstance("bloqueioCartao", Retryer.class)).isSameAs(Retryer.NEVER_RETRY);
                assertThat(applicationContext.getBeansOfType(Client.class)).isEmpty();
                assertThat(applicationContext.getBeansOfType(ErrorDecoder.class)).isEmpty();
        }

        @Test
        void deveReutilizarConexaoDoPool() throws InterruptedException {
                SERVER.enqueue(success());
                SERVER.enqueue(success());

                gateway.bloquear(bloqueio());
                var firstRequest = takeRequest();
                gateway.bloquear(bloqueio());
                var secondRequest = takeRequest();

                assertThat(secondRequest.getSequenceNumber()).isEqualTo(firstRequest.getSequenceNumber() + 1);
        }

        @ParameterizedTest
        @ValueSource(strings = {"bloqueio-cartao.api-key", "okhttp-configuracao-geral.write-timeout-millis",
                        "okhttp-configuracao-geral.retryOnConnectionFailure", "okhttp-connection-pool.max-idle-connections",
                        "okhttp-connection-pool.keep-alive-duration-segundos", "okhttp-dispatcher.max-requests",
                        "okhttp-dispatcher.max-requests-per-host"})
        void deveFalharQuandoPropriedadeObrigatoriaAusente(String missingProperty) {
                var properties = transportProperties();
                var configuredProperties = properties.entrySet().stream()
                                .filter(entry -> !entry.getKey().equals(missingProperty))
                                .map(entry -> entry.getKey() + "=" + entry.getValue())
                                .toArray(String[]::new);

                new ApplicationContextRunner()
                                .withUserConfiguration(BloqueioCartaoFeignConfig.class)
                                .withBean(PropertySourcesPlaceholderConfigurer.class,
                                                PropertySourcesPlaceholderConfigurer::new)
                                .withPropertyValues(configuredProperties)
                                .run(context -> {
                                        assertThat(context).hasFailed();
                                        assertThat(context.getStartupFailure())
                                                        .hasStackTraceContaining(missingProperty);
                                });
        }

        @ParameterizedTest
        @ValueSource(strings = {"okhttp-configuracao-geral.write-timeout-millis", "okhttp-dispatcher.max-requests"})
        void deveFalharQuandoConfiguracaoNaoNumerica(String invalidProperty) {
                var configuredProperties = transportProperties().entrySet().stream()
                                .map(entry -> entry.getKey() + "="
                                                + (entry.getKey().equals(invalidProperty) ? "invalid" : entry.getValue()))
                                .toArray(String[]::new);
                new ApplicationContextRunner()
                                .withUserConfiguration(BloqueioCartaoFeignConfig.class)
                                .withBean(PropertySourcesPlaceholderConfigurer.class,
                                                PropertySourcesPlaceholderConfigurer::new)
                                .withPropertyValues(configuredProperties)
                                .run(context -> {
                                        assertThat(context).hasFailed();
                                        assertThat(context.getStartupFailure())
                                                        .hasRootCauseInstanceOf(NumberFormatException.class);
                                });
        }

        private Map<String, String> transportProperties() {
                return Map.of("bloqueio-cartao.api-key", API_KEY,
                                "okhttp-configuracao-geral.write-timeout-millis", "5000",
                                "okhttp-configuracao-geral.retryOnConnectionFailure", "false",
                                "okhttp-connection-pool.max-idle-connections", "20",
                                "okhttp-connection-pool.keep-alive-duration-segundos", "60",
                                "okhttp-dispatcher.max-requests", "64",
                                "okhttp-dispatcher.max-requests-per-host", "5");
        }

        @ParameterizedTest
        @CsvSource({"400,502,DOWNSTREAM_ERROR", "500,502,DOWNSTREAM_ERROR",
                        "503,503,DOWNSTREAM_UNAVAILABLE", "504,504,DOWNSTREAM_TIMEOUT"})
        void devePreservarErrosHttpSemRetry(int downstreamStatus, int publicStatus, String code)
                        throws InterruptedException {
                int attempts = SERVER.getRequestCount();
                SERVER.enqueue(new MockResponse().setResponseCode(downstreamStatus).setBody("falha secreta"));

                var exception = assertThrows(DownstreamIntegrationException.class, () -> gateway.bloquear(bloqueio()));

                assertThat(exception.getStatus().value()).isEqualTo(downstreamStatus);
                assertThat(exception.getMessage()).isEqualTo("falha secreta");
                var response = handler.handleIntegrationException(exception);
                assertThat(response.getStatusCode().value()).isEqualTo(publicStatus);
                assertThat(response.getBody().code()).isEqualTo(code);
                assertThat(response.getBody().message()).doesNotContain("falha secreta");
                takeRequest();
                assertThat(SERVER.getRequestCount() - attempts).isEqualTo(1);
        }

        @Test
        void devePreservarErroHttpSemCorpo() throws InterruptedException {
                SERVER.enqueue(new MockResponse().setResponseCode(500));

                var exception = assertThrows(DownstreamIntegrationException.class, () -> gateway.bloquear(bloqueio()));

                assertThat(exception.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
                assertThat(exception.getMessage()).isEmpty();
                takeRequest();
        }

        @Test
        void devePreservarFallbackEFecharCorpoEmFalhaDeLeitura() throws IOException {
                var body = mock(Response.Body.class);
                when(body.asInputStream()).thenThrow(new IOException("secret"));
                var request = Request.create(Request.HttpMethod.POST, SERVER.url(PATH).toString(), Map.of(),
                                new byte[0], StandardCharsets.UTF_8, new RequestTemplate());
                var response = Response.builder().status(500).reason("error").request(request).body(body).build();
                var decoder = clientFactory.getInstance("bloqueioCartao", ErrorDecoder.class);

                var exception = decoder.decode("bloquear", response);

                assertThat(exception).isInstanceOfSatisfying(DownstreamIntegrationException.class, failure -> {
                        assertThat(failure.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
                        assertThat(failure.getMessage()).isEqualTo("Erro ao comunicar com o servi\u00e7o de bloqueio");
                });
                verify(body).close();
                assertThat(decoder.decode("bloquear", response.toBuilder().body((Response.Body) null).build()))
                                .hasMessage("");
        }

        @Test
        void deveRespeitarTimeoutSemRetry() throws InterruptedException {
                int attempts = SERVER.getRequestCount();
                SERVER.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE));
                long started = System.nanoTime();

                var exception = assertThrows(DownstreamIntegrationException.class, () -> gateway.bloquear(bloqueio()));

                assertThat(Duration.ofNanos(System.nanoTime() - started).toMillis())
                                .isBetween((long) READ_TIMEOUT / 2, (long) READ_TIMEOUT + 2000);
                assertThat(exception.getStatus()).isEqualTo(HttpStatus.GATEWAY_TIMEOUT);
                var response = handler.handleIntegrationException(exception);
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.GATEWAY_TIMEOUT);
                assertThat(response.getBody().code()).isEqualTo("DOWNSTREAM_TIMEOUT");
                takeRequest();
                assertThat(SERVER.getRequestCount() - attempts).isEqualTo(1);
        }

        @Test
        void deveTraduzirFalhaDeConexaoSemRetry() throws InterruptedException {
                int attempts = SERVER.getRequestCount();
                SERVER.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AFTER_REQUEST));

                var exception = assertThrows(DownstreamIntegrationException.class, () -> gateway.bloquear(bloqueio()));

                assertBadGateway(exception);
                takeRequest();
                assertThat(SERVER.getRequestCount() - attempts).isEqualTo(1);
        }

        @Test
        void deveTraduzirJsonInvalido() throws InterruptedException {
                SERVER.enqueue(new MockResponse().setHeader("Content-Type", "application/json").setBody("invalid json"));

                var exception = assertThrows(DownstreamIntegrationException.class, () -> gateway.bloquear(bloqueio()));

                assertBadGateway(exception);
                takeRequest();
        }

        private void assertBadGateway(DownstreamIntegrationException exception) {
                assertThat(exception.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY);
                assertThat(exception.getMessage()).isEqualTo("Servi\u00e7o de bloqueio indispon\u00edvel");
                var response = handler.handleIntegrationException(exception);
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
                assertThat(response.getBody().code()).isEqualTo("DOWNSTREAM_ERROR");
        }

        private RecordedRequest takeRequest() throws InterruptedException {
                var request = SERVER.takeRequest(2, TimeUnit.SECONDS);
                assertThat(request).isNotNull();
                return request;
        }

        private MockResponse success() {
                return new MockResponse().setHeader("Content-Type", "application/json").setBody("""
                                {"protocoloId": "%s", "status": "%s"}
                                """.formatted(PROTOCOLO_ID, STATUS_PROCESSING));
        }

        private BloqueioCartao bloqueio() {
                return new BloqueioCartao(CARTAO_ID, TipoBloqueio.DEFINITIVO, MOTIVO,
                                List.of(new Portador(PORTADOR_ID)));
    }
}
