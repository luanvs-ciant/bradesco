package br.com.empresa.bff.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Portador(@JsonProperty("portadorId") String id) {
}
