package br.com.empresa.bff.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.cloud.openfeign.FeignClientProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import br.com.empresa.bff.config.feign.BloqueioCartaoFeignConfig;
import br.com.empresa.bff.domain.gateway.BloqueioCartaoGateway;
import feign.Client;
import feign.RequestInterceptor;
import feign.Retryer;
import feign.codec.ErrorDecoder;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class BradescoApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ApplicationContext applicationContext;

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


	@Test
	void shouldKeepFeignConfigurationScopedToItsClient() {
		// BloqueioCartaoFeignConfig nao pode ser @Configuration: seus beans virariam padrao de todo client Feign.
		for (Class<?> type : List.of(Client.class, ErrorDecoder.class, RequestInterceptor.class, Retryer.class,
				okhttp3.OkHttpClient.class)) {
			assertThat(applicationContext.getBeansOfType(type)).as(type.getName()).isEmpty();
		}
	}

	@Test
	void shouldNotStartWithoutDownstreamApiKey() {
		// A API key e um segredo: nao pode ter valor padrao no application.yml.
		var runner = applicationYmlWithoutEnvironment()
				.withBean(PropertySourcesPlaceholderConfigurer.class, PropertySourcesPlaceholderConfigurer::new)
				.withUserConfiguration(BloqueioCartaoFeignConfig.class);

		runner.run(context -> assertThat(context).hasFailed()
				.getFailure().hasStackTraceContaining("BLOQUEIO_SERVICE_API_KEY"));
		runner.withPropertyValues("bloqueio-cartao.api-key=test-key")
				.run(context -> assertThat(context).hasNotFailed());
	}

	@Test
	void shouldApplyFeignClientDefaultsFromApplicationYml() {
		// Chaves do Feign com erro de digitacao sao ignoradas sem erro e o Feign usa os padroes dele.
		applicationYmlWithoutEnvironment().run(context -> {
			var clients = Binder.get(context.getEnvironment())
					.bind("spring.cloud.openfeign.client", FeignClientProperties.class)
					.get().getConfig();

			assertThat(clients).containsKey("bloqueioCartao");
			var client = clients.get("bloqueioCartao");
			assertThat(client.getUrl()).isEqualTo("http://localhost:8081");
			assertThat(client.getConnectTimeout()).isEqualTo(3000);
			assertThat(client.getReadTimeout()).isEqualTo(5000);
		});
	}

	// Carrega somente o application.yml real, sem variaveis de ambiente nem propriedades de sistema.
	private static ApplicationContextRunner applicationYmlWithoutEnvironment() {
		return new ApplicationContextRunner()
				.withInitializer(context -> {
					var sources = context.getEnvironment().getPropertySources();
					sources.remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
					sources.remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
				})
				.withInitializer(new ConfigDataApplicationContextInitializer());
	}

}
