package com.marketinghub.payments.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.payments.dto.MercadoPagoWebhookPayload;
import com.marketinghub.payments.integration.mercadopago.MercadoPagoPaymentDetails;
import com.marketinghub.payments.service.CheckoutService;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/mercadopago")
public class MercadoPagoWebhookController {

    private static final Logger log = LoggerFactory.getLogger(MercadoPagoWebhookController.class);

    private final CheckoutService checkoutService;
    private final ObjectMapper objectMapper;

    public MercadoPagoWebhookController(CheckoutService checkoutService, ObjectMapper objectMapper) {
        this.checkoutService = checkoutService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> handleWebhook(@RequestBody(required = false) MercadoPagoWebhookPayload payload,
                                              @RequestParam(name = "id", required = false) String idFromQuery,
                                              @RequestParam(name = "topic", required = false) String topic) {
        String resourceId = payload != null ? payload.extractResourceId() : null;
        if (!StringUtils.hasText(resourceId)) {
            resourceId = idFromQuery;
        }
        if (!StringUtils.hasText(resourceId)) {
            log.warn("Webhook do Mercado Pago recebido sem id (topic={})", topic);
            return ResponseEntity.badRequest().build();
        }
        try {
            Optional<MercadoPagoPaymentDetails> paymentDetails = checkoutService.fetchPayment(resourceId);
            if (paymentDetails.isEmpty()) {
                log.warn("Pagamento {} não encontrado no Mercado Pago. Webhook ACK sem alterações.", resourceId);
                return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
            }
            String rawPayload = payload != null ? serialize(payload) : null;
            checkoutService.updateFromPayment(paymentDetails.get(), rawPayload);
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        } catch (Exception ex) {
            log.error("Erro ao processar webhook do Mercado Pago (id={})", resourceId, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private String serialize(MercadoPagoWebhookPayload payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            return null;
        }
    }
}
