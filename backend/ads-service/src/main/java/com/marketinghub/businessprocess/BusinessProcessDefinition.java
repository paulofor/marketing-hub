package com.marketinghub.businessprocess;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/**
 * Responsabilidade: representar uma versão auditável de um processo de negócio do Marketing Hub.
 */
@Entity
@Table(
    name = "business_process_definition",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_business_process_code_version",
            columnNames = {"process_code", "version_number"}))
@Getter
@Setter
public class BusinessProcessDefinition {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "process_code", nullable = false, length = 100)
  private String processCode;

  @Column(name = "name", nullable = false, length = 160)
  private String name;

  @Column(name = "purpose", nullable = false, columnDefinition = "TEXT")
  private String purpose;

  @Column(name = "owner_name", nullable = false, length = 120)
  private String ownerName;

  @Column(name = "trigger_description", nullable = false, length = 500)
  private String triggerDescription;

  @Column(name = "outcome_description", nullable = false, length = 500)
  private String outcomeDescription;

  @Column(name = "version_number", nullable = false)
  private Integer versionNumber;

  @Column(name = "status", nullable = false, length = 20)
  private String status;

  @Column(name = "technical_reference", length = 200)
  private String technicalReference;

  @Column(name = "process_type", nullable = false, length = 20)
  private String processType = "VALUE_PROCESS";

  @Column(name = "parent_process_code", length = 100)
  private String parentProcessCode;

  @Column(name = "diagram_json", nullable = false, columnDefinition = "LONGTEXT")
  private String diagramJson;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "published_at")
  private Instant publishedAt;
}
