package com.marketinghub.payments.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Registra a tentativa idempotente de envio do email de entrega de produto digital.
 */
@Entity
@Table(name = "digital_product_delivery_email")
public class DigitalProductDeliveryEmail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payment_id", nullable = false, unique = true)
    private String paymentId;

    @Column(name = "external_reference", nullable = false)
    private String externalReference;

    @Column(name = "recipient_email", nullable = false)
    private String recipientEmail;

    @Column(name = "recipient_name")
    private String recipientName;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "delivery_page_url", length = 1200)
    private String deliveryPageUrl;

    @Column(name = "download_url", length = 1200)
    private String downloadUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DigitalProductDeliveryEmailStatus status = DigitalProductDeliveryEmailStatus.PENDING;

    @Column(name = "email_request_id")
    private String emailRequestId;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "attempts", nullable = false)
    private Integer attempts = 0;

    @Column(name = "last_error", columnDefinition = "LONGTEXT")
    private String lastError;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Inicializa os carimbos de data no primeiro registro. */
    @PrePersist
    public void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    /** Atualiza o carimbo de alteração antes de persistir mudanças. */
    @PreUpdate
    public void onUpdate() {
        updatedAt = Instant.now();
    }

    /** Retorna o identificador interno do registro. */
    public Long getId() {
        return id;
    }

    /** Retorna o identificador do pagamento no Mercado Pago. */
    public String getPaymentId() {
        return paymentId;
    }

    /** Define o identificador do pagamento no Mercado Pago. */
    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }

    /** Retorna a referência externa usada para identificar o produto vendido. */
    public String getExternalReference() {
        return externalReference;
    }

    /** Define a referência externa usada para identificar o produto vendido. */
    public void setExternalReference(String externalReference) {
        this.externalReference = externalReference;
    }

    /** Retorna o email do comprador que receberá a entrega. */
    public String getRecipientEmail() {
        return recipientEmail;
    }

    /** Define o email do comprador que receberá a entrega. */
    public void setRecipientEmail(String recipientEmail) {
        this.recipientEmail = recipientEmail;
    }

    /** Retorna o nome do comprador quando disponível. */
    public String getRecipientName() {
        return recipientName;
    }

    /** Define o nome do comprador quando disponível. */
    public void setRecipientName(String recipientName) {
        this.recipientName = recipientName;
    }

    /** Retorna o nome comercial do produto comprado. */
    public String getProductName() {
        return productName;
    }

    /** Define o nome comercial do produto comprado. */
    public void setProductName(String productName) {
        this.productName = productName;
    }

    /** Retorna a página de entrega enviada no email. */
    public String getDeliveryPageUrl() {
        return deliveryPageUrl;
    }

    /** Define a página de entrega enviada no email. */
    public void setDeliveryPageUrl(String deliveryPageUrl) {
        this.deliveryPageUrl = deliveryPageUrl;
    }

    /** Retorna a URL direta de download como alternativa de entrega. */
    public String getDownloadUrl() {
        return downloadUrl;
    }

    /** Define a URL direta de download como alternativa de entrega. */
    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    /** Retorna o status do envio. */
    public DigitalProductDeliveryEmailStatus getStatus() {
        return status;
    }

    /** Define o status do envio. */
    public void setStatus(DigitalProductDeliveryEmailStatus status) {
        this.status = status;
    }

    /** Retorna o requestId gerado pelo email-service. */
    public String getEmailRequestId() {
        return emailRequestId;
    }

    /** Define o requestId gerado pelo email-service. */
    public void setEmailRequestId(String emailRequestId) {
        this.emailRequestId = emailRequestId;
    }

    /** Retorna o momento em que o email foi enviado. */
    public Instant getSentAt() {
        return sentAt;
    }

    /** Define o momento em que o email foi enviado. */
    public void setSentAt(Instant sentAt) {
        this.sentAt = sentAt;
    }

    /** Retorna quantas tentativas de envio foram feitas. */
    public Integer getAttempts() {
        return attempts;
    }

    /** Define quantas tentativas de envio foram feitas. */
    public void setAttempts(Integer attempts) {
        this.attempts = attempts;
    }

    /** Retorna o último erro de envio. */
    public String getLastError() {
        return lastError;
    }

    /** Define o último erro de envio. */
    public void setLastError(String lastError) {
        this.lastError = lastError;
    }
}
