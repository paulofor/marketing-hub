package com.marketinghub.experiment;

import jakarta.persistence.*;
import lombok.*;
import com.marketinghub.ads.FacebookInstantForm;
import com.marketinghub.ads.FacebookPage;
import com.marketinghub.deliverable.DeliverablePackage;
import com.marketinghub.niche.MarketNiche;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import com.marketinghub.journey.model.JourneyTemplate;
import com.marketinghub.ads.InstagramAccount;
import com.marketinghub.leadportal.LeadPortalFlow;
import com.marketinghub.imagegeneration.ImageGenerationModel;
import com.marketinghub.imagegeneration.ImageGenerationQuality;
import com.marketinghub.sampleemail.SampleEmail;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Representa um experimento comercial com contexto, ativos, métricas e regras de publicação.
 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"niche_id", "name"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Experiment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "niche_id", nullable = false)
    private MarketNiche niche;

    @Column(nullable = false)
    private String name;

    @Column(length = 255)
    private String hypothesis;

    /** Dor única que o experimento deve validar no funil. */
    @Column(name = "single_pain", length = 512)
    private String singlePain;

    /** Recompensa gratuita única prometida ao lead. */
    @Column(name = "free_reward", length = 512)
    private String freeReward;

    /** Promessa central que deve ser repetida em anúncio, landing, formulário e entrega. */
    @Column(name = "funnel_promise", length = 512)
    private String funnelPromise;

    /** CTA principal do experimento, como “Receber as 3 mensagens”. */
    @Column(name = "primary_cta", length = 191)
    private String primaryCta;

    /** Objetivo de campanha exigido para publicação do experimento. */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "campaign_objective", length = 32, nullable = false)
    private ExperimentCampaignObjective campaignObjective = ExperimentCampaignObjective.LEADS;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "facebook_page_id")
    private FacebookPage facebookPage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "facebook_instant_form_id")
    private FacebookInstantForm facebookInstantForm;

    @Column(name = "facebook_release_requested_at")
    private Instant facebookReleaseRequestedAt;

    @Column(name = "funnel_reset_at")
    private Instant funnelResetAt;

    @Column(name = "follow_up_action_url", length = 512)
    private String followUpActionUrl;

    @Column(name = "lead_portal_flow_model", length = 191)
    private String leadPortalFlowModel;

    @Builder.Default
    @Column(name = "schema_first_lead_portal_enabled", nullable = false)
    private boolean schemaFirstLeadPortalEnabled = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lead_portal_flow_id")
    private LeadPortalFlow leadPortalFlow;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instagram_account_id")
    private InstagramAccount instagramAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "image_model_id")
    private ImageGenerationModel imageGenerationModel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "image_model_quality_id")
    private ImageGenerationQuality imageGenerationQuality;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "hypothesis_id", nullable = false)
    private com.marketinghub.hypothesis.Hypothesis hypothesisRef;

    /** Retorna o identificador da hipótese associada para contratos operacionais internos. */
    public UUID getHypothesisRefIdForPending() {
        return hypothesisRef != null ? hypothesisRef.getId() : null;
    }

    /** Retorna o título da hipótese associada para contratos operacionais internos. */
    public String getHypothesisRefTitleForPending() {
        return hypothesisRef != null ? hypothesisRef.getTitle() : null;
    }

    /** Retorna o JSON do framework da hipótese associada para contratos operacionais internos. */
    public String getHypothesisFrameworkJsonForPending() {
        return hypothesisRef != null ? hypothesisRef.getFrameworkJson() : null;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "metric_preset_id")
    private MetricPreset metricPreset;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journey_template_id")
    private JourneyTemplate journeyTemplate;

    @Column(precision = 10, scale = 2)
    private java.math.BigDecimal kpiTargetCpl;
    /** Stop-loss operacional em CPL. */
    @Column(precision = 10, scale = 2)
    private java.math.BigDecimal stopLossCpl;

    /** Tamanho de amostra desejado para o experimento. */
    private Integer sampleSize;

    /** Taxa de conversão base para comparação. */
    @Column(precision = 5, scale = 2)
    private java.math.BigDecimal baselineCvr;

    /** Taxa de conversão desejada para sucesso. */
    @Column(precision = 5, scale = 2)
    private java.math.BigDecimal targetCvr;

    /** Orçamento diário previsto para o experimento. */
    @Column(name = "daily_budget", precision = 10, scale = 2)
    private java.math.BigDecimal dailyBudget;

    @Column(name = "unit_price_brl", precision = 10, scale = 2)
    private java.math.BigDecimal unitPrice;

    /** Custo estimado em BRL para o experimento. */
    @Column(precision = 10, scale = 2)
    private java.math.BigDecimal cost;

    /** Custo total acumulado do experimento. */
    @Column(precision = 12, scale = 2)
    private java.math.BigDecimal totalCost;

    /** Despesa estimada em BRL para o experimento. */
    @Column(precision = 10, scale = 2)
    private java.math.BigDecimal expense;

    /** MDE (Minimum Detectable Effect) percentual. */
    @Column(precision = 5, scale = 2)
    private java.math.BigDecimal mdePercent;
    private LocalDate startDate;
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private ExperimentStatus status;

    @Enumerated(EnumType.STRING)
    private ExperimentPlatform platform;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "stage", length = 32)
    private ExperimentStage stage = ExperimentStage.AD;

    @Enumerated(EnumType.STRING)
    @Column(name = "creative_generation_mode", length = 32, nullable = false)
    @Builder.Default
    private CreativeGenerationMode creativeGenerationMode = CreativeGenerationMode.DEFAULT;

    /** Estado operacional da última solicitação de geração de criativos. */
    @Enumerated(EnumType.STRING)
    @Column(name = "creative_generation_status", length = 32, nullable = false)
    @Builder.Default
    private CreativeGenerationStatus creativeGenerationStatus = CreativeGenerationStatus.IDLE;

    /** Momento em que a geração de criativos foi solicitada. */
    @Column(name = "creative_generation_requested_at")
    private Instant creativeGenerationRequestedAt;

    /** Momento em que o Worker AI assumiu a geração de criativos. */
    @Column(name = "creative_generation_started_at")
    private Instant creativeGenerationStartedAt;

    /** Momento em que a geração de criativos foi concluída ou encerrada. */
    @Column(name = "creative_generation_finished_at")
    private Instant creativeGenerationFinishedAt;

    /** Último erro operacional da geração de criativos. */
    @Column(name = "creative_generation_error", length = 1024)
    private String creativeGenerationError;

    @Column(name = "primary_variable", length = 191)
    private String primaryVariable;

    @Column(name = "primary_metric", length = 191)
    private String primaryMetric;

    /** Indica se o criativo está aprovado pelo usuário. */
    @Column(nullable = false)
    private boolean creativeApproved;

    @Column(name = "creative_text_prompt", columnDefinition = "LONGTEXT")
    private String creativeTextPrompt;

    @Column(name = "creative_image_prompt", columnDefinition = "LONGTEXT")
    private String creativeImagePrompt;

    @Column(name = "campaign_angle", columnDefinition = "LONGTEXT")
    private String campaignAngle;

    @Column(name = "ad_copy", columnDefinition = "LONGTEXT")
    private String adCopy;

    @Column(name = "ad_image_briefing", columnDefinition = "LONGTEXT")
    private String adImageBriefing;

    @Column(name = "landing_page_copy", columnDefinition = "LONGTEXT")
    private String landingPageCopy;

    @Column(name = "landing_page_copy_job_id", columnDefinition = "BINARY(36)")
    private byte[] landingPageCopyJobId;

    @Column(name = "landing_page_wireframe", columnDefinition = "LONGTEXT")
    private String landingPageWireframe;

    @Column(name = "landing_page_wireframe_job_id", columnDefinition = "BINARY(36)")
    private byte[] landingPageWireframeJobId;

    @Column(name = "landing_page_image_planning", columnDefinition = "LONGTEXT")
    private String landingPageImagePlanning;

    @Column(name = "landing_page_image_assets", columnDefinition = "LONGTEXT")
    private String landingPageImageAssets;

    @Column(name = "landing_page_design_preset", columnDefinition = "LONGTEXT")
    private String landingPageDesignPreset;

    @Column(name = "html_geralanding", columnDefinition = "LONGTEXT")
    private String htmlGeraLanding;

    @Column(name = "landing_page_quality_review", columnDefinition = "LONGTEXT")
    private String landingPageQualityReview;

    @Column(name = "landing_page_deliverables", columnDefinition = "LONGTEXT")
    private String landingPageDeliverables;

    @Column(name = "landing_page_html", columnDefinition = "LONGTEXT")
    private String landingPageHtml;

    /** Quantidade de criativos a serem gerados pelo worker. */
    @Column(name = "creatives_to_generate")
    private Integer creativesToGenerate;

    /** Quantidade de instant forms a serem gerados pelo worker. */
    @Column(name = "instant_forms_to_generate")
    private Integer instantFormsToGenerate;

    /** Quantidade de e-mails a serem gerados pelo worker. */
    @Column(name = "emails_to_generate")
    private Integer emailsToGenerate;

    /** Quantidade de e-mails de amostra a serem gerados pelo worker. */
    @Column(name = "sample_emails_to_generate")
    private Integer sampleEmailsToGenerate;

    /** Quantidade de definições de entregáveis a serem geradas pelo worker. */
    @Column(name = "deliverables_to_generate")
    private Integer deliverablesToGenerate;

    /** Quantidade de fluxos do portal do lead a serem gerados pelo worker. */
    @Column(name = "lead_portal_flows_to_generate")
    private Integer leadPortalFlowsToGenerate;

    /** Quantidade de imagens que cada pacote deve conter. */
    @Builder.Default
    @Column(name = "images_per_package")
    private Integer imagesPerPackage = 20;

    /** Quantidade de imagens abertas por pacote. */
    @Column(name = "open_images_per_package")
    private Integer openImagesPerPackage;

    /** Quantidade de imagens compactadas por pacote. */
    @Column(name = "compressed_images_per_package")
    private Integer compressedImagesPerPackage;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    @OneToOne(mappedBy = "experiment", fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private ExperimentCampaignMetric campaignMetric;

    @OneToMany(mappedBy = "experiment")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private java.util.List<DeliverablePackage> deliverablePackages;

    @OneToMany(mappedBy = "experiment")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private java.util.List<SampleEmail> sampleEmails;

    @OneToMany(mappedBy = "experiment")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private java.util.List<LeadPortalFlow> leadPortalFlows;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "selected_sample_email_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private SampleEmail selectedSampleEmail;
    @PrePersist
    void applyMetricPreset() {
        if (metricPreset != null) {
            if (sampleSize == null) {
                sampleSize = metricPreset.getSampleSize();
            }
            if (stopLossCpl == null && kpiTargetCpl != null && metricPreset.getStopLossFactor() != null) {
                stopLossCpl = kpiTargetCpl.multiply(metricPreset.getStopLossFactor());
            }
            if (mdePercent == null) {
                mdePercent = metricPreset.getDefaultMdePp();
            }
        }
    }
}
