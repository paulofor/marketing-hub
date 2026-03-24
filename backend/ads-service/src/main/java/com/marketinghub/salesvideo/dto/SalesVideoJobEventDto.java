package com.marketinghub.salesvideo.dto;

import com.marketinghub.salesvideo.SalesVideoJobEventType;
import com.marketinghub.salesvideo.SalesVideoStatus;
import lombok.Data;

import java.time.Instant;

/**
 * DTO para auditoria de eventos de job.
 */
@Data
public class SalesVideoJobEventDto {
    private Long id;
    private SalesVideoJobEventType eventType;
    private SalesVideoStatus oldStatus;
    private SalesVideoStatus newStatus;
    private String message;
    private String detailsJson;
    private Instant createdAt;
}
