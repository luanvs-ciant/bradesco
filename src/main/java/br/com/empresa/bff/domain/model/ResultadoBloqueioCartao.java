package br.com.empresa.bff.domain.model;

public record ResultadoBloqueioCartao(
        String protocoloId,
        String status
) {
}