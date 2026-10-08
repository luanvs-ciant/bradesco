package br.com.empresa.bff.controller.dto.request;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import br.com.empresa.bff.controller.dto.TipoBloqueioDto;

public record BloqueioCartaoRequest(
        @NotBlank(message = "must not be blank") String cartaoId,
        @NotNull(message = "must not be null") TipoBloqueioDto tipoBloqueio,
        String motivo,
        @NotEmpty(message = "must not be empty") List<@Valid PortadorRequest> portadores
) {
}
