package com.marketinghub.salesvideo.dto;

import lombok.Data;

/**
 * Heartbeat periódico enviado pelos módulos externos.
 */
@Data
public class JobHeartbeatRequest {
    private String message;
    private String detailsJson;
}
