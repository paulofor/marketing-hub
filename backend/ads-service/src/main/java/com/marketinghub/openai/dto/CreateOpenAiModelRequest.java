package com.marketinghub.openai.dto;

import java.math.BigDecimal;
import lombok.Data;

/**
 * Responsabilidade: receber os dados editáveis do cadastro administrativo de modelos OpenAI.
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
    private boolean acceptsImageInput;
}
