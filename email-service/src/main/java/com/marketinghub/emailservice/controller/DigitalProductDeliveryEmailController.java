package com.marketinghub.emailservice.controller;

import com.marketinghub.emailservice.dto.DigitalProductDeliveryEmailRequest;
import com.marketinghub.emailservice.dto.EmailResponseDto;
import com.marketinghub.emailservice.service.DigitalProductDeliveryEmailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Expõe envio transacional de entrega pós-compra para produtos digitais.
 */
@RestController
@RequestMapping("/api/v1/product-deliveries")
@Tag(name = "Product Deliveries", description = "Envio de emails pós-compra para produtos digitais")
public class DigitalProductDeliveryEmailController {

    private final DigitalProductDeliveryEmailService service;

    public DigitalProductDeliveryEmailController(DigitalProductDeliveryEmailService service) {
        this.service = service;
    }

    /** Recebe uma solicitação de entrega digital e dispara o email transacional. */
    @Operation(summary = "Enviar entrega digital", description = "Dispara email pós-compra com página de entrega e link de backup")
    @PostMapping("/send")
    public ResponseEntity<EmailResponseDto> send(@Valid @RequestBody DigitalProductDeliveryEmailRequest request) {
        return ResponseEntity.ok(service.send(request));
    }
}
