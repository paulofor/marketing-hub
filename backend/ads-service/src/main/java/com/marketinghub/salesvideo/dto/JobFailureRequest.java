package com.marketinghub.salesvideo.dto;

import com.marketinghub.salesvideo.SalesVideoStatus;
import lombok.Data;

/**
 * Payload para marcar um job como falho.
 */
@Data
public class JobFailureRequest {
    private String failureCode;
    private String failureDetail;
    private SalesVideoStatus status;
    private String message;
}
