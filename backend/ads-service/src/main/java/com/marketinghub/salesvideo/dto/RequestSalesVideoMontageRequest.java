package com.marketinghub.salesvideo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Data;

/**
 * Solicitação para montar um vídeo único a partir de múltiplos clipes prontos.
 */
@Data
public class RequestSalesVideoMontageRequest {
    @NotBlank
    private String requestedBy;

    @Size(min = 2, message = "Selecione pelo menos dois vídeos para montagem.")
    private List<Long> sourceJobIds;
}
