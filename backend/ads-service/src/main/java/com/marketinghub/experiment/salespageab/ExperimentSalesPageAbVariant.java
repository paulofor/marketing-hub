package com.marketinghub.experiment.salespageab;

import com.marketinghub.experiment.video.ExperimentVideoAsset;
import com.marketinghub.gerasalespage.v1.GeraSalesPagePublicationAudit;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/** Responsabilidade: guardar uma variante mensuravel dentro de um teste A/B de pagina de venda. */
@Entity
@Table(name = "experiment_sales_page_ab_variant")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExperimentSalesPageAbVariant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ab_test_id", nullable = false)
    @ToString.Exclude
    private ExperimentSalesPageAbTest test;

    @Column(name = "variant_key", nullable = false, length = 16)
    private String variantKey;

    @Column(name = "name", nullable = false, length = 191)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "variant_type", nullable = false, length = 32)
    private ExperimentSalesPageAbVariantType variantType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private ExperimentSalesPageAbVariantStatus status;

    @Column(name = "traffic_weight", nullable = false, precision = 5, scale = 2)
    private BigDecimal trafficWeight;

    @Column(name = "sales_page_url", length = 1024)
    private String salesPageUrl;

    @Column(name = "checkout_url", length = 1024)
    private String checkoutUrl;

    @Column(name = "ad_destination_url", length = 1024)
    private String adDestinationUrl;

    @Column(name = "analytics_variant_param", nullable = false, length = 64)
    private String analyticsVariantParam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "publication_audit_id")
    @ToString.Exclude
    private GeraSalesPagePublicationAudit publicationAudit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "experiment_video_asset_id")
    @ToString.Exclude
    private ExperimentVideoAsset experimentVideoAsset;

    @Column(name = "required_collectors_present", nullable = false)
    private boolean requiredCollectorsPresent;

    @Column(name = "created_at")
    @CreationTimestamp
    private Instant createdAt;

    @Column(name = "updated_at")
    @UpdateTimestamp
    private Instant updatedAt;
}
