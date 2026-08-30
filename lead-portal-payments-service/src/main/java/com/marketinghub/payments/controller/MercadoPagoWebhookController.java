package com.marketinghub.payments.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.payments.dto.MercadoPagoWebhookPayload;
import com.marketinghub.payments.integration.mercadopago.MercadoPagoPaymentDetails;
import com.marketinghub.payments.model.MercadoPagoWebhookLog;
import com.marketinghub.payments.model.WebhookProcessingStatus;
import com.marketinghub.payments.repository.MercadoPagoWebhookLogRepository;
import com.marketinghub.payments.service.CheckoutService;
import com.marketinghub.payments.service.PaymentAuditPayloadSanitizer;
import com.marketinghub.payments.service.PdePaymentEntitlementClient;
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

/** Recebe o webhook do Mercado Pago, audita o payload e aplica seus efeitos idempotentes. */
@RestController
@RequestMapping("/api/v1/mercadopago")
public class MercadoPagoWebhookController {

    private static final Logger log = LoggerFactory.getLogger(MercadoPagoWebhookController.class);

    private final CheckoutService checkoutService;
    private final ObjectMapper objectMapper;
    private final MercadoPagoWebhookLogRepository webhookLogRepository;
    private final PdePaymentEntitlementClient pdePaymentEntitlementClient;
    private final PaymentAuditPayloadSanitizer paymentAuditPayloadSanitizer;

    /** Configura consulta, auditoria e publicação do estado financeiro confirmado. */
    public MercadoPagoWebhookController(CheckoutService checkoutService,
                                        ObjectMapper objectMapper,
                                        MercadoPagoWebhookLogRepository webhookLogRepository,
                                        PdePaymentEntitlementClient pdePaymentEntitlementClient,
                                        PaymentAuditPayloadSanitizer paymentAuditPayloadSanitizer) {
        this.checkoutService = checkoutService;
        this.objectMapper = objectMapper;
        this.webhookLogRepository = webhookLogRepository;
        this.pdePaymentEntitlementClient = pdePaymentEntitlementClient;
        this.paymentAuditPayloadSanitizer = paymentAuditPayloadSanitizer;
    }

    /** Consulta novamente o pagamento no provedor antes de aplicar entrega e entitlement. */
    @PostMapping("/webhook")
    public ResponseEntity<Void> handleWebhook(@RequestBody(required = false) MercadoPagoWebhookPayload payload,
                                              @RequestParam(name = "id", required = false) String idFromQuery,
                                              @RequestParam(name = "topic", required = false) String topic) {
        MercadoPagoWebhookLog auditLog = new MercadoPagoWebhookLog();
        auditLog.setQueryId(idFromQuery);
        auditLog.setQueryTopic(topic);
        auditLog.setPayloadAction(payload != null ? payload.getAction() : null);
        auditLog.setPayloadType(payload != null ? payload.getType() : null);
        auditLog.setHasPayload(payload != null);

        String rawPayload = serialize(payload);
        String minimizedPayload = paymentAuditPayloadSanitizer.minimize(rawPayload);
        auditLog.setPayload(minimizedPayload);

        String resourceId = payload != null ? payload.extractResourceId() : null;
        if (!StringUtils.hasText(resourceId)) {
            resourceId = idFromQuery;
        }
        auditLog.setResourceId(resourceId);
        auditLog.setTopic(topic);

        try {
            if (!StringUtils.hasText(resourceId)) {
                log.warn("Webhook do Mercado Pago recebido sem id (topic={})", topic);
                auditLog.setProcessingStatus(WebhookProcessingStatus.INVALID_REQUEST);
                auditLog.setErrorMessage("Webhook recebido sem id");
                return ResponseEntity.badRequest().build();
            }
            log.info("Webhook do Mercado Pago recebido (id={}, topic={}, hasPayload={})", resourceId, topic, payload != null);
            Optional<MercadoPagoPaymentDetails> paymentDetails = checkoutService.fetchPayment(resourceId);
            if (paymentDetails.isEmpty()) {
                log.warn("Pagamento {} não encontrado no Mercado Pago. Webhook ACK sem alterações.", resourceId);
                auditLog.setProcessingStatus(WebhookProcessingStatus.PAYMENT_NOT_FOUND);
                return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
            }
            MercadoPagoPaymentDetails details = paymentDetails.get();
            checkoutService.updateFromPayment(details, minimizedPayload);
            pdePaymentEntitlementClient.notifyIfSupported(details);
            auditLog.setResourceId(details.id());
            auditLog.setMercadoPagoStatus(details.status());
            auditLog.setMercadoPagoResponse(paymentAuditPayloadSanitizer.minimize(details.rawPayload()));
            auditLog.setProcessingStatus(WebhookProcessingStatus.PROCESSED);
            log.info("Webhook do Mercado Pago processado com sucesso (id={}, status={})", resourceId,
                    details.status());
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        } catch (Exception ex) {
            log.error("Erro ao processar webhook do Mercado Pago (id={})", resourceId, ex);
            auditLog.setProcessingStatus(WebhookProcessingStatus.ERROR);
            auditLog.setErrorMessage(ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } finally {
            try {
                webhookLogRepository.save(auditLog);
            } catch (Exception saveEx) {
                log.error("Falha ao registrar log do webhook do Mercado Pago", saveEx);
            }
        }
    }

    /** Serializa o payload recebido antes de aplicar a minimização da auditoria persistida. */
    private String serialize(MercadoPagoWebhookPayload payload) {
        if (payload == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            log.error("Falha ao serializar payload bruto do webhook do Mercado Pago", ex);
            return null;
        }
    }
}
