package com.marketinghub.businessprocesschain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/** Responsabilidade: representar uma versão auditável de uma cadeia de processos de valor. */
@Entity
@Table(
    name = "business_process_chain_definition",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_business_process_chain_code_version",
            columnNames = {"chain_code", "version_number"}))
@Getter
@Setter
public class BusinessProcessChainDefinition {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "chain_code", nullable = false, length = 100)
  private String chainCode;

  @Column(name = "name", nullable = false, length = 160)
  private String name;

  @Column(name = "purpose", nullable = false, columnDefinition = "TEXT")
  private String purpose;

  @Column(name = "outcome_description", nullable = false, length = 500)
  private String outcomeDescription;

  @Column(name = "primary_metric", nullable = false, length = 200)
  private String primaryMetric;

  @Column(name = "version_number", nullable = false)
  private Integer versionNumber;

  @Column(name = "status", nullable = false, length = 20)
  private String status;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "published_at")
  private Instant publishedAt;

  @OneToMany(mappedBy = "chainDefinition", fetch = FetchType.LAZY)
  @OrderBy("sequenceNumber ASC")
  private List<BusinessProcessChainItem> items = new ArrayList<>();
}
