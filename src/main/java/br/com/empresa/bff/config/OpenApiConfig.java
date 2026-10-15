package br.com.empresa.bff.config;

import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import br.com.empresa.bff.exception.GlobalExceptionHandler.ErrorResponse;
import br.com.empresa.bff.observability.CorrelationId;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI customOpenAPI() {
        Components components = new Components();
        ModelConverters.getInstance().read(ErrorResponse.class)
                .forEach(components::addSchemas);
        components.addResponses("ValidationError", errorResponse(
                        "VALIDATION_ERROR: campos inválidos, JSON malformado ou enum inválido"))
                .addResponses("InternalError", errorResponse("INTERNAL_ERROR: falha inesperada"))
                .addResponses("DownstreamError", errorResponse(
                        "DOWNSTREAM_ERROR: resposta de erro ou resposta ilegível do serviço"))
                .addResponses("DownstreamUnavailable", errorResponse(
                        "DOWNSTREAM_UNAVAILABLE: falha de conexão ou HTTP 503 downstream"))
                .addResponses("DownstreamTimeout", errorResponse(
                        "DOWNSTREAM_TIMEOUT: timeout de conexao/leitura ou HTTP 504 downstream"));
        return new OpenAPI().components(components);
    }

    private ApiResponse errorResponse(String description) {
        return new ApiResponse()
                .description(description)
                .headers(Map.of(CorrelationId.HEADER, new Header()
                        .description("Identificador da requisição")
                        .schema(new StringSchema())))
                .content(new Content().addMediaType("application/json", new MediaType()
                        .schema(new Schema<ErrorResponse>().$ref("#/components/schemas/ErrorResponse"))));
    }
}
