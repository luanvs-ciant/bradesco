package br.com.empresa.bff;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import br.com.empresa.bff.domain.gateway.BloqueioCartaoGateway;

@SpringBootTest
class BradescoApplicationTests {

	@MockitoBean
	private BloqueioCartaoGateway gateway;

	@Test
	void contextLoads() {
	}

}
