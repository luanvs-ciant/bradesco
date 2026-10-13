package br.com.empresa.bff.gateway.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import br.com.empresa.bff.config.feign.BloqueioCartaoFeignConfig;
import br.com.empresa.bff.gateway.dto.BloqueioCartaoDownstreamRequest;
import br.com.empresa.bff.gateway.dto.BloqueioCartaoDownstreamResponse;

@FeignClient(name = "bloqueioCartao", configuration = BloqueioCartaoFeignConfig.class)
public interface BloqueioCartaoClient {

    @PostMapping(value = "${bloqueio-cartao.path}", consumes = MediaType.APPLICATION_JSON_VALUE)
    BloqueioCartaoDownstreamResponse bloquear(@RequestBody BloqueioCartaoDownstreamRequest request);
}
