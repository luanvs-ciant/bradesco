package br.com.empresa.bff.service;

import org.springframework.stereotype.Service;

import br.com.empresa.bff.controller.dto.request.BloqueioCartaoRequest;
import br.com.empresa.bff.controller.dto.response.BloqueioCartaoResponse;
import br.com.empresa.bff.gateway.BloqueioCartaoGateway;
import br.com.empresa.bff.service.mapper.BloqueioCartaoMapper;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BloqueioCartaoService {

    private final BloqueioCartaoMapper mapper;
    private final BloqueioCartaoGateway gateway;

    public BloqueioCartaoResponse bloquear(BloqueioCartaoRequest request) {
        var downstreamResponse = gateway.bloquear(mapper.toDownstreamRequest(request));
        return mapper.toResponse(downstreamResponse);
    }
}
