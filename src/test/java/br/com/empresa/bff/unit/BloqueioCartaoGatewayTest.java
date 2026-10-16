package br.com.empresa.bff.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import br.com.empresa.bff.gateway.dto.BloqueioCartaoDownstreamRequest;
import br.com.empresa.bff.exception.DownstreamIntegrationException;
import br.com.empresa.bff.gateway.BloqueioCartaoGatewayImpl;
import br.com.empresa.bff.gateway.client.BloqueioCartaoClient;
import br.com.empresa.bff.gateway.dto.BloqueioCartaoDownstreamResponse;
import br.com.empresa.bff.support.JsonFixtures;
import feign.Request;
import feign.RetryableException;
import feign.codec.DecodeException;
import feign.codec.EncodeException;

class BloqueioCartaoGatewayTest {

    private BloqueioCartaoDownstreamRequest bloqueio;

    private BloqueioCartaoClient client;
    private BloqueioCartaoGatewayImpl gateway;

    @BeforeEach
    void setUp() {
        bloqueio = JsonFixtures.read("bloqueio-cartao/request-without-reason.json", BloqueioCartaoDownstreamRequest.class);
        client = mock(BloqueioCartaoClient.class);
        gateway = new BloqueioCartaoGatewayImpl(client);
    }

    @Test
    void sendsDomainToClientAndReturnsDownstreamResponse() {
        var downstreamResponse = JsonFixtures.read(
                "bloqueio-cartao/response-processing.json", BloqueioCartaoDownstreamResponse.class);
        when(client.bloquear(bloqueio)).thenReturn(downstreamResponse);

        var result = gateway.bloquear(bloqueio);

        assertSame(downstreamResponse, result);
        verify(client).bloquear(bloqueio);
    }

    @Test
    void propagatesDownstreamHttpErrors() {
        var failure = new DownstreamIntegrationException(HttpStatus.INTERNAL_SERVER_ERROR, "servico indisponivel");
        when(client.bloquear(bloqueio)).thenThrow(failure);

        var exception = assertThrows(DownstreamIntegrationException.class, () -> gateway.bloquear(bloqueio));

        assertSame(failure, exception);
    }

    @Test
    void translatesTimeoutToGatewayTimeout() {
        assertTranslated(ioFailure(new SocketTimeoutException("read timed out")), HttpStatus.GATEWAY_TIMEOUT);
    }

    @Test
    void translatesConnectionFailureToServiceUnavailable() {
        assertTranslated(ioFailure(new ConnectException("connection refused")), HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    void translatesFeignFailureToBadGatewayKeepingCause() {
        var failure = new DecodeException(200, "invalid payload", request(), new IllegalStateException("boom"));

        var exception = assertTranslated(failure, HttpStatus.BAD_GATEWAY);

        assertSame(failure, exception.getCause());
    }

    @Test
    void doesNotTranslateEncodeFailureBecauseTheRequestWasNeverSent() {
        var failure = new EncodeException("cannot serialize request", new IllegalStateException("boom"));
        when(client.bloquear(bloqueio)).thenThrow(failure);

        var thrown = assertThrows(EncodeException.class, () -> gateway.bloquear(bloqueio));

        assertSame(failure, thrown);
    }

    @Test
    void doesNotTranslateUnexpectedFailure() {
        var failure = new IllegalStateException("bug");
        when(client.bloquear(bloqueio)).thenThrow(failure);

        var thrown = assertThrows(IllegalStateException.class, () -> gateway.bloquear(bloqueio));

        assertSame(failure, thrown);
    }

    private DownstreamIntegrationException assertTranslated(RuntimeException failure, HttpStatus expectedStatus) {
        when(client.bloquear(bloqueio)).thenThrow(failure);

        var exception = assertThrows(DownstreamIntegrationException.class, () -> gateway.bloquear(bloqueio));

        assertEquals(expectedStatus, exception.getStatus());
        return exception;
    }

    private static Request request() {
        return Request.create(Request.HttpMethod.POST, "/", Map.of(), null, StandardCharsets.UTF_8, null);
    }

    private static RetryableException ioFailure(IOException cause) {
        return new RetryableException(-1, cause.getMessage(), Request.HttpMethod.POST, cause, (Long) null, request());
    }
}
