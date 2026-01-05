package com.marketinghub.openai.dto;

import java.math.BigDecimal;
import lombok.Data;

/**
 * Request body for creating/updating OpenAI models in the catalog.
 */
@Data
public class CreateOpenAiModelRequest {
    private String name;
    private String code;
    private BigDecimal priceInputStandard;
    private BigDecimal priceInputCachedStandard;
    private BigDecimal priceOutputStandard;
    private BigDecimal priceInputBatch;
    private BigDecimal priceInputCachedBatch;
    private BigDecimal priceOutputBatch;
}
