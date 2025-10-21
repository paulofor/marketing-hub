package com.marketinghub.deliverable.dto;

import lombok.Data;

import java.time.Instant;

/**
 * DTO exposing {@link com.marketinghub.deliverable.Deliverable} data to the API.
 */
@Data
public class DeliverableDto {
    private Long id;
    private Long nicheId;
    private String nicheName;
    private String title;
    private String description;
    private String content;
    private String model;
    private String prompt;
    private Instant createdAt;
    private Instant updatedAt;
}
