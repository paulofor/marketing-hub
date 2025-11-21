package com.marketinghub.leadportal.model;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record FlowSubmission(
        UUID id,
        String flowSlug,
        String name,
        String email,
        Map<String, Object> answers,
        String imageQuestionKey,
        String storedFileName,
        String originalFileName,
        String contentType,
        Instant createdAt) {}
