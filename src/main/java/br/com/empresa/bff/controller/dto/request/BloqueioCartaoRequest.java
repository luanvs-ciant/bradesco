package br.com.empresa.bff.controller.dto.request;

import java.util.List;

import br.com.empresa.bff.controller.dto.TipoBloqueioDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record BloqueioCartaoRequest(
        @Schema(description = "Identificador do cartão", example = "123456") @NotBlank String cartaoId,
        @Schema(description = "Tipo de bloqueio", example = "DEFINITIVO") @NotNull TipoBloqueioDto tipoBloqueio,
        @Schema(description = "Motivo opcional em texto livre", example = "SOLICITACAO_CLIENTE") String motivo,
        @Schema(description = "Portadores selecionados; ao menos um") @NotEmpty List<@Valid PortadorRequest> portadores
) {
}
