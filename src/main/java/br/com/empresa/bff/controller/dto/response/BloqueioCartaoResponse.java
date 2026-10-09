package br.com.empresa.bff.controller.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record BloqueioCartaoResponse(
        @Schema(description = "Protocolo da operação aceita", example = "8f6d2c10") String protocoloId,
        @Schema(description = "Estado inicial do processamento", example = "PROCESSING") String status
) {
}
