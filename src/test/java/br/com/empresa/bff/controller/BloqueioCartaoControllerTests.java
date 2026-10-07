package br.com.empresa.bff.controller;

import br.com.empresa.bff.application.service.BloqueioCartaoApplicationService;
import br.com.empresa.bff.controller.dto.response.BloqueioCartaoResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BloqueioCartaoController.class)
class BloqueioCartaoControllerTests {

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