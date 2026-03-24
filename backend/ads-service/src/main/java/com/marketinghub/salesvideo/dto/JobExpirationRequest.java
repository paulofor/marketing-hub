package com.marketinghub.salesvideo.dto;

import lombok.Data;

/**
 * Payload usado quando o provider expira o job antes de concluir.
 */
@Data
public class JobExpirationRequest {
    private String message;
    private String detailsJson;
}
