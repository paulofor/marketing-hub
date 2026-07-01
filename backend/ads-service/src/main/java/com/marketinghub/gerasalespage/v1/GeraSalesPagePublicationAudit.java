package com.marketinghub.gerasalespage.v1;

import com.marketinghub.experiment.Experiment;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Responsabilidade: preservar o snapshot historico de uma pagina de venda publicada pelo GeraSalesPage v1. */
@Entity
@Table(name = "gera_sales_page_publication_audit")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeraSalesPagePublicationAudit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "experiment_id", nullable = false)
    private Long experimentId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "experiment_id", nullable = false, insertable = false, updatable = false)
    private Experiment experiment;

    @Column(name = "publication_job_id", nullable = false, length = 36)
    private String publicationJobId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "publication_job_id", nullable = false, insertable = false, updatable = false)
    private GeraSalesPageStageExecution publicationJob;

    @Column(name = "published_at", nullable = false)
    private Instant publishedAt;

    @Column(name = "sales_page_url", length = 1024)
    private String salesPageUrl;

    @Column(name = "checkout_url", length = 1024)
    private String checkoutUrl;

    @Column(name = "html", columnDefinition = "LONGTEXT")
    private String html;

    @Column(name = "publication_package_json", columnDefinition = "LONGTEXT")
    private String publicationPackageJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
