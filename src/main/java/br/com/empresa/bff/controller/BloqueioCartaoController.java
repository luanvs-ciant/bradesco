package br.com.empresa.bff.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.empresa.bff.application.service.BloqueioCartaoApplicationService;
import br.com.empresa.bff.controller.dto.request.BloqueioCartaoRequest;
import br.com.empresa.bff.controller.dto.response.BloqueioCartaoResponse;
import br.com.empresa.bff.exception.GlobalExceptionHandler.ErrorResponse;
import br.com.empresa.bff.observability.CorrelationId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/operacoes/bloqueio-cartoes")
public class BloqueioCartaoController {

    private final BloqueioCartaoApplicationService applicationService;

    public BloqueioCartaoController(BloqueioCartaoApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @PostMapping
        @Operation(summary = "Solicitar bloqueio de portadores de um cartão",
            description = "Aceita uma operação assíncrona. O motivo é opcional. "
                + "A credencial X-Api-Key é configurada pelo servidor somente para o downstream e "
                + "não deve ser enviada pelo consumidor.",
            parameters = @Parameter(name = CorrelationId.HEADER, in = ParameterIn.HEADER,
                description = "Opcional. Letras ASCII, números, ponto, hífen ou sublinhado, até 128 caracteres. "
                    + "Se estiver ausente ou inválido, o BFF gera um UUID.",
                required = false,
                schema = @Schema(type = "string", maxLength = 128, pattern = "[A-Za-z0-9._-]{1,128}"),
                example = "corr-123"))
        @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Operação aceita para processamento",
                headers = @Header(name = CorrelationId.HEADER, description = "Identificador da requisição",
                    schema = @Schema(type = "string")),
                content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = BloqueioCartaoResponse.class),
                    examples = @ExampleObject(value = "{\"protocoloId\":\"8f6d2c10\",\"status\":\"PROCESSING\"}"))),
            @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR: campos inválidos, JSON malformado ou enum inválido",
                headers = @Header(name = CorrelationId.HEADER, description = "Identificador da requisição",
                    schema = @Schema(type = "string")),
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "INTERNAL_ERROR: falha inesperada",
                headers = @Header(name = CorrelationId.HEADER, description = "Identificador da requisição",
                    schema = @Schema(type = "string")),
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "502", description = "DOWNSTREAM_ERROR: resposta de erro ou resposta ilegível do serviço",
                headers = @Header(name = CorrelationId.HEADER, description = "Identificador da requisição",
                    schema = @Schema(type = "string")),
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "503", description = "DOWNSTREAM_UNAVAILABLE: falha de conexão ou HTTP 503 downstream",
                headers = @Header(name = CorrelationId.HEADER, description = "Identificador da requisição",
                    schema = @Schema(type = "string")),
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "504", description = "DOWNSTREAM_TIMEOUT: timeout de conexao/leitura ou HTTP 504 downstream",
                headers = @Header(name = CorrelationId.HEADER, description = "Identificador da requisição",
                    schema = @Schema(type = "string")),
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
        })
    public ResponseEntity<BloqueioCartaoResponse> bloquear(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
                content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = BloqueioCartaoRequest.class),
                    examples = @ExampleObject(value = """
                        {"cartaoId":"123456","tipoBloqueio":"DEFINITIVO","motivo":"SOLICITACAO_CLIENTE",
                         "portadores":[{"portadorId":"987654"},{"portadorId":"456789"}]}
                        """)))
            @Valid @RequestBody BloqueioCartaoRequest request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(applicationService.bloquear(request));
    }
}
