package br.com.empresa.bff.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;

import br.com.empresa.bff.config.feign.BloqueioCartaoFeignConfig;
import br.com.empresa.bff.exception.DownstreamIntegrationException;
import br.com.empresa.bff.observability.CorrelationId;
import feign.Request;
import feign.RequestTemplate;
import feign.Response;
import feign.Retryer;

class BloqueioCartaoFeignConfigTest {

    private static final String API_KEY = "test-api-key";

    private final BloqueioCartaoFeignConfig config =
            new BloqueioCartaoFeignConfig(API_KEY, 321, false, 1, 60, 12, 2);

    @AfterEach
    void clearContext() {
        MDC.clear();
    }

    @Test
    void shouldBuildOkHttpTransportFromProperties() {
        var transport = config.bloqueioCartaoOkHttpClient();

        assertThat(transport.writeTimeoutMillis()).isEqualTo(321);
        assertThat(transport.retryOnConnectionFailure()).isFalse();
        assertThat(config.bloqueioCartaoFeignClient(transport)).isInstanceOf(feign.okhttp.OkHttpClient.class);
        assertThat(config.bloqueioCartaoRetryer()).isSameAs(Retryer.NEVER_RETRY);
    }

    @Test
    void shouldAddApiKeyAndCorrelationIdHeaders() {
        MDC.put(CorrelationId.MDC_KEY, "corr-123");
        var template = new RequestTemplate();

        config.bloqueioCartaoRequestInterceptor().apply(template);

        assertThat(template.headers().get("X-Api-Key")).containsExactly(API_KEY);
        assertThat(template.headers().get(CorrelationId.HEADER)).containsExactly("corr-123");
    }

    @Test
    void shouldOmitCorrelationIdWhenAbsent() {
        var template = new RequestTemplate();

        config.bloqueioCartaoRequestInterceptor().apply(template);

        assertThat(template.headers()).containsKey("X-Api-Key").doesNotContainKey(CorrelationId.HEADER);
    }

    @Test
    void shouldDecodeErrorPreservingStatusAndBody() {
        var response = response(500).body("falha secreta", StandardCharsets.UTF_8).build();

        var exception = config.bloqueioCartaoErrorDecoder().decode("bloquear", response);

        assertThat(exception).isInstanceOfSatisfying(DownstreamIntegrationException.class, failure -> {
            assertThat(failure.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(failure.getMessage()).isEqualTo("falha secreta");
        });
    }

    @Test
    void shouldDecodeErrorWithoutBody() {
        var exception = config.bloqueioCartaoErrorDecoder().decode("bloquear", response(503).build());

        assertThat(exception).hasMessage("");
    }

    @Test
    void shouldUseFallbackMessageAndCloseBodyWhenReadFails() throws IOException {
        var body = mock(Response.Body.class);
        when(body.asInputStream()).thenThrow(new IOException("secret"));

        var exception = config.bloqueioCartaoErrorDecoder().decode("bloquear", response(500).body(body).build());

        assertThat(exception).hasMessage("Erro ao comunicar com o serviço de bloqueio");
        verify(body).close();
    }

    private static Response.Builder response(int status) {
        var request = Request.create(Request.HttpMethod.POST, "/", Map.of(), null, StandardCharsets.UTF_8, null);
        return Response.builder().status(status).reason("error").request(request);
    }
}
