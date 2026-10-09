package br.com.empresa.bff.controller;

import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.SocketTimeoutException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.server.ResponseStatusException;

import br.com.empresa.bff.application.service.BloqueioCartaoApplicationService;
import br.com.empresa.bff.controller.dto.response.BloqueioCartaoResponse;
import br.com.empresa.bff.exception.DownstreamIntegrationException;

@WebMvcTest(BloqueioCartaoController.class)
class BloqueioCartaoControllerTests {

  private static final String VALID_REQUEST = """
      {"cartaoId":"123456","tipoBloqueio":"TEMPORARIO","portadores":[{"portadorId":"987654"}]}
      """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BloqueioCartaoApplicationService applicationService;

    @Test
    void shouldAcceptValidRequestWithoutOptionalReason() throws Exception {
        when(applicationService.bloquear(any()))
                .thenReturn(new BloqueioCartaoResponse("protocol-123", "PROCESSING"));

        mockMvc.perform(post("/operacoes/bloqueio-cartoes")
                        .contentType("application/json")
                        .content("""
                                {
                                  "cartaoId": "123456",
                                  "tipoBloqueio": "DEFINITIVO",
                                  "portadores": [{"portadorId": "987654"}]
                                }
                                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.protocoloId").value("protocol-123"))
                .andExpect(jsonPath("$.status").value("PROCESSING"));

        verify(applicationService).bloquear(any());
    }

    @Test
        void shouldCorrelateValidationErrorsAndRejectInvalidEnum() throws Exception {
      mockMvc.perform(post("/operacoes/bloqueio-cartoes")
          .header("X-Correlation-ID", "corr-123")
          .contentType("application/json")
          .content(VALID_REQUEST.replace("TEMPORARIO", "INVALIDO")))
        .andExpect(status().isBadRequest())
        .andExpect(header().string("X-Correlation-ID", "corr-123"))
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.traceId").value("corr-123"));
      verify(applicationService, never()).bloquear(any());
        }

        @Test
        void shouldMapJourneyAndIntegrationErrorsToSafeContract() throws Exception {
      Object[][] failures = {
        {new ResponseStatusException(HttpStatus.FORBIDDEN, "secret"), 403, "FORBIDDEN"},
        {new ResourceAccessException("secret", new SocketTimeoutException()), 504, "DOWNSTREAM_TIMEOUT"},
        {new IllegalStateException("secret"), 500, "INTERNAL_ERROR"}
      };
      for (Object[] failure : failures) {
            doThrow((RuntimeException) failure[0]).when(applicationService).bloquear(any());
          mockMvc.perform(post("/operacoes/bloqueio-cartoes")
              .header("X-Correlation-ID", "corr-error")
              .contentType("application/json").content(VALID_REQUEST))
            .andExpect(status().is((Integer) failure[1]))
            .andExpect(header().string("X-Correlation-ID", "corr-error"))
            .andExpect(jsonPath("$.code").value(failure[2]))
            .andExpect(jsonPath("$.traceId").value("corr-error"))
            .andExpect(jsonPath("$.stackTrace").doesNotExist())
            .andExpect(jsonPath("$.message", not(org.hamcrest.Matchers.containsString("secret"))));
      }
        }

    @Test
    void shouldReturnMappedStatusForIntegrationException() throws Exception {
      doThrow(new DownstreamIntegrationException(HttpStatus.BAD_GATEWAY, "internal details"))
          .when(applicationService).bloquear(any());

      mockMvc.perform(post("/operacoes/bloqueio-cartoes")
              .header("X-Correlation-ID", "corr-integration")
              .contentType("application/json")
              .content(VALID_REQUEST))
          .andExpect(status().isBadGateway())
          .andExpect(header().string("X-Correlation-ID", "corr-integration"))
          .andExpect(jsonPath("$.code").value("DOWNSTREAM_ERROR"))
          .andExpect(jsonPath("$.message").value("Erro na integração com o serviço"))
          .andExpect(jsonPath("$.message", not(org.hamcrest.Matchers.containsString("internal details"))));
    }

        @Test
        void shouldGenerateCorrelationIdOnAcceptedRequest() throws Exception {
      when(applicationService.bloquear(any()))
        .thenReturn(new BloqueioCartaoResponse("protocol-123", "PROCESSING"));
      mockMvc.perform(post("/operacoes/bloqueio-cartoes")
          .contentType("application/json").content(VALID_REQUEST))
        .andExpect(status().isAccepted())
                .andExpect(header().string("X-Correlation-ID", not(emptyOrNullString())));
        }

        @Test
    void shouldRejectRequestWithoutPortadores() throws Exception {
        mockMvc.perform(post("/operacoes/bloqueio-cartoes")
                        .contentType("application/json")
                        .content("""
                                {
                                  "cartaoId": "123456",
                                  "tipoBloqueio": "TEMPORARIO",
                                  "portadores": []
                                }
                                """))
                .andExpect(status().isBadRequest());

        verify(applicationService, never()).bloquear(any());
    }
}
