package br.com.empresa.bff.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.server.ResponseStatusException;

@DisplayName("GlobalExceptionHandler - Testes")
class GlobalExceptionHandlerTest {

    private static final String OBJECT_NAME = "BloqueioCartaoRequest";
    private static final String FIELD_NAME = "cartaoId";
    private static final String FIELD_ERROR_MESSAGE = "não deve estar vazio";
    private static final String GENERIC_EXCEPTION_MESSAGE = "Erro de teste";

    private static final String TEST_ERROR_CODE = "TEST_ERROR";
    private static final String TEST_ERROR_MESSAGE = "Mensagem de teste";
    private static final String TEST_TRACE_ID = "trace-123";

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @AfterEach
    void clearContext() {
        MDC.clear();
    }

    @Test
    void shouldMapAuthorizationWithoutExposingReason() {
        for (HttpStatus status : List.of(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN)) {
            var response = handler.handleAuthorizationException(new ResponseStatusException(status, "secret"));
            assertEquals(status, response.getStatusCode());
            assertFalse(response.getBody().message().contains("secret"));
            assertFalse(response.getBody().traceId().isBlank());
        }
    }

    @Test
    void shouldMapTimeoutAndUnavailableService() {
        var timeout = handler.handleDownstreamException(new ResourceAccessException("secret",
                new SocketTimeoutException("secret")));
        assertEquals(HttpStatus.GATEWAY_TIMEOUT, timeout.getStatusCode());
        assertEquals(ErrorCode.DOWNSTREAM_TIMEOUT.name(), timeout.getBody().code());
        var unavailable = handler.handleDownstreamException(new ResourceAccessException("secret",
                new ConnectException("secret")));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, unavailable.getStatusCode());
        assertEquals(ErrorCode.DOWNSTREAM_UNAVAILABLE.name(), unavailable.getBody().code());
    }

    @Test
    void shouldMapDownstreamStatusesWithoutExposingDetails() {
        for (HttpStatus status : List.of(HttpStatus.INTERNAL_SERVER_ERROR,
                HttpStatus.SERVICE_UNAVAILABLE, HttpStatus.GATEWAY_TIMEOUT)) {
            var response = handler.handleDownstreamException(new HttpServerErrorException(status, "secret"));
            assertEquals(status == HttpStatus.INTERNAL_SERVER_ERROR ? HttpStatus.BAD_GATEWAY : status,
                    response.getStatusCode());
            assertFalse(response.getBody().message().contains("secret"));
        }
    }

        @Test
        void shouldMapIntegrationExceptionToApprovedPublicStatuses() {
        var badGateway = handler.handleIntegrationException(new DownstreamIntegrationException(
            HttpStatus.BAD_GATEWAY, "internal downstream details"));
        assertEquals(HttpStatus.BAD_GATEWAY, badGateway.getStatusCode());
        assertEquals(ErrorCode.DOWNSTREAM_ERROR.name(), badGateway.getBody().code());
        assertFalse(badGateway.getBody().message().contains("internal downstream details"));

        var unavailable = handler.handleIntegrationException(new DownstreamIntegrationException(
            HttpStatus.SERVICE_UNAVAILABLE, "internal downstream details"));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, unavailable.getStatusCode());
        assertEquals(ErrorCode.DOWNSTREAM_UNAVAILABLE.name(), unavailable.getBody().code());

        var timeout = handler.handleIntegrationException(new DownstreamIntegrationException(
            HttpStatus.GATEWAY_TIMEOUT, "internal downstream details"));
        assertEquals(HttpStatus.GATEWAY_TIMEOUT, timeout.getStatusCode());
        assertEquals(ErrorCode.DOWNSTREAM_TIMEOUT.name(), timeout.getBody().code());

        var downstreamUnauthorized = handler.handleIntegrationException(new DownstreamIntegrationException(
            HttpStatus.UNAUTHORIZED, "internal downstream details"));
        assertEquals(HttpStatus.BAD_GATEWAY, downstreamUnauthorized.getStatusCode());
        assertEquals(ErrorCode.DOWNSTREAM_ERROR.name(), downstreamUnauthorized.getBody().code());
        }

    @Test
    void shouldRejectUnreadableJsonWithoutExposingDetails() {
        MDC.put("correlationId", TEST_TRACE_ID);
        var exception = new HttpMessageNotReadableException("secret downstream details",
                new MockHttpInputMessage(new byte[0]));
        var response = handler.handleUnreadableMessage(exception);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(ErrorCode.VALIDATION_ERROR.defaultMessage(), response.getBody().message());
        assertEquals(TEST_TRACE_ID, response.getBody().traceId());
    }

    @Test
    @DisplayName("Deve retornar 400 com VALIDATION_ERROR quando MethodArgumentNotValidException")
    void testHandleValidationException() {
        // Arrange
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError(OBJECT_NAME, FIELD_NAME, FIELD_ERROR_MESSAGE);

        List<FieldError> fieldErrors = new ArrayList<>();
        fieldErrors.add(fieldError);

        when(bindingResult.getFieldErrors()).thenReturn(fieldErrors);

        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

        // Act
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
            handler.handleValidationException(ex);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(ErrorCode.VALIDATION_ERROR.name(), response.getBody().code());
        assertTrue(response.getBody().message().contains(FIELD_NAME));
    }

    @Test
    @DisplayName("Deve retornar 500 com INTERNAL_ERROR quando Exception genérica")
    void testHandleGenericException() {
        // Arrange
        Exception ex = new RuntimeException(GENERIC_EXCEPTION_MESSAGE);

        // Act
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
            handler.handleGenericException(ex);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(ErrorCode.INTERNAL_ERROR.name(), response.getBody().code());
        assertEquals(ErrorCode.INTERNAL_ERROR.defaultMessage(), response.getBody().message());
    }

    @Test
    @DisplayName("Deve criar ErrorResponse com campos corretos")
    void testErrorResponseRecord() {
        // Act
        GlobalExceptionHandler.ErrorResponse response =
            new GlobalExceptionHandler.ErrorResponse(TEST_ERROR_CODE, TEST_ERROR_MESSAGE, TEST_TRACE_ID);

        // Assert
        assertEquals(TEST_ERROR_CODE, response.code());
        assertEquals(TEST_ERROR_MESSAGE, response.message());
        assertEquals(TEST_TRACE_ID, response.traceId());
    }
}
