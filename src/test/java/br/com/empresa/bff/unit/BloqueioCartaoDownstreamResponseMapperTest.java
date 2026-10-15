package br.com.empresa.bff.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import br.com.empresa.bff.domain.model.ResultadoBloqueioCartao;
import br.com.empresa.bff.gateway.dto.BloqueioCartaoDownstreamResponse;
import br.com.empresa.bff.gateway.mapper.BloqueioCartaoDownstreamResponseMapperImpl;

class BloqueioCartaoDownstreamResponseMapperTest {

    private final BloqueioCartaoDownstreamResponseMapperImpl mapper = new BloqueioCartaoDownstreamResponseMapperImpl();

    @Test
    void mapsProtocoloIdAndStatus() {
        var response = new BloqueioCartaoDownstreamResponse("abc123", "PROCESSING");

        var result = mapper.toResult(response);

        assertEquals(new ResultadoBloqueioCartao("abc123", "PROCESSING"), result);
    }

    @Test
    void returnsNullWhenResponseIsNull() {
        assertNull(mapper.toResult(null));
    }
}
