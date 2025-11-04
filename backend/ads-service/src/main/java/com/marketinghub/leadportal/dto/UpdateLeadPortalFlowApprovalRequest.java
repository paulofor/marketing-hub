package com.marketinghub.leadportal.dto;

import lombok.Data;

/**
 * Request body used to approve or revoke a lead portal flow.
 */
@Data
public class UpdateLeadPortalFlowApprovalRequest {
    private boolean approved;
}
