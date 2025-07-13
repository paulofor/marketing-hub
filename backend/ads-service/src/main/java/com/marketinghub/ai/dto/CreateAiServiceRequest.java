package com.marketinghub.ai.dto;

import java.math.BigDecimal;
import lombok.Data;

/**
 * Request body for creating an AI service.
 */
@Data
public class CreateAiServiceRequest {
    private String name;
    private String objective;
    private String url;
    private BigDecimal price;
    private BigDecimal cost;
}
