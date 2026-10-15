package br.com.empresa.bff.application.service;

import org.springframework.stereotype.Service;

import br.com.empresa.bff.controller.dto.request.BloqueioCartaoRequest;
import br.com.empresa.bff.controller.dto.response.BloqueioCartaoResponse;
import br.com.empresa.bff.domain.gateway.BloqueioCartaoGateway;
import br.com.empresa.bff.mapper.BloqueioCartaoMapper;
import br.com.empresa.bff.mapper.BloqueioCartaoResponseMapper;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BloqueioCartaoApplicationService {

    private final BloqueioCartaoMapper mapper;
    private final BloqueioCartaoGateway gateway;
    private final BloqueioCartaoResponseMapper responseMapper;

    public BloqueioCartaoResponse bloquear(BloqueioCartaoRequest request) {
        var domain = mapper.toDomain(request);
        var response = gateway.bloquear(domain);
        return responseMapper.toResponse(response);
    }
}
