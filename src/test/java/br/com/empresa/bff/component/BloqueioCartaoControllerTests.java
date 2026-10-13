package br.com.empresa.bff.component;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;

import br.com.empresa.bff.application.service.BloqueioCartaoApplicationService;
import br.com.empresa.bff.controller.BloqueioCartaoController;
import br.com.empresa.bff.controller.dto.request.BloqueioCartaoRequest;
import br.com.empresa.bff.controller.dto.response.BloqueioCartaoResponse;
import br.com.empresa.bff.exception.DownstreamIntegrationException;

@WebMvcTest(BloqueioCartaoController.class)
class BloqueioCartaoControllerTests {

    private static final String REQUEST_PATH = "/operacoes/bloqueio-cartoes";
    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String VALID_REQUEST_WITH_REASON = """
            {
              "cartaoId": "123456",
              "tipoBloqueio": "DEFINITIVO",
              "motivo": "SOLICITACAO_CLIENTE",
              "portadores": [{"portadorId": "987654"}, {"portadorId": "456789"}]
            }
            """;
    private static final String VALID_REQUEST_WITHOUT_REASON = """
            {
              "cartaoId": "123456",
              "tipoBloqueio": "TEMPORARIO",
              "portadores": [{"portadorId": "987654"}]
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BloqueioCartaoApplicationService applicationService;

    @Test
    void shouldAcceptRequestAndPreserveHttpAndJsonContracts() throws Exception {
        when(applicationService.bloquear(any()))
                .thenReturn(new BloqueioCartaoResponse("protocol-123", "PROCESSING"));

        mockMvc.perform(post(REQUEST_PATH)
                        .header(CORRELATION_ID_HEADER, "corr-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST_WITH_REASON))
                .andExpect(status().isAccepted())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {"protocoloId":"protocol-123","status":"PROCESSING"}
                        """, JsonCompareMode.STRICT))
                .andExpect(header().string(CORRELATION_ID_HEADER, "corr-123"));

        ArgumentCaptor<BloqueioCartaoRequest> requestCaptor =
                ArgumentCaptor.forClass(BloqueioCartaoRequest.class);
        verify(applicationService).bloquear(requestCaptor.capture());

        BloqueioCartaoRequest capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.cartaoId()).isEqualTo("123456");
        assertThat(capturedRequest.tipoBloqueio().name()).isEqualTo("DEFINITIVO");
        assertThat(capturedRequest.motivo()).isEqualTo("SOLICITACAO_CLIENTE");
        assertThat(capturedRequest.portadores())
                .extracting("portadorId")
                .containsExactly("987654", "456789");
    }

    @Test
    void shouldAcceptRequestWhenOptionalReasonIsOmitted() throws Exception {
        when(applicationService.bloquear(any()))
                .thenReturn(new BloqueioCartaoResponse("protocol-123", "PROCESSING"));

        mockMvc.perform(post(REQUEST_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST_WITHOUT_REASON))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.protocoloId").value("protocol-123"))
                .andExpect(jsonPath("$.status").value("PROCESSING"));

        ArgumentCaptor<BloqueioCartaoRequest> requestCaptor =
                ArgumentCaptor.forClass(BloqueioCartaoRequest.class);
        verify(applicationService).bloquear(requestCaptor.capture());
        assertThat(requestCaptor.getValue().motivo()).isNull();
    }

    @ParameterizedTest
    @MethodSource("invalidRequests")
    void shouldRejectInvalidRequestsWithValidationError(
            String requestBody, String expectedField) throws Exception {
        mockMvc.perform(post(REQUEST_PATH)
                        .header(CORRELATION_ID_HEADER, "corr-invalid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(header().string(CORRELATION_ID_HEADER, "corr-invalid"))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message", containsString(expectedField)))
                .andExpect(jsonPath("$.traceId").value("corr-invalid"))
                .andExpect(jsonPath("$.stackTrace").doesNotExist());

        verify(applicationService, never()).bloquear(any());
    }

    static Stream<Arguments> invalidRequests() {
        return Stream.of(
                Arguments.of("""
                        {"cartaoId":"123456","tipoBloqueio":"TEMPORARIO"}
                        """, "portadores"),
                Arguments.of("""
                        {"cartaoId":"123456","tipoBloqueio":"TEMPORARIO","portadores":[]}
                        """, "portadores"),
                Arguments.of("""
                        {"cartaoId":"123456","tipoBloqueio":"TEMPORARIO","portadores":[{}]}
                        """, "portadores"),
                Arguments.of("""
                        {"tipoBloqueio":"TEMPORARIO","portadores":[{"portadorId":"987654"}]}
                        """, "cartaoId"),
                Arguments.of("""
                        {"cartaoId":"123456","portadores":[{"portadorId":"987654"}]}
                        """, "tipoBloqueio"));
    }

    @Test
    void shouldMapInvalidEnumToValidationErrorContract() throws Exception {
        mockMvc.perform(post(REQUEST_PATH)
                        .header(CORRELATION_ID_HEADER, "corr-enum")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST_WITHOUT_REASON.replace("TEMPORARIO", "INVALIDO")))
                .andExpect(status().isBadRequest())
                .andExpect(header().string(CORRELATION_ID_HEADER, "corr-enum"))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.traceId").value("corr-enum"))
                .andExpect(jsonPath("$.stackTrace").doesNotExist());

        verify(applicationService, never()).bloquear(any());
    }

    @ParameterizedTest
    @MethodSource("serviceFailures")
    void shouldMapServiceFailuresToSafeGlobalErrorContract(
            RuntimeException failure, int expectedStatus, String expectedCode) throws Exception {
        doThrow(failure).when(applicationService).bloquear(any());

        mockMvc.perform(post(REQUEST_PATH)
                        .header(CORRELATION_ID_HEADER, "corr-error")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST_WITHOUT_REASON))
                .andExpect(status().is(expectedStatus))
                .andExpect(header().string(CORRELATION_ID_HEADER, "corr-error"))
                .andExpect(jsonPath("$.code").value(expectedCode))
                .andExpect(jsonPath("$.traceId").value("corr-error"))
                .andExpect(jsonPath("$.stackTrace").doesNotExist())
                .andExpect(jsonPath("$.message", not(containsString("secret"))));
    }

    static Stream<Arguments> serviceFailures() {
        return Stream.of(
                Arguments.of(
                        new DownstreamIntegrationException(HttpStatus.GATEWAY_TIMEOUT, "secret"),
                        HttpStatus.GATEWAY_TIMEOUT.value(),
                        "DOWNSTREAM_TIMEOUT"),
                Arguments.of(
                        new IllegalStateException("secret"),
                        HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        "INTERNAL_ERROR"));
    }

    @Test
    void shouldMapIntegrationExceptionWithoutExposingInternalDetails() throws Exception {
        doThrow(new DownstreamIntegrationException(HttpStatus.BAD_GATEWAY, "internal details"))
                .when(applicationService).bloquear(any());

        mockMvc.perform(post(REQUEST_PATH)
                        .header(CORRELATION_ID_HEADER, "corr-integration")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST_WITHOUT_REASON))
                .andExpect(status().isBadGateway())
                .andExpect(header().string(CORRELATION_ID_HEADER, "corr-integration"))
                .andExpect(jsonPath("$.code").value("DOWNSTREAM_ERROR"))
                .andExpect(jsonPath("$.message").value("Erro na integração com o serviço"))
                .andExpect(jsonPath("$.message", not(containsString("internal details"))))
                .andExpect(jsonPath("$.traceId").value("corr-integration"))
                .andExpect(jsonPath("$.stackTrace").doesNotExist());
    }

    @Test
    void shouldGenerateCorrelationIdWhenHeaderIsOmitted() throws Exception {
        when(applicationService.bloquear(any()))
                .thenReturn(new BloqueioCartaoResponse("protocol-123", "PROCESSING"));

        mockMvc.perform(post(REQUEST_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST_WITHOUT_REASON))
                .andExpect(status().isAccepted())
                .andExpect(header().string(CORRELATION_ID_HEADER, not(emptyOrNullString())));
    }
}
