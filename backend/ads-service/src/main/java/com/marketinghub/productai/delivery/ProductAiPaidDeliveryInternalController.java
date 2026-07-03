package com.marketinghub.productai.delivery;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Responsabilidade: expor contratos internos da entrega paga para payments-service e product-ai-worker. */
@RestController
@RequestMapping("/api/internal/product-ai/personalizedsample/v1/paid-delivery/stage-executions")
public class ProductAiPaidDeliveryInternalController {
    private final ProductAiPaidDeliveryService service;

    /** Inicializa controller com o service de entrega paga. */
    public ProductAiPaidDeliveryInternalController(ProductAiPaidDeliveryService service) {
        this.service = service;
    }

    /** Recebe notificação de compra aprovada e enfileira entrega paga. */
    @PostMapping("/approved-purchase")
    public ProductAiPaidDeliveryDtos.EnqueueResponse approvedPurchase(
            @RequestBody ProductAiPaidDeliveryDtos.PurchaseApprovedRequest request) {
        return service.enqueueApprovedPurchase(request);
    }

    /** Lista entregas pendentes para consumo do worker. */
    @GetMapping("/pending")
    public List<ProductAiPaidDeliveryDtos.PendingResponse> pending() {
        return service.pending();
    }

    /** Registra request bruto e prompt antes da chamada externa. */
    @PostMapping("/{idJob}/recebeRequest")
    public ResponseEntity<Void> recebeRequest(
            @PathVariable String idJob,
            @RequestBody ProductAiPaidDeliveryDtos.ReceiveRequestRequest request) {
        service.receiveRequest(idJob, request);
        return ResponseEntity.noContent().build();
    }

    /** Registra resposta, custo e artefato gerado pelo worker. */
    @PostMapping("/{idJob}/recebeResponse")
    public ResponseEntity<Void> recebeResponse(
            @PathVariable String idJob,
            @RequestBody ProductAiPaidDeliveryDtos.ReceiveResponseRequest request) {
        service.receiveResponse(idJob, request);
        return ResponseEntity.noContent().build();
    }
}
