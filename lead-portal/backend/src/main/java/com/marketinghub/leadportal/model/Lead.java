package com.marketinghub.leadportal.model;

import java.time.Instant;
import java.util.UUID;

public class Lead {

    private final UUID id;
    private final String name;
    private final String email;
    private final String notes;
    private final String originalFileName;
    private final String storedFileName;
    private final String contentType;
    private final Instant createdAt;
    private volatile Instant completedAt;
    private volatile LeadStatus status;
    private volatile String result;

    public Lead(
            UUID id,
            String name,
            String email,
            String notes,
            String originalFileName,
            String storedFileName,
            String contentType,
            Instant createdAt) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.notes = notes;
        this.originalFileName = originalFileName;
        this.storedFileName = storedFileName;
        this.contentType = contentType;
        this.createdAt = createdAt;
        this.status = LeadStatus.PROCESSING;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getNotes() {
        return notes;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public String getStoredFileName() {
        return storedFileName;
    }

    public String getContentType() {
        return contentType;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public LeadStatus getStatus() {
        return status;
    }

    public String getResult() {
        return result;
    }

    public void markCompleted(String result, Instant completedAt) {
        this.status = LeadStatus.COMPLETED;
        this.result = result;
        this.completedAt = completedAt;
    }
}
