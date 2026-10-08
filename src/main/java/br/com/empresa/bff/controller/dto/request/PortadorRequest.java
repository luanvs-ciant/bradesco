package br.com.empresa.bff.controller.dto.request;

import jakarta.validation.constraints.NotBlank;

public record PortadorRequest(@NotBlank(message = "must not be blank") String portadorId) {
}
