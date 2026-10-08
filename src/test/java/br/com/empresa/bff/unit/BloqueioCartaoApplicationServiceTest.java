package br.com.empresa.bff.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.empresa.bff.application.service.BloqueioCartaoApplicationService;
import br.com.empresa.bff.controller.dto.TipoBloqueioDto;
import br.com.empresa.bff.controller.dto.request.BloqueioCartaoRequest;
import br.com.empresa.bff.controller.dto.request.PortadorRequest;
import br.com.empresa.bff.controller.dto.response.BloqueioCartaoResponse;
import br.com.empresa.bff.domain.gateway.BloqueioCartaoGateway;
import br.com.empresa.bff.domain.model.BloqueioCartao;
import br.com.empresa.bff.domain.model.Portador;
import br.com.empresa.bff.domain.model.ResultadoBloqueioCartao;
import br.com.empresa.bff.mapper.BloqueioCartaoMapper;

@ExtendWith(MockitoExtension.class)
class BloqueioCartaoApplicationServiceTest {

    @Mock
    private BloqueioCartaoMapper mapper;

    @Mock
    private BloqueioCartaoGateway gateway;

    private BloqueioCartaoApplicationService applicationService;

    @BeforeEach
    void setUp() {
        applicationService = new BloqueioCartaoApplicationService(mapper, gateway);
    }

    @Test
    void shouldSubmitBlockAndReturnOperationResult() {
        BloqueioCartaoRequest request = request();
        BloqueioCartao bloqueio = new BloqueioCartao(
                "card-123", br.com.empresa.bff.domain.model.TipoBloqueio.DEFINITIVO,
            null, List.of(new Portador("holder-123")));
        when(mapper.toDomain(request)).thenReturn(bloqueio);
        when(gateway.bloquear(bloqueio))
                .thenReturn(new ResultadoBloqueioCartao("protocol-123", "PROCESSING"));

        BloqueioCartaoResponse response = applicationService.bloquear(request);

        assertEquals(new BloqueioCartaoResponse("protocol-123", "PROCESSING"), response);
        verify(mapper).toDomain(request);
        verify(gateway).bloquear(bloqueio);
    }

    private BloqueioCartaoRequest request() {
        return new BloqueioCartaoRequest(
                "card-123", TipoBloqueioDto.DEFINITIVO, null,
                List.of(new PortadorRequest("holder-123")));
    }
}
