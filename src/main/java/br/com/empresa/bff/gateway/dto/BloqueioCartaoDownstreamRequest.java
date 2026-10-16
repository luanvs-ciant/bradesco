package br.com.empresa.bff.gateway.dto;

import java.util.List;

import br.com.empresa.bff.model.TipoBloqueio;

public record BloqueioCartaoDownstreamRequest(
        String cartaoId,
        TipoBloqueio tipoBloqueio,
        String motivo,
        List<PortadorDownstreamRequest> portadores
) {

    public record PortadorDownstreamRequest(String portadorId) {
    }
}
