package com.marketinghub.payments.controller;

import com.marketinghub.payments.dto.DigitalProductDeliveryEmailRetryRequest;
import com.marketinghub.payments.dto.DigitalProductDeliveryEmailRetryResponse;
import com.marketinghub.payments.integration.mercadopago.MercadoPagoPaymentDetails;
import com.marketinghub.payments.model.DigitalProductDeliveryEmail;
import com.marketinghub.payments.service.CheckoutService;
import com.marketinghub.payments.service.DigitalProductPostPurchaseEmailService;
import jakarta.validation.Valid;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Expõe ações de recuperação de entrega para produtos digitais vendidos via checkout direto.
 */
@RestController
@RequestMapping("/api/v1/digital-product-deliveries")
public class DigitalProductDeliveryController {

    private static final Logger log = LoggerFactory.getLogger(DigitalProductDeliveryController.class);

    private final CheckoutService checkoutService;
    private final DigitalProductPostPurchaseEmailService emailService;

    public DigitalProductDeliveryController(CheckoutService checkoutService,
                                            DigitalProductPostPurchaseEmailService emailService) {
        this.checkoutService = checkoutService;
        this.emailService = emailService;
    }

    /** Valida o pagamento e envia a entrega digital para o email informado. */
    @PostMapping("/exp51/email")
    public ResponseEntity<DigitalProductDeliveryEmailRetryResponse> sendExperiment51Email(
            @Valid @RequestBody DigitalProductDeliveryEmailRetryRequest request) {
        try {
            Optional<MercadoPagoPaymentDetails> paymentDetails = checkoutService.fetchPayment(request.paymentId());
            if (paymentDetails.isEmpty()) {
                return ResponseEntity.badRequest().body(new DigitalProductDeliveryEmailRetryResponse(
                        "PAYMENT_NOT_FOUND",
                        "Pagamento não encontrado."));
            }
            DigitalProductDeliveryEmail delivery = emailService.sendToRecipient(
                    paymentDetails.get(),
                    request.email(),
                    request.name());
            return ResponseEntity.ok(new DigitalProductDeliveryEmailRetryResponse(
                    delivery.getStatus().name(),
                    delivery.getStatus().name().equals("SENT")
                            ? "Email enviado com sucesso."
                            : "Não foi possível enviar o email agora."));
        } catch (Exception ex) {
            log.error("Falha ao reenviar entrega digital do experimento 51 (paymentId={})",
                    request.paymentId(), ex);
            return ResponseEntity.badRequest().body(new DigitalProductDeliveryEmailRetryResponse(
                    "FAILED",
                    ex.getMessage()));
        }
    }
}
