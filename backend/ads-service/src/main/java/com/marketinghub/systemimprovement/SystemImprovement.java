package com.marketinghub.systemimprovement;

import com.marketinghub.agent.Agent;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/**
 * Responsabilidade: representar uma melhoria do sistema sugerida por um agente durante uma tarefa.
 */
@Entity
@Table(name = "system_improvement")
@Getter
@Setter
public class SystemImprovement {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "requested_by_agent_id", nullable = false)
  private Agent requestedByAgent;

  @Column(name = "title", nullable = false, length = 160)
  private String title;

  @Column(name = "description", nullable = false, columnDefinition = "LONGTEXT")
  private String description;

  @Column(name = "task_reference", length = 200)
  private String taskReference;

  @Column(name = "status", nullable = false, length = 30)
  private String status;

  @Column(name = "requested_at", nullable = false)
  private Instant requestedAt;
}
