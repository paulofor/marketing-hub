package com.marketinghub.salesvideo;

/**
 * Eventos registrados para auditoria dos jobs de vídeo.
 */
public enum SalesVideoJobEventType {
    CREATED,
    CLAIMED,
    HEARTBEAT,
    PROGRESS,
    STATUS_CHANGED,
    COMPLETED,
    FAILED,
    EXPIRED,
    RETRIED
}
