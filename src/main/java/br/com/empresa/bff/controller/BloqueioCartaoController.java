package br.com.empresa.bff.controller;

import br.com.empresa.bff.application.service.BloqueioCartaoApplicationService;
import br.com.empresa.bff.controller.dto.request.BloqueioCartaoRequest;
import br.com.empresa.bff.controller.dto.response.BloqueioCartaoResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/operacoes/bloqueio-cartoes")
public class BloqueioCartaoController {

    private final BloqueioCartaoApplicationService applicationService;

    public BloqueioCartaoController(BloqueioCartaoApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @PostMapping
    public ResponseEntity<BloqueioCartaoResponse> bloquear(
            @Valid @RequestBody BloqueioCartaoRequest request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(applicationService.bloquear(request));
    }
}