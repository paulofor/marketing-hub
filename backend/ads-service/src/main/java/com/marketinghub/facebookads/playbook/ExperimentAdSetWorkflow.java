package com.marketinghub.facebookads.playbook;

import com.marketinghub.experiment.Experiment;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Workflow orchestration entity for the ad set playbook.
 */
@Entity
@Table(name = "experiment_adset_workflow")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExperimentAdSetWorkflow {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "experiment_id", nullable = false, unique = true)
    @ToString.Exclude
    private Experiment experiment;

    @Enumerated(EnumType.STRING)
    @Column(length = 32, nullable = false)
    @Builder.Default
    private ExperimentAdSetWorkflowStatus status = ExperimentAdSetWorkflowStatus.NOT_STARTED;

    @Column(name = "seed_keyword", length = 255)
    private String seedKeyword;

    @Column(name = "seed_locale", length = 10)
    private String seedLocale;

    @Column(name = "seed_interest_id", length = 64)
    private String seedInterestId;

    @Column(name = "seed_interest_name", length = 255)
    private String seedInterestName;

    @Column(name = "seed_audience_lower")
    private Long seedAudienceLower;

    @Column(name = "seed_audience_upper")
    private Long seedAudienceUpper;

    @Column(name = "ai_notes", columnDefinition = "LONGTEXT")
    private String aiNotes;

    @Column(name = "last_error", columnDefinition = "LONGTEXT")
    private String lastError;

    @Column(name = "completed_at")
    private Instant completedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    @OneToMany(mappedBy = "workflow", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ExperimentAdSetJob> jobs = new ArrayList<>();

    @OneToMany(mappedBy = "workflow", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ExperimentAdSetSpec> specs = new ArrayList<>();

    public void resetForRestart() {
        this.status = ExperimentAdSetWorkflowStatus.NOT_STARTED;
        this.seedKeyword = null;
        this.seedLocale = null;
        this.seedInterestId = null;
        this.seedInterestName = null;
        this.seedAudienceLower = null;
        this.seedAudienceUpper = null;
        this.aiNotes = null;
        this.lastError = null;
        this.completedAt = null;
    }
}
