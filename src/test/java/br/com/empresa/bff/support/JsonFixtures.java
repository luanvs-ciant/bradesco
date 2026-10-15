package br.com.empresa.bff.support;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

public final class JsonFixtures {

    private static final JsonMapper MAPPER = JsonMapper.builder()
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    private JsonFixtures() {
    }

    public static String json(String path) {
        try (var stream = JsonFixtures.class.getResourceAsStream("/fixtures/" + path)) {
            if (stream == null) {
                throw new AssertionError("JSON fixture not found: " + path);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new AssertionError("Could not read JSON fixture: " + path, exception);
        }
    }

    public static <T> T read(String path, Class<T> type) {
        try {
            return MAPPER.readValue(json(path), type);
        } catch (JacksonException exception) {
            throw new AssertionError("Could not deserialize JSON fixture: " + path + " as " + type.getName(),
                    exception);
        }
    }
}
