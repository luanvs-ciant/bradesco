package br.com.empresa.bff.application.service;

import org.springframework.stereotype.Service;

import br.com.empresa.bff.controller.dto.request.BloqueioCartaoRequest;
import br.com.empresa.bff.controller.dto.response.BloqueioCartaoResponse;
import br.com.empresa.bff.gateway.BloqueioCartaoGateway;
import br.com.empresa.bff.mapper.BloqueioCartaoMapper;

@Service
public class BloqueioCartaoApplicationService {

    private final BloqueioCartaoMapper mapper;
    private final BloqueioCartaoGateway gateway;

    public BloqueioCartaoApplicationService(
            BloqueioCartaoMapper mapper,
            BloqueioCartaoGateway gateway) {
        this.mapper = mapper;
        this.gateway = gateway;
    }

    public BloqueioCartaoResponse bloquear(BloqueioCartaoRequest request) {
        var domain = mapper.toDomain(request);
        var result = gateway.bloquear(domain);
        return new BloqueioCartaoResponse(result.protocoloId(), result.status());
    }
}