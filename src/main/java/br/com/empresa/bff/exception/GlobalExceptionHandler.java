package br.com.empresa.bff.exception;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import br.com.empresa.bff.observability.CorrelationId;
import io.swagger.v3.oas.annotations.media.Schema;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    public record ErrorResponse(
        @Schema(description = "Código do erro", example = "VALIDATION_ERROR") String code,
        @Schema(description = "Mensagem segura, sem detalhes internos", example = "Erro de validação") String message,
        @Schema(description = "ID retornado no cabeçalho de correlação", example = "corr-123") String traceId
    ) {}

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
        MethodArgumentNotValidException ex
    ) {
        String message = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(e -> e.getField() + ": " + e.getDefaultMessage())
            .findFirst()
            .orElse(ErrorCode.VALIDATION_ERROR.defaultMessage());

        return error(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR, message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableMessage(HttpMessageNotReadableException ex) {
        return error(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR);
    }

    @ExceptionHandler(DownstreamIntegrationException.class)
    public ResponseEntity<ErrorResponse> handleIntegrationException(DownstreamIntegrationException ex) {
        int downstreamStatus = ex.getStatus().value();
        if (downstreamStatus == HttpStatus.SERVICE_UNAVAILABLE.value()) {
            return error(HttpStatus.SERVICE_UNAVAILABLE, ErrorCode.DOWNSTREAM_UNAVAILABLE);
        }
        if (downstreamStatus == HttpStatus.GATEWAY_TIMEOUT.value()) {
            return error(HttpStatus.GATEWAY_TIMEOUT, ErrorCode.DOWNSTREAM_TIMEOUT);
        }
        return error(HttpStatus.BAD_GATEWAY, ErrorCode.DOWNSTREAM_ERROR);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_ERROR);
    }

    private ResponseEntity<ErrorResponse> error(HttpStatus status, ErrorCode code) {
        return error(status, code, code.defaultMessage());
    }

    private ResponseEntity<ErrorResponse> error(HttpStatus status, ErrorCode code, String message) {
        String traceId = traceId();
        logger.atLevel(status.is5xxServerError() ? org.slf4j.event.Level.ERROR : org.slf4j.event.Level.WARN)
                .addKeyValue("code", code.name()).addKeyValue("status", status.value())
                .addKeyValue("traceId", traceId).log("Requisição rejeitada");
        return ResponseEntity.status(status).body(new ErrorResponse(code.name(), message, traceId));
    }

    private String traceId() {
        String correlationId = MDC.get(CorrelationId.MDC_KEY);
        if (correlationId == null) {
            correlationId = UUID.randomUUID().toString();
            MDC.put(CorrelationId.MDC_KEY, correlationId);
        }
        return correlationId;
    }
}
