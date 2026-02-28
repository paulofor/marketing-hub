package com.marketinghub.leadportal.entity;

import com.marketinghub.leadportal.model.FlowSubmission;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "flow_submissions")
public class FlowSubmissionEntity {

    @Id
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 36)
    private UUID id;

    @Column(name = "flow_slug", nullable = false, length = 190)
    private String flowSlug;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String email;

    @Column(columnDefinition = "LONGTEXT")
    @Convert(converter = SubmissionAnswerConverter.class)
    private Map<String, Object> answers;

    @Column(name = "image_question_key")
    private String imageQuestionKey;

    @Column(name = "stored_file_name")
    private String storedFileName;

    @Column(name = "original_file_name")
    private String originalFileName;

    @Column(name = "content_type")
    private String contentType;

    @Column(name = "campaign_code", length = 190)
    private String campaignCode;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getFlowSlug() {
        return flowSlug;
    }

    public void setFlowSlug(String flowSlug) {
        this.flowSlug = flowSlug;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Map<String, Object> getAnswers() {
        return answers;
    }

    public void setAnswers(Map<String, Object> answers) {
        this.answers = answers;
    }

    public String getImageQuestionKey() {
        return imageQuestionKey;
    }

    public void setImageQuestionKey(String imageQuestionKey) {
        this.imageQuestionKey = imageQuestionKey;
    }

    public String getStoredFileName() {
        return storedFileName;
    }

    public void setStoredFileName(String storedFileName) {
        this.storedFileName = storedFileName;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public void setOriginalFileName(String originalFileName) {
        this.originalFileName = originalFileName;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public String getCampaignCode() {
        return campaignCode;
    }

    public void setCampaignCode(String campaignCode) {
        this.campaignCode = campaignCode;
    }

    public FlowSubmission toModel() {
        return new FlowSubmission(
                id,
                flowSlug,
                name,
                email,
                answers,
                imageQuestionKey,
                storedFileName,
                originalFileName,
                contentType,
                createdAt,
                campaignCode);
    }

    public static FlowSubmissionEntity fromModel(FlowSubmission submission) {
        FlowSubmissionEntity entity = new FlowSubmissionEntity();
        entity.setId(submission.id());
        entity.setFlowSlug(submission.flowSlug());
        entity.setName(submission.name());
        entity.setEmail(submission.email());
        entity.setAnswers(submission.answers());
        entity.setImageQuestionKey(submission.imageQuestionKey());
        entity.setStoredFileName(submission.storedFileName());
        entity.setOriginalFileName(submission.originalFileName());
        entity.setContentType(submission.contentType());
        entity.setCampaignCode(submission.campaignCode());
        entity.setCreatedAt(submission.createdAt());
        return entity;
    }
}
