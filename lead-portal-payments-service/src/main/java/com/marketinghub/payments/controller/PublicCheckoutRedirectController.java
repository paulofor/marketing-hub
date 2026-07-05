package com.marketinghub.payments.controller;

import com.marketinghub.payments.service.PublicCheckoutRedirectService;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Responsabilidade: expor o redirecionamento público de checkout usado por clientes do Lead Portal. */
@RestController
@RequestMapping("/api/public/lead-portal/purchases")
public class PublicCheckoutRedirectController {

    private final PublicCheckoutRedirectService redirectService;

    /** Inicializa o controller com o serviço de redirecionamento público. */
    public PublicCheckoutRedirectController(PublicCheckoutRedirectService redirectService) {
        this.redirectService = redirectService;
    }

    /** Registra o clique no checkout e redireciona para o provedor de pagamento. */
    @GetMapping("/{purchaseId}/checkout")
    public ResponseEntity<Void> redirectToCheckout(
            @PathVariable Long purchaseId,
            @RequestParam(name = "sid", required = false) String submissionId) {
        return redirectService.registerCheckoutAccess(purchaseId, submissionId)
                .map(url -> ResponseEntity.status(HttpStatus.FOUND)
                        .location(URI.create(url))
                        .<Void>build())
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
