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

import br.com.empresa.bff.domain.model.BloqueioCartao;
import br.com.empresa.bff.exception.DownstreamIntegrationException;
import br.com.empresa.bff.gateway.BloqueioCartaoGatewayImpl;
import br.com.empresa.bff.gateway.client.BloqueioCartaoClient;
import br.com.empresa.bff.gateway.dto.BloqueioCartaoDownstreamResponse;
import br.com.empresa.bff.support.JsonFixtures;
import feign.Request;
import feign.RetryableException;

class BloqueioCartaoGatewayTest {

    private BloqueioCartao bloqueio;

    private BloqueioCartaoClient client;
    private BloqueioCartaoGatewayImpl gateway;

    @BeforeEach
    void setUp() {
        bloqueio = JsonFixtures.read("bloqueio-cartao/request-without-reason.json", BloqueioCartao.class);
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
    void translatesUnexpectedFailureToBadGatewayKeepingCause() {
        var failure = new IllegalStateException("invalid payload");

        var exception = assertTranslated(failure, HttpStatus.BAD_GATEWAY);

        assertSame(failure, exception.getCause());
    }

    private DownstreamIntegrationException assertTranslated(RuntimeException failure, HttpStatus expectedStatus) {
        when(client.bloquear(bloqueio)).thenThrow(failure);

        var exception = assertThrows(DownstreamIntegrationException.class, () -> gateway.bloquear(bloqueio));

        assertEquals(expectedStatus, exception.getStatus());
        return exception;
    }

    private static RetryableException ioFailure(IOException cause) {
        var request = Request.create(Request.HttpMethod.POST, "/", Map.of(), null, StandardCharsets.UTF_8, null);
        return new RetryableException(-1, cause.getMessage(), Request.HttpMethod.POST, cause, (Long) null, request);
    }
}
