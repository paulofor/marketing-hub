package com.marketinghub.payments.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "lead_portal_premium_delivery")
public class LeadPortalPremiumDelivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "purchase_id", nullable = false, unique = true)
    private Long purchaseId;

    @Column(name = "package_id", nullable = false)
    private Long packageId;

    @Column(name = "submission_id")
    private String submissionId;

    @Column(name = "submission_name")
    private String submissionName;

    @Column(name = "submission_email")
    private String submissionEmail;

    @Column(name = "buyer_name")
    private String buyerName;

    @Column(name = "buyer_email")
    private String buyerEmail;

    @Column(name = "recipient_name")
    private String recipientName;

    @Column(name = "recipient_email", nullable = false)
    private String recipientEmail;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private PremiumDeliveryStatus status = PremiumDeliveryStatus.PENDING_ZIP;

    @Column(name = "zip_object_key")
    private String zipObjectKey;

    @Column(name = "zip_download_url")
    private String zipDownloadUrl;

    @Column(name = "zip_size_bytes")
    private Long zipSizeBytes;

    @Column(name = "zip_generated_at")
    private Instant zipGeneratedAt;

    @Column(name = "zip_attempts", nullable = false)
    private Integer zipAttempts = 0;

    @Column(name = "zip_last_attempt")
    private Instant zipLastAttempt;

    @Column(name = "zip_last_error", columnDefinition = "TEXT")
    private String zipLastError;

    @Column(name = "email_request_id")
    private String emailRequestId;

    @Column(name = "email_sent_at")
    private Instant emailSentAt;

    @Column(name = "email_attempts", nullable = false)
    private Integer emailAttempts = 0;

    @Column(name = "email_last_attempt")
    private Instant emailLastAttempt;

    @Column(name = "email_last_error", columnDefinition = "TEXT")
    private String emailLastError;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private Instant updatedAt;

    public Long getId() {
        return id;
    }

    public Long getPurchaseId() {
        return purchaseId;
    }

    public void setPurchaseId(Long purchaseId) {
        this.purchaseId = purchaseId;
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

    public String getSubmissionName() {
        return submissionName;
    }

    public void setSubmissionName(String submissionName) {
        this.submissionName = submissionName;
    }

    public String getSubmissionEmail() {
        return submissionEmail;
    }

    public void setSubmissionEmail(String submissionEmail) {
        this.submissionEmail = submissionEmail;
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

    public String getRecipientName() {
        return recipientName;
    }

    public void setRecipientName(String recipientName) {
        this.recipientName = recipientName;
    }

    public String getRecipientEmail() {
        return recipientEmail;
    }

    public void setRecipientEmail(String recipientEmail) {
        this.recipientEmail = recipientEmail;
    }

    public PremiumDeliveryStatus getStatus() {
        return status;
    }

    public void setStatus(PremiumDeliveryStatus status) {
        this.status = status;
    }

    public String getZipObjectKey() {
        return zipObjectKey;
    }

    public void setZipObjectKey(String zipObjectKey) {
        this.zipObjectKey = zipObjectKey;
    }

    public String getZipDownloadUrl() {
        return zipDownloadUrl;
    }

    public void setZipDownloadUrl(String zipDownloadUrl) {
        this.zipDownloadUrl = zipDownloadUrl;
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

    public Integer getZipAttempts() {
        return zipAttempts;
    }

    public void setZipAttempts(Integer zipAttempts) {
        this.zipAttempts = zipAttempts;
    }

    public Instant getZipLastAttempt() {
        return zipLastAttempt;
    }

    public void setZipLastAttempt(Instant zipLastAttempt) {
        this.zipLastAttempt = zipLastAttempt;
    }

    public String getZipLastError() {
        return zipLastError;
    }

    public void setZipLastError(String zipLastError) {
        this.zipLastError = zipLastError;
    }

    public String getEmailRequestId() {
        return emailRequestId;
    }

    public void setEmailRequestId(String emailRequestId) {
        this.emailRequestId = emailRequestId;
    }

    public Instant getEmailSentAt() {
        return emailSentAt;
    }

    public void setEmailSentAt(Instant emailSentAt) {
        this.emailSentAt = emailSentAt;
    }

    public Integer getEmailAttempts() {
        return emailAttempts;
    }

    public void setEmailAttempts(Integer emailAttempts) {
        this.emailAttempts = emailAttempts;
    }

    public Instant getEmailLastAttempt() {
        return emailLastAttempt;
    }

    public void setEmailLastAttempt(Instant emailLastAttempt) {
        this.emailLastAttempt = emailLastAttempt;
    }

    public String getEmailLastError() {
        return emailLastError;
    }

    public void setEmailLastError(String emailLastError) {
        this.emailLastError = emailLastError;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void resetZipState() {
        this.zipObjectKey = null;
        this.zipDownloadUrl = null;
        this.zipSizeBytes = null;
        this.zipGeneratedAt = null;
        this.zipLastError = null;
        this.zipAttempts = 0;
        this.zipLastAttempt = null;
    }

    public void resetEmailState() {
        this.emailRequestId = null;
        this.emailSentAt = null;
        this.emailLastError = null;
        this.emailAttempts = 0;
        this.emailLastAttempt = null;
    }
}
