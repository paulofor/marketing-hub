package com.marketinghub.ai.dto;

import java.math.BigDecimal;
import java.time.Instant;
import lombok.Data;

/**
 * Data transfer object for {@link com.marketinghub.ai.AiService}.
 */
@Data
public class AiServiceDto {
    private Long id;
    private String name;
    private String objective;
    private String url;
    private BigDecimal price;
    private BigDecimal cost;
    private Instant createdAt;
    private Instant updatedAt;
}
