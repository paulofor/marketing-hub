package com.marketinghub.agent;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/** Responsabilidade: preservar um contrato imutavel e auditavel de uma versao de agente. */
@Getter
@Setter
@Entity
@Table(name = "agent_version")
public class AgentVersion {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "agent_id", nullable = false)
  private Agent agent;

  @Column(name = "version_number", nullable = false)
  private Integer versionNumber;

  @Column(name = "contract_snapshot", nullable = false, columnDefinition = "LONGTEXT")
  private String contractSnapshot;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;
}
