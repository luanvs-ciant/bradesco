package br.com.empresa.bff.gateway.dto;

import java.util.List;

public record BloqueioCartaoDownstreamRequest(
        String cartaoId,
        String tipoBloqueio,
        String motivo,
        List<PortadorDownstreamRequest> portadores
) {

    public record PortadorDownstreamRequest(String portadorId) {
    }
}
