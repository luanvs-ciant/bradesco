package br.com.empresa.bff.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import br.com.empresa.bff.controller.dto.response.BloqueioCartaoResponse;
import br.com.empresa.bff.domain.model.ResultadoBloqueioCartao;
import br.com.empresa.bff.mapper.BloqueioCartaoResponseMapperImpl;

class BloqueioCartaoResponseMapperTest {

    private final BloqueioCartaoResponseMapperImpl mapper = new BloqueioCartaoResponseMapperImpl();

    @Test
    void mapsProtocoloIdAndStatus() {
        var resultado = new ResultadoBloqueioCartao("abc123", "PROCESSING");

        var response = mapper.toResponse(resultado);

        assertEquals(new BloqueioCartaoResponse("abc123", "PROCESSING"), response);
    }

    @Test
    void returnsNullWhenResultadoIsNull() {
        assertNull(mapper.toResponse(null));
    }
}
