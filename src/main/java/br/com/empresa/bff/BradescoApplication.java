package br.com.empresa.bff;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients(basePackages = "br.com.empresa.bff.gateway.client")
public class BradescoApplication {

	public static void main(String[] args) {
		SpringApplication.run(BradescoApplication.class, args);
	}

}
