package br.com.empresa.bff.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import org.junit.jupiter.api.Test;

import br.com.empresa.bff.controller.dto.request.BloqueioCartaoRequest;
import br.com.empresa.bff.controller.dto.response.BloqueioCartaoResponse;
import br.com.empresa.bff.gateway.dto.BloqueioCartaoDownstreamRequest;
import br.com.empresa.bff.gateway.dto.BloqueioCartaoDownstreamRequest.PortadorDownstreamRequest;
import br.com.empresa.bff.gateway.dto.BloqueioCartaoDownstreamResponse;
import br.com.empresa.bff.model.TipoBloqueio;
import br.com.empresa.bff.service.mapper.BloqueioCartaoMapperImpl;
import br.com.empresa.bff.support.JsonFixtures;

class BloqueioCartaoMapperTest {

    private final BloqueioCartaoMapperImpl mapper = new BloqueioCartaoMapperImpl();

    @Test
    void returnsNullWhenRequestIsNull() {
        assertNull(mapper.toDownstreamRequest(null));
    }

    @Test
    void mapsNullPortadoresToNull() {
        var request = new BloqueioCartaoRequest("123456", null, null, null);

        var result = mapper.toDownstreamRequest(request);

        assertNull(result.tipoBloqueio());
        assertNull(result.portadores());
    }

    @Test
    void mapsRequestToDownstreamRequest() {
        var request = JsonFixtures.read("bloqueio-cartao/request-with-reason.json", BloqueioCartaoRequest.class);

        var result = mapper.toDownstreamRequest(request);

        assertEquals(new BloqueioCartaoDownstreamRequest(
                "123456",
                TipoBloqueio.DEFINITIVO,
                "SOLICITACAO_CLIENTE",
                List.of(new PortadorDownstreamRequest("987654"), new PortadorDownstreamRequest("456789"))
        ), result);
    }

    @Test
    void mapsRequestWithOptionalMotivoOmitted() {
        var request = JsonFixtures.read("bloqueio-cartao/request-without-reason.json", BloqueioCartaoRequest.class);

        var result = mapper.toDownstreamRequest(request);

        assertEquals("123456", result.cartaoId());
        assertEquals(TipoBloqueio.TEMPORARIO, result.tipoBloqueio());
        assertNull(result.motivo());
        assertEquals(List.of(new PortadorDownstreamRequest("987654")), result.portadores());
    }

    @Test
    void mapsDownstreamResponseToResponse() {
        var downstreamResponse = JsonFixtures.read(
                "bloqueio-cartao/response-processing.json", BloqueioCartaoDownstreamResponse.class);

        assertEquals(new BloqueioCartaoResponse("protocol-123", "PROCESSING"),
                mapper.toResponse(downstreamResponse));
    }

    @Test
    void returnsNullWhenResponseIsNull() {
        assertNull(mapper.toResponse(null));
    }
}
