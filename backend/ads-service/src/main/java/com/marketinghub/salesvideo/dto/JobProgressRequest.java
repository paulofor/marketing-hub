package com.marketinghub.salesvideo.dto;

import com.marketinghub.salesvideo.SalesVideoStatus;
import lombok.Data;

/**
 * Atualização de progresso vinda dos módulos assíncronos.
 */
@Data
public class JobProgressRequest {
    private Integer progressPercent;
    private SalesVideoStatus status;
    private String message;
    private String detailsJson;
}
