package br.com.empresa.bff.application.service;

import org.springframework.stereotype.Service;

import br.com.empresa.bff.controller.dto.request.BloqueioCartaoRequest;
import br.com.empresa.bff.controller.dto.response.BloqueioCartaoResponse;
import br.com.empresa.bff.domain.gateway.BloqueioCartaoGateway;
import br.com.empresa.bff.domain.model.BloqueioCartao;
import br.com.empresa.bff.domain.model.ResultadoBloqueioCartao;
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
        BloqueioCartao bloqueio = mapper.toDomain(request);
        ResultadoBloqueioCartao resultado = gateway.bloquear(bloqueio);
        return new BloqueioCartaoResponse(resultado.protocoloId(), resultado.status());
    }
}
