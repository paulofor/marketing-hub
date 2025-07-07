package com.marketinghub.course.dto;

import lombok.Data;

/**
 * Request body for creating a course plan.
 */
@Data
public class CreateCoursePlanRequest {
    private String targetAudience;
    private String transformation;
    private String macroTopics;
}
