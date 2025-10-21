package com.marketinghub.deliverable.dto;

import lombok.Data;

/**
 * Payload for updating existing deliverables.
 */
@Data
public class UpdateDeliverableRequest {
    private String title;
    private String description;
    private String content;
    private String model;
    private String prompt;
}
