package com.marketinghub.payments.model;

public enum WebhookProcessingStatus {
    RECEIVED,
    INVALID_REQUEST,
    PAYMENT_NOT_FOUND,
    PROCESSED,
    ERROR
}
