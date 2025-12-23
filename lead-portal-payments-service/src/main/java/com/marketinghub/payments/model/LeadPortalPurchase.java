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
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "lead_portal_purchase")
public class LeadPortalPurchase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "package_id", nullable = false)
    private Long packageId;

    @Column(name = "submission_id")
    private String submissionId;

    @Column(name = "buyer_name")
    private String buyerName;

    @Column(name = "buyer_email")
    private String buyerEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PurchaseStatus status = PurchaseStatus.PREFERENCE_CREATED;

    @Column(name = "mp_preference_id")
    private String mercadoPagoPreferenceId;

    @Column(name = "mp_payment_id")
    private String mercadoPagoPaymentId;

    @Column(name = "mp_status")
    private String mercadoPagoStatus;

    @Column(name = "checkout_url", length = 1200)
    private String checkoutUrl;

    @Column(name = "checkout_expires_at")
    private Instant checkoutExpiresAt;

    @Column(name = "amount")
    private BigDecimal amount;

    @Column(name = "currency")
    private String currency;

    @Column(name = "notification_payload", columnDefinition = "LONGTEXT")
    private String notificationPayload;

    @Column(name = "delivery_attempts")
    private Integer deliveryAttempts = 0;

    @Column(name = "delivery_error", columnDefinition = "LONGTEXT")
    private String deliveryError;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "payment_approved_at")
    private Instant paymentApprovedAt;

    @Column(name = "zip_object_key")
    private String zipObjectKey;

    @Column(name = "zip_size_bytes")
    private Long zipSizeBytes;

    @Column(name = "zip_generated_at")
    private Instant zipGeneratedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPackageId() {
        return packageId;
    }

    public void setPackageId(Long packageId) {
        this.packageId = packageId;
    }

    public String getSubmissionId() {
        return submissionId;
    }

    public void setSubmissionId(String submissionId) {
        this.submissionId = submissionId;
    }

    public String getBuyerName() {
        return buyerName;
    }

    public void setBuyerName(String buyerName) {
        this.buyerName = buyerName;
    }

    public String getBuyerEmail() {
        return buyerEmail;
    }

    public void setBuyerEmail(String buyerEmail) {
        this.buyerEmail = buyerEmail;
    }

    public PurchaseStatus getStatus() {
        return status;
    }

    public void setStatus(PurchaseStatus status) {
        this.status = status;
    }

    public String getMercadoPagoPreferenceId() {
        return mercadoPagoPreferenceId;
    }

    public void setMercadoPagoPreferenceId(String mercadoPagoPreferenceId) {
        this.mercadoPagoPreferenceId = mercadoPagoPreferenceId;
    }

    public String getMercadoPagoPaymentId() {
        return mercadoPagoPaymentId;
    }

    public void setMercadoPagoPaymentId(String mercadoPagoPaymentId) {
        this.mercadoPagoPaymentId = mercadoPagoPaymentId;
    }

    public String getMercadoPagoStatus() {
        return mercadoPagoStatus;
    }

    public void setMercadoPagoStatus(String mercadoPagoStatus) {
        this.mercadoPagoStatus = mercadoPagoStatus;
    }

    public String getCheckoutUrl() {
        return checkoutUrl;
    }

    public void setCheckoutUrl(String checkoutUrl) {
        this.checkoutUrl = checkoutUrl;
    }

    public Instant getCheckoutExpiresAt() {
        return checkoutExpiresAt;
    }

    public void setCheckoutExpiresAt(Instant checkoutExpiresAt) {
        this.checkoutExpiresAt = checkoutExpiresAt;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getNotificationPayload() {
        return notificationPayload;
    }

    public void setNotificationPayload(String notificationPayload) {
        this.notificationPayload = notificationPayload;
    }

    public Integer getDeliveryAttempts() {
        return deliveryAttempts;
    }

    public void setDeliveryAttempts(Integer deliveryAttempts) {
        this.deliveryAttempts = deliveryAttempts;
    }

    public String getDeliveryError() {
        return deliveryError;
    }

    public void setDeliveryError(String deliveryError) {
        this.deliveryError = deliveryError;
    }

    public Instant getDeliveredAt() {
        return deliveredAt;
    }

    public void setDeliveredAt(Instant deliveredAt) {
        this.deliveredAt = deliveredAt;
    }

    public Instant getPaymentApprovedAt() {
        return paymentApprovedAt;
    }

    public void setPaymentApprovedAt(Instant paymentApprovedAt) {
        this.paymentApprovedAt = paymentApprovedAt;
    }

    public String getZipObjectKey() {
        return zipObjectKey;
    }

    public void setZipObjectKey(String zipObjectKey) {
        this.zipObjectKey = zipObjectKey;
    }

    public Long getZipSizeBytes() {
        return zipSizeBytes;
    }

    public void setZipSizeBytes(Long zipSizeBytes) {
        this.zipSizeBytes = zipSizeBytes;
    }

    public Instant getZipGeneratedAt() {
        return zipGeneratedAt;
    }

    public void setZipGeneratedAt(Instant zipGeneratedAt) {
        this.zipGeneratedAt = zipGeneratedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
