package com.marketinghub.experiment.report.dto;

import com.marketinghub.experiment.dto.ExperimentCampaignMetricDto;
import com.marketinghub.experiment.funnel.dto.ExperimentFunnelStageDto;
import com.marketinghub.experiment.funnel.service.analytics.ExperimentLandingAnalyticsDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Pacote de informações utilizadas para compor o relatório objetivo de um experimento.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExperimentReportMaterialDto {
    private ExperimentSnapshot experiment;
    private MarketNicheSnapshot niche;
    private HypothesisSnapshot hypothesis;
    @Builder.Default
    private List<CreativeSnapshot> creatives = Collections.emptyList();
    @Builder.Default
    private List<CreativeVariantSnapshot> creativeVariants = Collections.emptyList();
    @Builder.Default
    private List<LandingPageSnapshot> landingPages = Collections.emptyList();
    @Builder.Default
    private List<LeadPortalFlowSnapshot> leadPortalFlows = Collections.emptyList();
    private InstantFormSnapshot instantForm;
    private ExperimentCampaignMetricDto campaignMetric;
    private ExperimentLandingAnalyticsDto landingAnalytics;
    @Builder.Default
    private List<ExperimentFunnelStageDto> funnelStages = Collections.emptyList();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExperimentSnapshot {
        private Long id;
        private String name;
        private String status;
        private String platform;
        private String stage;
        private String captureDestinationType;
        private String primaryVariable;
        private String primaryMetric;
        private LocalDate startDate;
        private LocalDate endDate;
        private BigDecimal dailyBudget;
        private BigDecimal kpiTargetCpl;
        private BigDecimal stopLossCpl;
        private Integer sampleSize;
        private BigDecimal baselineCvr;
        private BigDecimal targetCvr;
        private BigDecimal mdePercent;
        private Instant createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MarketNicheSnapshot {
        private Long id;
        private String name;
        private String description;
        private List<String> interestList;
        private List<String> roleList;
        private List<String> behaviorList;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HypothesisSnapshot {
        private UUID id;
        private String title;
        private String promise;
        private String problem;
        private String persona;
        private String mechanism;
        private String uniqueMechanism;
        private String entrega;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreativeSnapshot {
        private Long id;
        private String headline;
        private String primaryText;
        private String description;
        private String cta;
        private String destinationUrl;
        private String imageUrl;
        private String videoId;
        private String format;
        private String status;
        private List<String> angles;
        private List<String> emotionalTriggers;
        private List<String> visualProofs;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreativeVariantSnapshot {
        private Long id;
        private String type;
        private String assetUrl;
        private List<String> titles;
        private List<String> descriptions;
        private Instant createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LandingPageSnapshot {
        private Long id;
        private String url;
        private String type;
        private String status;
        private Instant createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LeadPortalFlowSnapshot {
        private Long id;
        private String name;
        private String slug;
        private String description;
        private String model;
        private boolean approved;
        private String publicUrl;
        private String previewImageUrl;
        private Instant createdAt;
        private List<LeadPortalQuestionSnapshot> questions;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LeadPortalQuestionSnapshot {
        private Long id;
        private String title;
        private String type;
        private boolean required;
        private List<String> options;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InstantFormSnapshot {
        private Long id;
        private String name;
        private String status;
        private String facebookFormId;
        private String shareLink;
        private String followUpActionUrl;
        private String privacyPolicyUrl;
        private boolean approved;
        private boolean published;
    }
}
