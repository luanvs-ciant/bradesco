package br.com.empresa.bff.gateway.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "unconfiguredClient")
interface UnconfiguredFeignClient {

    @GetMapping("/probe")
    void probe();
}