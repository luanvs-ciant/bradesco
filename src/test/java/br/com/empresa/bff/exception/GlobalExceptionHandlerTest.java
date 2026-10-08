package br.com.empresa.bff.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("GlobalExceptionHandler - Testes")
class GlobalExceptionHandlerTest {

    private static final String OBJECT_NAME = "BloqueioCartaoRequest";
    private static final String FIELD_NAME = "cartaoId";
    private static final String FIELD_ERROR_MESSAGE = "must not be blank";
    private static final String GENERIC_EXCEPTION_MESSAGE = "Erro de teste";

    private static final String TEST_ERROR_CODE = "TEST_ERROR";
    private static final String TEST_ERROR_MESSAGE = "Mensagem de teste";
    private static final String TEST_TRACE_ID = "trace-123";

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

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
    @DisplayName("Deve preservar 404 para recurso não encontrado no downstream")
    void testHandleIntegrationNotFound() {
        IntegrationException ex = new IntegrationException(404, "Resource not found");

        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
            handler.handleIntegrationException(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(ErrorCode.INTEGRATION_ERROR.name(), response.getBody().code());
        assertEquals("Resource not found", response.getBody().message());
    }

    @Test
    @DisplayName("Deve preservar 422 para erro de validação no downstream")
    void testHandleIntegrationUnprocessableContent() {
        IntegrationException ex = new IntegrationException(422, "Invalid data");

        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
            handler.handleIntegrationException(ex);

        assertEquals(HttpStatus.UNPROCESSABLE_CONTENT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(ErrorCode.INTEGRATION_ERROR.name(), response.getBody().code());
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
