package com.marketinghub.deliverable.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

/**
 * Payload for creating deliverables.
 */
@Data
public class CreateDeliverableRequest {
    @JsonAlias({"nicheId", "marketNicheId"})
    private Long marketNicheId;
    private String title;
    private String description;
    private String content;
    private String model;
    private String prompt;
}
