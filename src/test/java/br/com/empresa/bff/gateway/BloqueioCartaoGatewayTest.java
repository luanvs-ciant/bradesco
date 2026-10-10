package br.com.empresa.bff.gateway;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import br.com.empresa.bff.gateway.client.BloqueioCartaoClient;
import br.com.empresa.bff.gateway.dto.BloqueioCartaoDownstreamResponse;
import br.com.empresa.bff.gateway.mapper.BloqueioCartaoGatewayMapper;
import feign.Request;
import feign.RetryableException;

class BloqueioCartaoGatewayTest {

    private BloqueioCartaoClient client;
    private final BloqueioCartaoGatewayMapper mapper = new BloqueioCartaoGatewayMapper();
        private BloqueioCartaoGatewayImpl gateway;

    @BeforeEach
    void setUp() {
        client = mock(BloqueioCartaoClient.class);
                gateway = new BloqueioCartaoGatewayImpl(client, mapper);
    }

    @Test
    void mapsDomainToDownstreamRequestAndMapsResponse() {
        var domain = new BloqueioCartao(
                "123456",
                TipoBloqueio.DEFINITIVO,
                "SOLICITACAO_CLIENTE",
                List.of(new Portador("987654"), new Portador("456789")));
        var request = mapper.toDownstreamRequest(domain);
        when(client.bloquear(request)).thenReturn(new BloqueioCartaoDownstreamResponse("abc123", "PROCESSING"));

        var result = gateway.bloquear(domain);

        assertEquals(new ResultadoBloqueioCartao("abc123", "PROCESSING"), result);
        verify(client).bloquear(request);
    }

    @Test
    void propagatesDownstreamIntegrationFailures() {
        var domain = new BloqueioCartao(
                "123456",
                TipoBloqueio.TEMPORARIO,
                null,
                List.of(new Portador("987654")));
        var failure = new DownstreamIntegrationException(HttpStatus.INTERNAL_SERVER_ERROR, "servico indisponivel");
        when(client.bloquear(mapper.toDownstreamRequest(domain))).thenThrow(failure);

        var exception = assertThrows(DownstreamIntegrationException.class,
                () -> gateway.bloquear(domain));

        assertSame(failure, exception);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatus());
        assertEquals("servico indisponivel", exception.getMessage());
    }

    @Test
    void translatesExecutionFailuresWithoutChangingPublicStatus() {
        var domain = new BloqueioCartao("123456", TipoBloqueio.TEMPORARIO, null,
                List.of(new Portador("987654")));
        when(client.bloquear(mapper.toDownstreamRequest(domain)))
                .thenThrow(new IllegalStateException("internal details"));

        var exception = assertThrows(DownstreamIntegrationException.class,
                () -> gateway.bloquear(domain));

        assertEquals(HttpStatus.BAD_GATEWAY, exception.getStatus());
        assertEquals("Servi\u00e7o de bloqueio indispon\u00edvel", exception.getMessage());
    }

    @Test
    void translatesTimeoutFailuresToGatewayTimeout() {
        var domain = new BloqueioCartao("123456", TipoBloqueio.TEMPORARIO, null,
                List.of(new Portador("987654")));
        when(client.bloquear(mapper.toDownstreamRequest(domain)))
                .thenThrow(new RetryableException(-1, "read timed out", Request.HttpMethod.POST,
                        new SocketTimeoutException("read timed out"), (Long) null,
                        Request.create(Request.HttpMethod.POST, "/", Map.of(), null, StandardCharsets.UTF_8, null)));

        var exception = assertThrows(DownstreamIntegrationException.class,
                () -> gateway.bloquear(domain));

        assertEquals(HttpStatus.GATEWAY_TIMEOUT, exception.getStatus());
    }
}
