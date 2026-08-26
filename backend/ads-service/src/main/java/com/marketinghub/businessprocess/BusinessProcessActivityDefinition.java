package com.marketinghub.businessprocess;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/**
 * Responsabilidade: representar uma atividade versionada e persistida de um processo de negócio.
 */
@Entity
@Table(
    name = "business_process_activity_definition",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_business_process_activity_definition",
            columnNames = {"process_definition_id", "activity_id"}))
@Getter
@Setter
public class BusinessProcessActivityDefinition {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "process_definition_id", nullable = false)
  private BusinessProcessDefinition processDefinition;

  @Column(name = "activity_id", nullable = false, length = 100)
  private String activityId;

  @Column(name = "name", nullable = false, length = 160)
  private String name;

  @Column(name = "objective", columnDefinition = "TEXT")
  private String objective;

  @Column(name = "owner_name", length = 160)
  private String ownerName;

  @Column(name = "execution_resource_code", length = 100)
  private String executionResourceCode;

  @Column(name = "subprocess_code", length = 100)
  private String subprocessCode;

  @Column(name = "definition_json", nullable = false, columnDefinition = "LONGTEXT")
  private String definitionJson;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;
}
