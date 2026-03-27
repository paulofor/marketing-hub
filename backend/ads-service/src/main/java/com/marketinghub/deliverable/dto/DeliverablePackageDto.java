package com.marketinghub.deliverable.dto;

import lombok.Data;

import java.time.Instant;
import java.util.List;

/**
 * DTO describing a {@link com.marketinghub.deliverable.DeliverablePackage}.
 */
@Data
public class DeliverablePackageDto {
    private Long id;
    private Long experimentId;
    private String experimentName;
    private java.util.UUID hypothesisId;
    private String hypothesisTitle;
    private Long nicheId;
    private String nicheName;
    private String name;
    private String description;
    private String model;
    private String prompt;
    private List<DeliverableDto> deliverables;
    private Instant createdAt;
    private Instant updatedAt;
}
