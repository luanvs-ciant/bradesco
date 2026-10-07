package br.com.empresa.bff.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import org.junit.jupiter.api.Test;

import br.com.empresa.bff.controller.dto.TipoBloqueioDto;
import br.com.empresa.bff.controller.dto.request.BloqueioCartaoRequest;
import br.com.empresa.bff.controller.dto.request.PortadorRequest;
import br.com.empresa.bff.domain.model.BloqueioCartao;
import br.com.empresa.bff.domain.model.Portador;
import br.com.empresa.bff.domain.model.TipoBloqueio;
import br.com.empresa.bff.mapper.BloqueioCartaoMapper;

class BloqueioCartaoMapperTest {

    private final BloqueioCartaoMapper mapper = new BloqueioCartaoMapper();

    @Test
    void mapsRequestToDomain() {
        var request = new BloqueioCartaoRequest(
                "123456",
                TipoBloqueioDto.DEFINITIVO,
                "SOLICITACAO_CLIENTE",
                List.of(new PortadorRequest("987654"), new PortadorRequest("456789"))
        );

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
        var request = new BloqueioCartaoRequest(
                "123456",
                TipoBloqueioDto.TEMPORARIO,
                null,
                List.of(new PortadorRequest("987654"))
        );

        var result = mapper.toDomain(request);

        assertEquals("123456", result.cartaoId());
        assertEquals(TipoBloqueio.TEMPORARIO, result.tipoBloqueio());
        assertNull(result.motivo());
        assertEquals(List.of(new Portador("987654")), result.portadores());
    }
}
