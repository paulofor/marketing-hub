package com.marketinghub.creative.dto;

import com.marketinghub.creative.CreativeStatus;
import java.math.BigDecimal;
import lombok.Data;

/**
 * Corpo da requisição para criar ou atualizar um criativo.
 */
@Data
public class CreateCreativeRequest {
    private String format;
    private String headline;
    private String primaryText;
    private String imageUrl;
    private String videoId;
    private String videoUrl;
    private String description;
    private String cta;
    private String destinationUrl;
    private String leadGenFormId;
    private String instagramUserId;
    private CreativeStatus status;

    /**
     * Custo estimado em USD para gerar este criativo.
     */
    private BigDecimal costUsd;
}
