package br.com.empresa.bff.gateway.client;

import br.com.empresa.bff.gateway.dto.BloqueioCartaoDownstreamRequest;
import br.com.empresa.bff.gateway.dto.BloqueioCartaoDownstreamResponse;

public interface BloqueioCartaoClient {

    BloqueioCartaoDownstreamResponse bloquear(BloqueioCartaoDownstreamRequest request);
}
