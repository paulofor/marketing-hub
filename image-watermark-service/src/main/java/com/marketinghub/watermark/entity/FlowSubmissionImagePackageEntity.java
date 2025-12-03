package com.marketinghub.watermark.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "flow_submission_image_package")
public class FlowSubmissionImagePackageEntity {

    public enum Status {
        RECEIVED,
        RECENT,
        PROCESSING,
        WATERMARK_PENDING,
        WATERMARKING,
        COMPLETED,
        FAILED
    }

    @Id
    private Long id;

    @Column(name = "submission_id")
    private UUID submissionId;

    @Enumerated(EnumType.STRING)
    private Status status;

    @Column(name = "prompt", columnDefinition = "LONGTEXT")
    private String prompt;

    private String model;

    @Column(name = "failure_reason", columnDefinition = "LONGTEXT")
    private String failureReason;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "imagePackage", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<FlowSubmissionImageItemEntity> items = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UUID getSubmissionId() {
        return submissionId;
    }

    public void setSubmissionId(UUID submissionId) {
        this.submissionId = submissionId;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public List<FlowSubmissionImageItemEntity> getItems() {
        return items;
    }

    public void setItems(List<FlowSubmissionImageItemEntity> items) {
        this.items = items;
    }
}
