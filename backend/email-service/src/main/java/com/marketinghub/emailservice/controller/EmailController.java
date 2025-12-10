package com.marketinghub.emailservice.controller;

import com.marketinghub.emailservice.dto.BulkEmailRequestDto;
import com.marketinghub.emailservice.dto.EmailRequestDto;
import com.marketinghub.emailservice.dto.EmailResponseDto;
import com.marketinghub.emailservice.service.EmailOrchestratorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/emails")
@Tag(name = "Email Service", description = "API para envio de e-mails transacionais e de campanhas")
public class EmailController {

    private final EmailOrchestratorService emailOrchestratorService;

    public EmailController(EmailOrchestratorService emailOrchestratorService) {
        this.emailOrchestratorService = emailOrchestratorService;
    }

    @Operation(summary = "Enviar e-mail único", description = "Orquestra o envio de um e-mail utilizando templates do Marketing Hub")
    @PostMapping("/send")
    public ResponseEntity<EmailResponseDto> sendEmail(@Valid @RequestBody EmailRequestDto request) {
        return ResponseEntity.ok(emailOrchestratorService.sendEmail(request));
    }

    @Operation(summary = "Enviar e-mails em lote", description = "Dispara vários e-mails de uma única vez a partir do mesmo payload")
    @PostMapping("/bulk")
    public ResponseEntity<List<EmailResponseDto>> sendBulk(@Valid @RequestBody BulkEmailRequestDto request) {
        return ResponseEntity.ok(emailOrchestratorService.sendBulk(request.emails()));
    }

    @Operation(summary = "Consultar status de envio", description = "Retorna informações sobre o processamento de uma requisição de envio de e-mail")
    @GetMapping("/{requestId}")
    public ResponseEntity<EmailResponseDto> getStatus(@PathVariable String requestId) {
        return ResponseEntity.ok(emailOrchestratorService.getStatus(requestId));
    }
}
