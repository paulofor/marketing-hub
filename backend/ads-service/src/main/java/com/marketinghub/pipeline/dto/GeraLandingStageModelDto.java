package com.marketinghub.pipeline.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO que expõe o modelo OpenAI configurado no banco para uma etapa operacional do Gera Landing.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeraLandingStageModelDto {
    private String stageCode;
    private Long pipelineId;
    private String pipelineCode;
    private Long pipelineStageId;
    private String pipelineStageCode;
    private Long openAiModelId;
    private String openAiModelName;
    private String openAiModelCode;
    private BigDecimal priceInputFlex;
    private BigDecimal priceInputCachedFlex;
    private BigDecimal priceOutputFlex;
    private String pricingMode;
    private String generatedAssetType;
    private boolean defaultModelApplied;
}
