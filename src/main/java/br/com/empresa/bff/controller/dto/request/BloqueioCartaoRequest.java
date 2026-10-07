package br.com.empresa.bff.controller.dto.request;

import java.util.List;

import br.com.empresa.bff.controller.dto.TipoBloqueioDto;

public record BloqueioCartaoRequest(
        String cartaoId,
        TipoBloqueioDto tipoBloqueio,
        String motivo,
        List<PortadorRequest> portadores
) {
}
