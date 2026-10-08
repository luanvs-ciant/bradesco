package br.com.empresa.bff.domain.gateway;

import br.com.empresa.bff.domain.model.BloqueioCartao;
import br.com.empresa.bff.domain.model.ResultadoBloqueioCartao;

public interface BloqueioCartaoGateway {

    ResultadoBloqueioCartao bloquear(BloqueioCartao bloqueio);
}