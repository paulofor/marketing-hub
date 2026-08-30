package com.marketinghub.businessprocess.independent;

import com.marketinghub.businessprocess.BusinessProcessDefinition;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/** Responsabilidade: correlacionar uma solicitação independente com sua execução BPM auditável. */
@Entity
@Table(
    name = "business_process_independent_execution",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_business_process_independent_execution_request",
          columnNames = "request_key"),
      @UniqueConstraint(
          name = "uk_business_process_independent_execution_source",
          columnNames = "source_reference")
    })
@Getter
@Setter
public class IndependentBusinessProcessExecution {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "request_key", nullable = false, length = 36)
  private String requestKey;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "process_definition_id", nullable = false)
  private BusinessProcessDefinition processDefinition;

  @Column(name = "source_reference", length = 200)
  private String sourceReference;

  @Column(name = "display_name", nullable = false, length = 160)
  private String displayName;

  @Column(name = "requested_by_name", nullable = false, length = 100)
  private String requestedByName;

  @Column(name = "input_json", nullable = false, columnDefinition = "LONGTEXT")
  private String inputJson;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;
}
