package br.com.empresa.bff.gateway.dto;

public record BloqueioCartaoDownstreamResponse(
        String protocoloId,
        String status
) {
}
