package com.marketinghub.agenttask;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/** Responsabilidade: vincular uma fonte acessada ou um link de ajuda à tarefa que o originou. */
@Entity
@Table(name = "agent_task_audit_link")
@Getter
@Setter
public class AgentTaskAuditLink {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "agent_task_id", nullable = false)
  private AgentTask task;

  @Column(name = "link_type", nullable = false, length = 32)
  private String linkType;

  @Column(name = "label", nullable = false, length = 200)
  private String label;

  @Column(name = "url", nullable = false, length = 2048)
  private String url;

  @Column(name = "access_method", length = 32)
  private String accessMethod;

  @Column(name = "accessed_at")
  private Instant accessedAt;

  @Column(name = "display_order", nullable = false)
  private Integer displayOrder;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;
}
