package br.com.empresa.bff.controller.dto.response;

public record BloqueioCartaoResponse(
        String protocoloId,
        String status
) {
}