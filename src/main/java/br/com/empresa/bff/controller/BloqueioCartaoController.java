package br.com.empresa.bff.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.empresa.bff.controller.dto.request.BloqueioCartaoRequest;
import br.com.empresa.bff.controller.dto.response.BloqueioCartaoResponse;
import br.com.empresa.bff.service.BloqueioCartaoService;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/operacoes/bloqueio-cartoes")
public class BloqueioCartaoController implements BloqueioCartaoApi {

    private final BloqueioCartaoService service;

    @Override
    @PostMapping
    public ResponseEntity<BloqueioCartaoResponse> bloquear(@RequestBody BloqueioCartaoRequest request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(service.bloquear(request));
    }
}
