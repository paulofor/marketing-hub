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
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
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

  @Column(name = "plan_version_number", nullable = false)
  private Integer planVersionNumber;

  @Column(name = "assigned_agent_key", length = 100)
  private String assignedAgentKey;

  @Column(name = "assigned_agent_nickname", length = 100)
  private String assignedAgentNickname;

  @Lob
  @JdbcTypeCode(SqlTypes.LONGVARCHAR)
  @Column(name = "expected_result", columnDefinition = "LONGTEXT")
  private String expectedResult;

  @Column(name = "execution_status", nullable = false, length = 30)
  private String executionStatus;

  @Column(name = "due_date", nullable = false)
  private LocalDate dueDate;

  @Column(name = "planned_cost", precision = 15, scale = 2)
  private BigDecimal plannedCost;

  @Column(name = "planned_revenue", precision = 15, scale = 2)
  private BigDecimal plannedRevenue;

  @CreationTimestamp
  @Column(name = "created_at")
  private Instant createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at")
  private Instant updatedAt;
}
