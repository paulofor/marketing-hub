package com.marketinghub.course.dto;

import java.time.Instant;
import lombok.Data;

/**
 * Data transfer object for {@link com.marketinghub.course.CoursePlan}.
 */
@Data
public class CoursePlanDto {
    private Long id;
    private String targetAudience;
    private String transformation;
    private String macroTopics;
    private String modules;
    private String objectives;
    private String resources;
    private Instant createdAt;
    private Instant updatedAt;
}
