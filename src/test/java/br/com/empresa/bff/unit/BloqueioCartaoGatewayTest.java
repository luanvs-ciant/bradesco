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
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import br.com.empresa.bff.domain.model.BloqueioCartao;
import br.com.empresa.bff.domain.model.Portador;
import br.com.empresa.bff.domain.model.ResultadoBloqueioCartao;
import br.com.empresa.bff.domain.model.TipoBloqueio;
import br.com.empresa.bff.exception.DownstreamIntegrationException;
import br.com.empresa.bff.gateway.BloqueioCartaoGatewayImpl;
import br.com.empresa.bff.gateway.client.BloqueioCartaoClient;
import br.com.empresa.bff.gateway.dto.BloqueioCartaoDownstreamResponse;
import br.com.empresa.bff.gateway.mapper.BloqueioCartaoGatewayMapper;
import feign.Request;
import feign.RetryableException;

class BloqueioCartaoGatewayTest {

    private static final BloqueioCartao BLOQUEIO = new BloqueioCartao(
            "123456", TipoBloqueio.TEMPORARIO, null, List.of(new Portador("987654")));

    private final BloqueioCartaoGatewayMapper mapper = new BloqueioCartaoGatewayMapper();
    private BloqueioCartaoClient client;
    private BloqueioCartaoGatewayImpl gateway;

    @BeforeEach
    void setUp() {
        client = mock(BloqueioCartaoClient.class);
        gateway = new BloqueioCartaoGatewayImpl(client, mapper);
    }

    @Test
    void mapsDomainToDownstreamRequestAndMapsResponse() {
        var request = mapper.toDownstreamRequest(BLOQUEIO);
        when(client.bloquear(request)).thenReturn(new BloqueioCartaoDownstreamResponse("abc123", "PROCESSING"));

        var result = gateway.bloquear(BLOQUEIO);

        assertEquals(new ResultadoBloqueioCartao("abc123", "PROCESSING"), result);
        verify(client).bloquear(request);
    }

    @Test
    void propagatesDownstreamHttpErrors() {
        var failure = new DownstreamIntegrationException(HttpStatus.INTERNAL_SERVER_ERROR, "servico indisponivel");
        when(client.bloquear(mapper.toDownstreamRequest(BLOQUEIO))).thenThrow(failure);

        var exception = assertThrows(DownstreamIntegrationException.class, () -> gateway.bloquear(BLOQUEIO));

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
        when(client.bloquear(mapper.toDownstreamRequest(BLOQUEIO))).thenThrow(failure);

        var exception = assertThrows(DownstreamIntegrationException.class, () -> gateway.bloquear(BLOQUEIO));

        assertEquals(expectedStatus, exception.getStatus());
        return exception;
    }

    private static RetryableException ioFailure(IOException cause) {
        var request = Request.create(Request.HttpMethod.POST, "/", Map.of(), null, StandardCharsets.UTF_8, null);
        return new RetryableException(-1, cause.getMessage(), Request.HttpMethod.POST, cause, (Long) null, request);
    }
}
