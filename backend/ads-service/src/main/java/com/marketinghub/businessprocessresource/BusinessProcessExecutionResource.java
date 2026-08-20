package com.marketinghub.businessprocessresource;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/**
 * Responsabilidade: representar um recurso especializado disponível para atividades de processo.
 */
@Entity
@Table(
    name = "business_process_execution_resource",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_business_process_execution_resource_code",
            columnNames = "resource_code"))
@Getter
@Setter
public class BusinessProcessExecutionResource {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "resource_code", nullable = false, length = 100)
  private String resourceCode;

  @Column(name = "name", nullable = false, length = 160)
  private String name;

  @Column(name = "description", nullable = false, length = 500)
  private String description;

  @Column(name = "resource_type", nullable = false, length = 30)
  private String resourceType;

  @Column(name = "responsible_agent_key", nullable = false, length = 100)
  private String responsibleAgentKey;

  @Column(name = "executor_reference", nullable = false, length = 160)
  private String executorReference;

  @Column(name = "usage_instructions", nullable = false, columnDefinition = "TEXT")
  private String usageInstructions;

  @Column(name = "active", nullable = false)
  private boolean active;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;
}
