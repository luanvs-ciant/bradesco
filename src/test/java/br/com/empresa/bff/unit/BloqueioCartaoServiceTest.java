package br.com.empresa.bff.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.empresa.bff.controller.dto.request.BloqueioCartaoRequest;
import br.com.empresa.bff.controller.dto.response.BloqueioCartaoResponse;
import br.com.empresa.bff.gateway.BloqueioCartaoGateway;
import br.com.empresa.bff.gateway.dto.BloqueioCartaoDownstreamRequest;
import br.com.empresa.bff.gateway.dto.BloqueioCartaoDownstreamResponse;
import br.com.empresa.bff.service.BloqueioCartaoService;
import br.com.empresa.bff.service.mapper.BloqueioCartaoMapper;
import br.com.empresa.bff.support.JsonFixtures;

@ExtendWith(MockitoExtension.class)
class BloqueioCartaoServiceTest {

    @Mock
    private BloqueioCartaoMapper mapper;

    @Mock
    private BloqueioCartaoGateway gateway;

    private BloqueioCartaoService service;

    @BeforeEach
    void setUp() {
        service = new BloqueioCartaoService(mapper, gateway);
    }

    @Test
    void shouldSubmitBlockAndReturnOperationResult() {
        var request = JsonFixtures.read("bloqueio-cartao/request-without-reason.json", BloqueioCartaoRequest.class);
        var downstreamRequest = JsonFixtures.read(
                "bloqueio-cartao/request-without-reason.json", BloqueioCartaoDownstreamRequest.class);
        var downstreamResponse = JsonFixtures.read(
                "bloqueio-cartao/response-processing.json", BloqueioCartaoDownstreamResponse.class);
        var esperado = JsonFixtures.read("bloqueio-cartao/response-processing.json", BloqueioCartaoResponse.class);
        when(mapper.toDownstreamRequest(request)).thenReturn(downstreamRequest);
        when(gateway.bloquear(downstreamRequest)).thenReturn(downstreamResponse);
        when(mapper.toResponse(downstreamResponse)).thenReturn(esperado);

        BloqueioCartaoResponse response = service.bloquear(request);

        assertEquals(esperado, response);
        verify(gateway).bloquear(downstreamRequest);
    }
}
