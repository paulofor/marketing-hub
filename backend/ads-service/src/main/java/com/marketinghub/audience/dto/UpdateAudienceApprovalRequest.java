package com.marketinghub.audience.dto;

import lombok.Data;

/**
 * Request body for updating the approval status of an audience.
 */
@Data
public class UpdateAudienceApprovalRequest {
    private boolean approved;
}
