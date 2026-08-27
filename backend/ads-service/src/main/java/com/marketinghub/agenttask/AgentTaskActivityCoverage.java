package com.marketinghub.agenttask;

import com.marketinghub.businessprocess.BusinessProcessActivityDefinition;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/**
 * Responsabilidade: vincular uma tarefa composta às atividades adicionais realmente cobertas por
 * sua execução.
 */
@Entity
@Table(
    name = "agent_task_activity_coverage",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_agent_task_activity_coverage",
            columnNames = {"agent_task_id", "activity_definition_id"}))
@Getter
@Setter
public class AgentTaskActivityCoverage {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "agent_task_id", nullable = false)
  private AgentTask agentTask;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "activity_definition_id", nullable = false)
  private BusinessProcessActivityDefinition activityDefinition;

  @Column(name = "coverage_source", nullable = false, length = 40)
  private String coverageSource;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;
}
