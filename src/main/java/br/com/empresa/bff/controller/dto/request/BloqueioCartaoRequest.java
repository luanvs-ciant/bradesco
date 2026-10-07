package br.com.empresa.bff.controller.dto.request;

import br.com.empresa.bff.controller.dto.TipoBloqueioDto;

import java.util.List;

public record BloqueioCartaoRequest(
        String cartaoId,
        TipoBloqueioDto tipoBloqueio,
        String motivo,
        List<PortadorRequest> portadores
) {
}