package com.marketinghub.payments.service;

import com.marketinghub.payments.config.DigitalProductEmailDeliveryProperties;
import com.marketinghub.payments.integration.email.DigitalProductDeliveryEmailClient;
import com.marketinghub.payments.integration.email.DigitalProductDeliveryEmailRequest;
import com.marketinghub.payments.integration.email.DigitalProductDeliveryEmailResponse;
import com.marketinghub.payments.integration.mercadopago.MercadoPagoPaymentDetails;
import com.marketinghub.payments.model.DigitalProductDeliveryEmail;
import com.marketinghub.payments.model.DigitalProductDeliveryEmailStatus;
import com.marketinghub.payments.repository.DigitalProductDeliveryEmailRepository;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Dispara email pós-compra para produtos digitais vendidos fora do fluxo de pacote do Lead Portal.
 */
@Service
public class DigitalProductPostPurchaseEmailService {

    private static final Logger log = LoggerFactory.getLogger(DigitalProductPostPurchaseEmailService.class);

    private final DigitalProductEmailDeliveryProperties properties;
    private final DigitalProductDeliveryEmailRepository repository;
    private final DigitalProductDeliveryEmailClient emailClient;

    public DigitalProductPostPurchaseEmailService(DigitalProductEmailDeliveryProperties properties,
                                                  DigitalProductDeliveryEmailRepository repository,
                                                  DigitalProductDeliveryEmailClient emailClient) {
        this.properties = properties;
        this.repository = repository;
        this.emailClient = emailClient;
    }

    /** Envia o email de entrega se o pagamento aprovado pertencer a produto digital suportado. */
    @Transactional
    public void sendIfSupported(MercadoPagoPaymentDetails paymentDetails) {
        if (!properties.isEnabled()) {
            log.info("Email de entrega digital desabilitado. Ignorando pagamento {}", paymentDetails.id());
            return;
        }
        if (!"approved".equalsIgnoreCase(paymentDetails.status())) {
            log.info("Pagamento {} ainda não aprovado para envio de entrega digital (status={})",
                    paymentDetails.id(), paymentDetails.status());
            return;
        }
        Optional<DigitalProductConfig> productConfig = supportedProduct(paymentDetails.externalReference());
        if (productConfig.isEmpty()) {
            log.info("Pagamento {} não pertence a produto digital suportado (externalReference={})",
                    paymentDetails.id(), paymentDetails.externalReference());
            return;
        }

        DigitalProductDeliveryEmail delivery = repository.findByPaymentId(paymentDetails.id())
                .orElseGet(() -> createPendingDelivery(paymentDetails, productConfig.get()));
        send(delivery, paymentDetails);
    }

    /** Reenvia a entrega para um email informado pelo comprador após validar o pagamento. */
    @Transactional
    public DigitalProductDeliveryEmail sendToRecipient(MercadoPagoPaymentDetails paymentDetails,
                                                       String recipientEmail,
                                                       String recipientName) {
        if (!properties.isEnabled()) {
            throw new IllegalStateException("Email de entrega digital desabilitado");
        }
        if (!"approved".equalsIgnoreCase(paymentDetails.status())) {
            throw new IllegalStateException("Pagamento " + paymentDetails.id() + " ainda não aprovado");
        }
        DigitalProductConfig productConfig = supportedProduct(paymentDetails.externalReference())
                .orElseThrow(() -> new IllegalStateException(
                        "Pagamento " + paymentDetails.id() + " não pertence a produto digital suportado"));
        if (!StringUtils.hasText(recipientEmail)) {
            throw new IllegalArgumentException("Email do comprador é obrigatório");
        }
        DigitalProductDeliveryEmail delivery = repository.findByPaymentId(paymentDetails.id())
                .orElseGet(() -> createPendingDelivery(paymentDetails, recipientEmail, recipientName, productConfig));
        delivery.setRecipientEmail(recipientEmail.trim());
        if (StringUtils.hasText(recipientName)) {
            delivery.setRecipientName(recipientName.trim());
        }
        if (!StringUtils.hasText(delivery.getDeliveryPageUrl())) {
            delivery.setDeliveryPageUrl(productConfig.deliveryPageUrl());
        }
        if (!StringUtils.hasText(delivery.getDownloadUrl())) {
            delivery.setDownloadUrl(resolveDownloadUrl(paymentDetails.metadata(), productConfig));
        }
        delivery.setStatus(DigitalProductDeliveryEmailStatus.PENDING);
        delivery.setLastError(null);
        repository.save(delivery);
        send(delivery, paymentDetails);
        return delivery;
    }

