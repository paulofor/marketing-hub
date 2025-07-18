package com.marketinghub.creative.dto;

import com.marketinghub.creative.CreativeStatus;
import lombok.Data;

/**
 * Data transfer object for Creative.
 */
@Data
public class CreativeDto {
    private Long id;
    private Long experimentId;
    private String headline;
    private String primaryText;
    private String imageUrl;
    private CreativeStatus status;
}
