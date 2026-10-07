package br.com.empresa.bff.mapper;

import org.springframework.stereotype.Component;

import br.com.empresa.bff.controller.dto.request.BloqueioCartaoRequest;
import br.com.empresa.bff.domain.model.BloqueioCartao;
import br.com.empresa.bff.domain.model.Portador;
import br.com.empresa.bff.domain.model.TipoBloqueio;

@Component
public class BloqueioCartaoMapper {

    public BloqueioCartao toDomain(BloqueioCartaoRequest request) {
        return new BloqueioCartao(
                request.cartaoId(),
                TipoBloqueio.valueOf(request.tipoBloqueio().name()),
                request.motivo(),
                request.portadores().stream()
                        .map(portador -> new Portador(portador.portadorId()))
                        .toList()
        );
    }
}
