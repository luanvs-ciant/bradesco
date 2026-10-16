package br.com.empresa.bff.gateway;

import br.com.empresa.bff.gateway.dto.BloqueioCartaoDownstreamRequest;
import br.com.empresa.bff.gateway.dto.BloqueioCartaoDownstreamResponse;

public interface BloqueioCartaoGateway {

    BloqueioCartaoDownstreamResponse bloquear(BloqueioCartaoDownstreamRequest request);
}
