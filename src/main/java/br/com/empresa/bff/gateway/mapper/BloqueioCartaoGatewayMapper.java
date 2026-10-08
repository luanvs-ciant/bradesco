package br.com.empresa.bff.gateway.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import br.com.empresa.bff.domain.model.BloqueioCartao;
import br.com.empresa.bff.domain.model.Portador;
import br.com.empresa.bff.domain.model.ResultadoBloqueioCartao;
import br.com.empresa.bff.gateway.dto.BloqueioCartaoDownstreamRequest;
import br.com.empresa.bff.gateway.dto.BloqueioCartaoDownstreamRequest.PortadorDownstreamRequest;
import br.com.empresa.bff.gateway.dto.BloqueioCartaoDownstreamResponse;

@Component
public class BloqueioCartaoGatewayMapper {

    public BloqueioCartaoDownstreamRequest toDownstreamRequest(BloqueioCartao bloqueio) {
        return new BloqueioCartaoDownstreamRequest(
                bloqueio.cartaoId(),
                bloqueio.tipoBloqueio().name(),
                bloqueio.motivo(),
                toPortadores(bloqueio.portadores()));
    }

    public ResultadoBloqueioCartao toResult(BloqueioCartaoDownstreamResponse response) {
        return new ResultadoBloqueioCartao(response.protocoloId(), response.status());
    }

    private List<PortadorDownstreamRequest> toPortadores(List<Portador> portadores) {
        return portadores.stream()
                .map(portador -> new PortadorDownstreamRequest(portador.id()))
                .toList();
    }
}
