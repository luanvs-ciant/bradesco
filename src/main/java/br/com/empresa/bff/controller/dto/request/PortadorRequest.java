package br.com.empresa.bff.controller.dto.request;

import jakarta.validation.constraints.NotBlank;

public record PortadorRequest(@NotBlank String portadorId) {
}
