package com.marketinghub.opportunitydossier;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Responsabilidade: preservar uma evidência e sua fonte no dossiê. */
@Entity
@Table(name = "opportunity_evidence")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpportunityEvidence {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "dossier_id", nullable = false)
  private OpportunityDossier dossier;

  @Column(name = "source_url", nullable = false, length = 1024)
  private String sourceUrl;

  @Lob
  @JdbcTypeCode(SqlTypes.LONGVARCHAR)
  @Column(nullable = false, columnDefinition = "LONGTEXT")
  private String summary;

  @Column(name = "created_by", nullable = false, length = 191)
  private String createdBy;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false)
  private Instant createdAt;
}
