package br.com.empresa.bff.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import org.junit.jupiter.api.Test;

import br.com.empresa.bff.controller.dto.request.BloqueioCartaoRequest;
import br.com.empresa.bff.domain.model.BloqueioCartao;
import br.com.empresa.bff.domain.model.Portador;
import br.com.empresa.bff.domain.model.TipoBloqueio;
import br.com.empresa.bff.mapper.BloqueioCartaoMapperImpl;
import br.com.empresa.bff.support.JsonFixtures;

class BloqueioCartaoMapperTest {

    private final BloqueioCartaoMapperImpl mapper = new BloqueioCartaoMapperImpl();

    @Test
    void returnsNullWhenRequestIsNull() {
        assertNull(mapper.toDomain(null));
    }

    @Test
    void mapsNullTipoBloqueioAndPortadoresToNull() {
        var request = new BloqueioCartaoRequest("123456", null, null, null);

        var result = mapper.toDomain(request);

        assertNull(result.tipoBloqueio());
        assertNull(result.portadores());
    }

    @Test
    void mapsRequestToDomain() {
        var request = JsonFixtures.read("bloqueio-cartao/request-with-reason.json", BloqueioCartaoRequest.class);

        var result = mapper.toDomain(request);

        assertEquals(new BloqueioCartao(
                "123456",
                TipoBloqueio.DEFINITIVO,
                "SOLICITACAO_CLIENTE",
                List.of(new Portador("987654"), new Portador("456789"))
        ), result);
    }

    @Test
    void mapsRequestWithOptionalMotivoOmitted() {
        var request = JsonFixtures.read("bloqueio-cartao/request-without-reason.json", BloqueioCartaoRequest.class);

        var result = mapper.toDomain(request);

        assertEquals("123456", result.cartaoId());
        assertEquals(TipoBloqueio.TEMPORARIO, result.tipoBloqueio());
        assertNull(result.motivo());
        assertEquals(List.of(new Portador("987654")), result.portadores());
    }
}
