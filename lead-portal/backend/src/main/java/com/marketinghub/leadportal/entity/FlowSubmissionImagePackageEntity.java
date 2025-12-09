package com.marketinghub.leadportal.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "submission_id", nullable = false, length = 36)
    private UUID submissionId;

    @Column(nullable = false, length = 30)
    private String status = Status.RECEIVED.name();

    @Column(name = "planned_outputs")
    private Integer plannedOutputs;

    @Column(name = "free_images", nullable = false)
    private Integer freeImages = 0;

    @Column(name = "model")
    private String model;

    @Column(name = "prompt", nullable = false, columnDefinition = "LONGTEXT")
    private String prompt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private Instant updatedAt;

    @Column(name = "notified_at")
    private Instant notifiedAt;

    @Column(name = "notification_attempts", nullable = false)
    private int notificationAttempts = 0;

    @Column(name = "notification_last_attempt")
    private Instant notificationLastAttempt;

    @Column(name = "notification_last_error", columnDefinition = "TEXT")
    private String notificationLastError;

    public Long getId() {
        return id;
    }

    public UUID getSubmissionId() {
        return submissionId;
    }

    public void setSubmissionId(UUID submissionId) {
        this.submissionId = submissionId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getPlannedOutputs() {
        return plannedOutputs;
    }

    public void setPlannedOutputs(Integer plannedOutputs) {
        this.plannedOutputs = plannedOutputs;
    }

    public Integer getFreeImages() {
        return freeImages;
    }

    public void setFreeImages(Integer freeImages) {
        this.freeImages = freeImages;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }


    public Instant getNotifiedAt() {
        return notifiedAt;
    }

    public void setNotifiedAt(Instant notifiedAt) {
        this.notifiedAt = notifiedAt;
    }

    public int getNotificationAttempts() {
        return notificationAttempts;
    }

    public void setNotificationAttempts(int notificationAttempts) {
        this.notificationAttempts = notificationAttempts;
    }

    public Instant getNotificationLastAttempt() {
        return notificationLastAttempt;
    }

    public void setNotificationLastAttempt(Instant notificationLastAttempt) {
        this.notificationLastAttempt = notificationLastAttempt;
    }

    public String getNotificationLastError() {
        return notificationLastError;
    }

    public void setNotificationLastError(String notificationLastError) {
        this.notificationLastError = notificationLastError;
    }
}
