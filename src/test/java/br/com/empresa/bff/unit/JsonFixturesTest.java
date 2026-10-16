package br.com.empresa.bff.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import br.com.empresa.bff.controller.dto.request.BloqueioCartaoRequest;
import br.com.empresa.bff.gateway.dto.BloqueioCartaoDownstreamRequest;
import br.com.empresa.bff.model.TipoBloqueio;
import br.com.empresa.bff.support.JsonFixtures;

class JsonFixturesTest {

    private static final String REQUEST = "bloqueio-cartao/request-without-reason.json";

    @Test
    void readsRawJsonAndDeserializesRecordsWithEnumsAndNestedLists() {
        assertTrue(JsonFixtures.json(REQUEST).contains("\"cartaoId\": \"123456\""));

        var request = JsonFixtures.read(REQUEST, BloqueioCartaoRequest.class);

        assertEquals("123456", request.cartaoId());
        assertEquals(TipoBloqueio.TEMPORARIO, request.tipoBloqueio());
        assertNull(request.motivo());
        assertEquals("987654", request.portadores().getFirst().portadorId());
    }

    @Test
    void returnsIndependentObjectsOnEveryRead() {
        var first = JsonFixtures.read(REQUEST, BloqueioCartaoRequest.class);
        var second = JsonFixtures.read(REQUEST, BloqueioCartaoRequest.class);

        assertNotSame(first, second);
        assertNotSame(first.portadores(), second.portadores());
        first.portadores().clear();
        assertEquals(1, second.portadores().size());
    }

    @Test
    void failsExplicitlyWhenFixtureDoesNotExist() {
        var error = assertThrows(AssertionError.class, () -> JsonFixtures.json("missing.json"));

        assertTrue(error.getMessage().contains("missing.json"));
    }

    @Test
    void failsWithPathAndCauseWhenFixtureCannotBeDeserialized() {
        var error = assertThrows(AssertionError.class, () -> JsonFixtures.read(REQUEST, Integer.class));

        assertTrue(error.getMessage().contains(REQUEST));
        assertTrue(error.getMessage().contains(Integer.class.getName()));
        assertNotNull(error.getCause());
    }

    @Test
    void rejectsUnknownFieldsInsteadOfSilentlyCreatingIncompleteObjects() {
        var path = "bloqueio-cartao/response-processing.json";
        var error = assertThrows(AssertionError.class, () -> JsonFixtures.read(path, BloqueioCartaoRequest.class));

        assertTrue(error.getMessage().contains(path));
        assertTrue(error.getCause().getMessage().contains("protocoloId"));
    }

    @Test
    void readsDownstreamRequestUsingTheSameJsonContractAsTheRequest() {
        var bloqueio = JsonFixtures.read(REQUEST, BloqueioCartaoDownstreamRequest.class);

        assertEquals("123456", bloqueio.cartaoId());
        assertEquals("987654", bloqueio.portadores().getFirst().portadorId());
    }
}
