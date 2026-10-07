package br.com.empresa.bff.domain.model;

import java.util.List;

public record BloqueioCartao(
        String cartaoId,
        TipoBloqueio tipoBloqueio,
        String motivo,
        List<Portador> portadores
) {
}
