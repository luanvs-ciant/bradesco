package br.com.empresa.bff.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import br.com.empresa.bff.controller.dto.response.BloqueioCartaoResponse;
import br.com.empresa.bff.gateway.dto.BloqueioCartaoDownstreamResponse;
import br.com.empresa.bff.mapper.BloqueioCartaoResponseMapperImpl;
import br.com.empresa.bff.support.JsonFixtures;

class BloqueioCartaoResponseMapperTest {

    private final BloqueioCartaoResponseMapperImpl mapper = new BloqueioCartaoResponseMapperImpl();

    @Test
    void mapsProtocoloIdAndStatus() {
        var downstreamResponse = JsonFixtures.read(
                "bloqueio-cartao/response-processing.json", BloqueioCartaoDownstreamResponse.class);

        var response = mapper.toResponse(downstreamResponse);

        assertEquals(new BloqueioCartaoResponse("protocol-123", "PROCESSING"), response);
    }

    @Test
    void returnsNullWhenResponseIsNull() {
        assertNull(mapper.toResponse(null));
    }
}
