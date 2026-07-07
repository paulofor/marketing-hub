package com.marketinghub.planning;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

/** Responsabilidade: representar um objetivo avaliavel de uma semana do planejamento comercial. */
@Entity
@Table(name = "commercial_plan_week_objective")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommercialPlanWeekObjective {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private CommercialPlan plan;

    @Column(name = "week_number", nullable = false)
    private Integer weekNumber;

    @Column(name = "sequence_order", nullable = false)
    private Integer sequenceOrder;

    @Lob
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "objective_text", nullable = false, columnDefinition = "LONGTEXT")
    private String objectiveText;

    @Column(name = "score")
    private Integer score;

    @CreationTimestamp
    @Column(name = "created_at")
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
