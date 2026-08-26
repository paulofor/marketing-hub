package com.marketinghub.product;

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

/** Responsabilidade: preservar um commit atribuído explicitamente a um produto e processo. */
@Entity
@Table(
    name = "product_process_commit",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_product_process_commit",
            columnNames = {"product_id", "process_definition_id", "repository_name", "commit_sha"}))
@Getter
@Setter
public class ProductProcessCommit {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "product_id", nullable = false)
  private Product product;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "process_definition_id", nullable = false)
  private BusinessProcessDefinition processDefinition;

  @Column(name = "repository_name", nullable = false, length = 160)
  private String repositoryName;

  @Column(name = "commit_sha", nullable = false, length = 64)
  private String commitSha;

  @Column(name = "commit_summary", nullable = false, length = 500)
  private String commitSummary;

  @Column(name = "commit_url", length = 512)
  private String commitUrl;

  @Column(name = "recorded_by", nullable = false, length = 191)
  private String recordedBy;

  @Column(name = "recorded_at", nullable = false)
  private Instant recordedAt;
}
