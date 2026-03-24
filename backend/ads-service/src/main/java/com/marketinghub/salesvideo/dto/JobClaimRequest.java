package com.marketinghub.salesvideo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Requisição para "claim" de um job pelos workers internos.
 */
@Data
public class JobClaimRequest {
    @NotBlank
    private String workerId;

    private String message;
}