    /** Cria o registro pendente a partir dos dados do pagamento aprovado. */
    private DigitalProductDeliveryEmail createPendingDelivery(MercadoPagoPaymentDetails paymentDetails,
                                                              DigitalProductConfig productConfig) {
        String recipientEmail = paymentDetails.email();
        if (!StringUtils.hasText(recipientEmail)) {
            throw new IllegalStateException("Pagamento " + paymentDetails.id() + " não possui email do comprador");
        }
        DigitalProductDeliveryEmail delivery = new DigitalProductDeliveryEmail();
        delivery.setPaymentId(paymentDetails.id());
        delivery.setExternalReference(paymentDetails.externalReference());
        delivery.setRecipientEmail(recipientEmail);
        delivery.setRecipientName(resolveBuyerName(paymentDetails.metadata()));
        delivery.setProductName(resolveProductName(paymentDetails, productConfig));
        delivery.setDeliveryPageUrl(productConfig.deliveryPageUrl());
        delivery.setDownloadUrl(resolveDownloadUrl(paymentDetails.metadata(), productConfig));
        delivery.setStatus(DigitalProductDeliveryEmailStatus.PENDING);
        return repository.save(delivery);
    }

    /** Cria registro pendente usando email informado manualmente pelo comprador. */
    private DigitalProductDeliveryEmail createPendingDelivery(MercadoPagoPaymentDetails paymentDetails,
                                                              String recipientEmail,
                                                              String recipientName,
                                                              DigitalProductConfig productConfig) {
        DigitalProductDeliveryEmail delivery = new DigitalProductDeliveryEmail();
        delivery.setPaymentId(paymentDetails.id());
        delivery.setExternalReference(paymentDetails.externalReference());
        delivery.setRecipientEmail(recipientEmail.trim());
        delivery.setRecipientName(StringUtils.hasText(recipientName) ? recipientName.trim() : null);
        delivery.setProductName(resolveProductName(paymentDetails, productConfig));
        delivery.setDeliveryPageUrl(productConfig.deliveryPageUrl());
        delivery.setDownloadUrl(resolveDownloadUrl(paymentDetails.metadata(), productConfig));
        delivery.setStatus(DigitalProductDeliveryEmailStatus.PENDING);
        return repository.save(delivery);
    }

    /** Executa o envio e atualiza o registro com sucesso ou falha. */
    private void send(DigitalProductDeliveryEmail delivery, MercadoPagoPaymentDetails paymentDetails) {
        if (delivery.getStatus() == DigitalProductDeliveryEmailStatus.SENT) {
            log.info("Email de entrega digital já enviado para pagamento {} (requestId={})",
                    paymentDetails.id(), delivery.getEmailRequestId());
            return;
        }

        delivery.setAttempts((delivery.getAttempts() == null ? 0 : delivery.getAttempts()) + 1);
        repository.save(delivery);

        try {
            DigitalProductDeliveryEmailResponse response = emailClient.send(new DigitalProductDeliveryEmailRequest(
                    delivery.getRecipientEmail(),
                    delivery.getRecipientName(),
                    delivery.getProductName(),
                    withPaymentId(delivery.getDeliveryPageUrl(), paymentDetails.id()),
                    delivery.getDownloadUrl(),
                    paymentDetails.id(),
                    delivery.getExternalReference()));
            delivery.setStatus(DigitalProductDeliveryEmailStatus.SENT);
            delivery.setEmailRequestId(response != null ? response.requestId() : null);
            delivery.setSentAt(Instant.now());
            delivery.setLastError(null);
            repository.save(delivery);
            log.info("Email de entrega digital enviado para pagamento {} (requestId={})",
                    paymentDetails.id(), delivery.getEmailRequestId());
        } catch (Exception ex) {
            log.error("Falha ao enviar email de entrega digital (paymentId={}, externalReference={})",
                    paymentDetails.id(), paymentDetails.externalReference(), ex);
            delivery.setStatus(DigitalProductDeliveryEmailStatus.FAILED);
            delivery.setLastError(ex.getMessage());
            repository.save(delivery);
        }
    }

