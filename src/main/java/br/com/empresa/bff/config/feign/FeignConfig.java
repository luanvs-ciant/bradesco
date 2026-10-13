package br.com.empresa.bff.config.feign;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

import br.com.empresa.bff.gateway.client.BloqueioCartaoClient;

@Configuration(proxyBeanMethods = false)
@EnableFeignClients(clients = BloqueioCartaoClient.class)
public class FeignConfig {
}
