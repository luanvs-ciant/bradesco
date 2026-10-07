package br.com.empresa.bff.application.service;

import br.com.empresa.bff.controller.dto.request.BloqueioCartaoRequest;
import br.com.empresa.bff.controller.dto.response.BloqueioCartaoResponse;
import br.com.empresa.bff.mapper.BloqueioCartaoMapper;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class BloqueioCartaoApplicationService {

    private final BloqueioCartaoMapper mapper;

    public BloqueioCartaoApplicationService(BloqueioCartaoMapper mapper) {
        this.mapper = mapper;
    }

    public BloqueioCartaoResponse bloquear(BloqueioCartaoRequest request) {
        mapper.toDomain(request);
        return new BloqueioCartaoResponse(UUID.randomUUID().toString(), "PROCESSING");
    }
}