    /** Resolve qual produto digital direto corresponde à referência do Mercado Pago. */
    private Optional<DigitalProductConfig> supportedProduct(String externalReference) {
        if (!StringUtils.hasText(externalReference)) {
            return Optional.empty();
        }
        String normalized = externalReference.trim().toLowerCase(Locale.ROOT);
        if (normalized.equals(normalize(properties.getExperiment51Reference()))) {
            return Optional.of(new DigitalProductConfig(
                    properties.getExperiment51Reference(),
                    properties.getExperiment51ProductName(),
                    properties.getExperiment51DeliveryPageUrl(),
                    properties.getExperiment51DownloadUrl()));
        }
        if (normalized.equals(normalize(properties.getExperiment66Reference()))) {
            return Optional.of(new DigitalProductConfig(
                    properties.getExperiment66Reference(),
                    properties.getExperiment66ProductName(),
                    properties.getExperiment66DeliveryPageUrl(),
                    properties.getExperiment66DownloadUrl()));
        }
        if (normalized.equals(normalize(properties.getAgendaCheiaReference()))) {
            return Optional.of(new DigitalProductConfig(
                    properties.getAgendaCheiaReference(),
                    properties.getAgendaCheiaProductName(),
                    properties.getAgendaCheiaDeliveryPageUrl(),
                    null));
        }
        return Optional.empty();
    }

    /** Normaliza referência configurada para comparação estável. */
    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim().toLowerCase(Locale.ROOT) : "";
    }

    /** Resolve o nome do comprador quando veio nos metadados. */
    private String resolveBuyerName(Map<String, Object> metadata) {
        return asString(metadata, "buyer_name", "buyerName", "name");
    }

    /** Resolve o nome do produto priorizando descrição real do pagamento. */
    private String resolveProductName(MercadoPagoPaymentDetails paymentDetails, DigitalProductConfig productConfig) {
        if (StringUtils.hasText(paymentDetails.description())) {
            return paymentDetails.description();
        }
        String metadataProductName = asString(paymentDetails.metadata(), "product_name", "productName");
        if (StringUtils.hasText(metadataProductName)) {
            return metadataProductName;
        }
        return productConfig.productName();
    }

    /** Resolve a URL de download configurada nos metadados ou no fallback do experimento. */
    private String resolveDownloadUrl(Map<String, Object> metadata, DigitalProductConfig productConfig) {
        String metadataDeliveryUrl = asString(metadata, "delivery_url", "deliveryUrl", "download_url", "downloadUrl");
        return StringUtils.hasText(metadataDeliveryUrl)
                ? metadataDeliveryUrl
                : productConfig.downloadUrl();
    }

    /** Acrescenta o payment_id à página de entrega para consulta posterior do pagamento. */
    private String withPaymentId(String url, String paymentId) {
        if (!StringUtils.hasText(url) || !StringUtils.hasText(paymentId)) {
            return url;
        }
        String separator = url.contains("?") ? "&" : "?";
        return url + separator + "payment_id=" + paymentId + "&status=approved";
    }

    /** Extrai texto de metadados aceitando variações de chave. */
    private String asString(Map<String, Object> metadata, String... keys) {
        if (metadata == null || keys == null) {
            return null;
        }
        for (String key : keys) {
            Object value = metadata.get(key);
            if (value instanceof String str && StringUtils.hasText(str)) {
                return str;
            }
            if (value instanceof Number number) {
                return number.toString();
            }
        }
        return null;
    }

    /** Guarda os dados de entrega de cada produto digital vendido por checkout direto. */
    private record DigitalProductConfig(
            String reference,
            String productName,
            String deliveryPageUrl,
            String downloadUrl) {}
}
