package com.marketinghub.audience.dto;

import lombok.Data;

/**
 * Request body for updating an audience.
 */
@Data
public class UpdateAudienceRequest {
    private Boolean approved;
}
