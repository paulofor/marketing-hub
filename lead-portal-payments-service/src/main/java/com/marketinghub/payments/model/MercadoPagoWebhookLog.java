package com.marketinghub.payments.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "mercadopago_webhook_log")
public class MercadoPagoWebhookLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "resource_id")
    private String resourceId;

    @Column(name = "topic")
    private String topic;

    @Column(name = "query_id")
    private String queryId;

    @Column(name = "query_topic")
    private String queryTopic;

    @Column(name = "payload_type")
    private String payloadType;

    @Column(name = "payload_action")
    private String payloadAction;

    @Column(name = "has_payload")
    private Boolean hasPayload;

    @Column(name = "payload", columnDefinition = "LONGTEXT")
    private String payload;

    @Column(name = "mercadopago_status")
    private String mercadoPagoStatus;

    @Column(name = "mercadopago_response", columnDefinition = "LONGTEXT")
    private String mercadoPagoResponse;

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", nullable = false)
    private WebhookProcessingStatus processingStatus = WebhookProcessingStatus.RECEIVED;

    @Column(name = "error_message", columnDefinition = "LONGTEXT")
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    public void onCreate() {
        createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getQueryId() {
        return queryId;
    }

    public void setQueryId(String queryId) {
        this.queryId = queryId;
    }

    public String getQueryTopic() {
        return queryTopic;
    }

    public void setQueryTopic(String queryTopic) {
        this.queryTopic = queryTopic;
    }

    public String getPayloadType() {
        return payloadType;
    }

    public void setPayloadType(String payloadType) {
        this.payloadType = payloadType;
    }

    public String getPayloadAction() {
        return payloadAction;
    }

    public void setPayloadAction(String payloadAction) {
        this.payloadAction = payloadAction;
    }

    public Boolean getHasPayload() {
        return hasPayload;
    }

    public void setHasPayload(Boolean hasPayload) {
        this.hasPayload = hasPayload;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public String getMercadoPagoStatus() {
        return mercadoPagoStatus;
    }

    public void setMercadoPagoStatus(String mercadoPagoStatus) {
        this.mercadoPagoStatus = mercadoPagoStatus;
    }

    public String getMercadoPagoResponse() {
        return mercadoPagoResponse;
    }

    public void setMercadoPagoResponse(String mercadoPagoResponse) {
        this.mercadoPagoResponse = mercadoPagoResponse;
    }

    public WebhookProcessingStatus getProcessingStatus() {
        return processingStatus;
    }

    public void setProcessingStatus(WebhookProcessingStatus processingStatus) {
        this.processingStatus = processingStatus;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
