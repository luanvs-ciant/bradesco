package br.com.empresa.bff.controller.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record PortadorRequest(@Schema(description = "Identificador do portador", example = "987654")
	@NotBlank String portadorId) {
}
