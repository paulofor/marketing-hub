package com.example.marketinghub.funnel.dto;

import java.util.UUID;
import lombok.Data;

/**
 * DTO representing a sales funnel summary with experiment count.
 */
@Data
public class SalesFunnelDto {
    private UUID id;
    private String name;
    private String objective;
    private long experimentCount;
}
