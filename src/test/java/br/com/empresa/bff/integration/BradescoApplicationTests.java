package br.com.empresa.bff.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import br.com.empresa.bff.domain.gateway.BloqueioCartaoGateway;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class BradescoApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private BloqueioCartaoGateway gateway;

	@Test
	void contextLoads() {
	}

	@Test
	void shouldPublishOperationAndErrorSchemas() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(header().exists("X-Correlation-ID"))
				.andExpect(jsonPath("$.paths['/operacoes/bloqueio-cartoes'].post.parameters[0].name")
						.value("X-Correlation-ID"))
				.andExpect(jsonPath("$.paths['/operacoes/bloqueio-cartoes'].post.responses['400'].headers['X-Correlation-ID']").exists())
				.andExpect(jsonPath("$.paths['/operacoes/bloqueio-cartoes'].post.responses['202']").exists())
				.andExpect(jsonPath("$.paths['/operacoes/bloqueio-cartoes'].post.responses['400']").exists())
				.andExpect(jsonPath("$.paths['/operacoes/bloqueio-cartoes'].post.responses['500']").exists())
				.andExpect(jsonPath("$.paths['/operacoes/bloqueio-cartoes'].post.responses['502']").exists())
				.andExpect(jsonPath("$.paths['/operacoes/bloqueio-cartoes'].post.responses['503']").exists())
				.andExpect(jsonPath("$.paths['/operacoes/bloqueio-cartoes'].post.responses['504']").exists())
				.andExpect(jsonPath("$.components.schemas.ErrorResponse.properties.traceId").exists())
				.andExpect(jsonPath("$.components.schemas.BloqueioCartaoRequest.required",
						org.hamcrest.Matchers.containsInAnyOrder("cartaoId", "tipoBloqueio", "portadores")))
				.andExpect(jsonPath("$.components.schemas.BloqueioCartaoRequest.properties.motivo.description").exists());
	}

}
