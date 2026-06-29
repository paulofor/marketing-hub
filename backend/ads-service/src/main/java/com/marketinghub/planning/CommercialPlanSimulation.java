package com.marketinghub.planning;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Responsabilidade: registrar simulacoes de cenario usadas para apoiar decisoes comerciais. */
@Entity
@Table(name = "commercial_plan_simulation")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommercialPlanSimulation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private CommercialPlan plan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private CommercialPlanRecommendation recommendation;

    @Column(name = "most_likely_scenario", length = 512)
    private String mostLikelyScenario;

    @Column(name = "best_realistic_scenario", length = 512)
    private String bestRealisticScenario;

    @Column(name = "worst_likely_scenario", length = 512)
    private String worstLikelyScenario;

    @Column(name = "main_risk", length = 512)
    private String mainRisk;

    @Column(name = "best_next_action", length = 512)
    private String bestNextAction;

    @Column(name = "action_to_avoid", length = 512)
    private String actionToAvoid;

    @Column(name = "continue_condition", length = 512)
    private String continueCondition;

    @Column(name = "stop_condition", length = 512)
    private String stopCondition;

    @Column(name = "evidence_7_days", length = 512)
    private String evidence7Days;

    @Column(name = "evidence_14_days", length = 512)
    private String evidence14Days;

    @Column(name = "evidence_30_days", length = 512)
    private String evidence30Days;

    @Lob
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "decision_notes", columnDefinition = "LONGTEXT")
    private String decisionNotes;

    @CreationTimestamp
    @Column(name = "created_at")
    private Instant createdAt;
}
