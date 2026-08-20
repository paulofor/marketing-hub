package com.marketinghub.businessprocesschain;

import com.marketinghub.businessprocess.BusinessProcessDefinition;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/** Responsabilidade: vincular um processo versionado a uma posição da cadeia de valor. */
@Entity
@Table(
    name = "business_process_chain_item",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_business_process_chain_sequence",
          columnNames = {"chain_definition_id", "sequence_number"}),
      @UniqueConstraint(
          name = "uk_business_process_chain_process",
          columnNames = {"chain_definition_id", "process_definition_id"})
    })
@Getter
@Setter
public class BusinessProcessChainItem {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "chain_definition_id", nullable = false)
  private BusinessProcessChainDefinition chainDefinition;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "process_definition_id", nullable = false)
  private BusinessProcessDefinition processDefinition;

  @Column(name = "sequence_number", nullable = false)
  private Integer sequenceNumber;

  @Column(name = "value_contribution", nullable = false, length = 500)
  private String valueContribution;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;
}
