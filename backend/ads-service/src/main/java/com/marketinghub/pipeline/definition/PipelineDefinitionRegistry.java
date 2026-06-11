package com.marketinghub.pipeline.definition;

import com.marketinghub.experiment.pipeline.ExperimentPipelineSection;
import com.marketinghub.oprm.generalaudience.pipeline.OprmGeneralAudienceDiscoveryPipelineSection;
import com.marketinghub.oprm.nichocnae.pipeline.OprmNichoCnaePipelineSection;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Registro oficial que define os pipelines, módulos e etapas estruturais conhecidos pelo backend.
 */
@Component
public class PipelineDefinitionRegistry {
    private static final String EXPERIMENT_PIPELINE_CODE = "experiment-pipeline";
    private static final String EXPERIMENT_MODULE = "EXPERIMENT";
    private static final String EXPERIMENT_CANONICAL_VERSION = "procedimento-experimento-canon.v1";
    private static final String EXPERIMENT_BACKEND_ROOT_PACKAGE = "com.marketinghub.experiment.pipeline";
    private static final String EXPERIMENT_MODULE_ROOT_PACKAGE = "com.marketinghub.worker.experimentpipeline";
    private static final String GERALANDING_BACKEND_ROOT_PACKAGE = "com.marketinghub.geralanding";
    private static final String GERALANDING_OPENAI_CORE_ROOT_PACKAGE = "com.marketinghub.worker.openai.core";
    private static final String GERALANDING_DELIVERABLES_MODULE_PACKAGE = "com.marketinghub.worker.geralanding.deliverables";
    private static final String OPRM_NICHO_CNAE_PIPELINE_CODE = "oprm-nicho-cnae-pipeline";
    private static final String OPRM_MODULE = "OPRM";
    private static final String OPRM_NICHO_CNAE_CANONICAL_VERSION = "oprm-nichocnae-canon.v1";
    private static final String OPRM_GENERAL_AUDIENCE_DISCOVERY_PIPELINE_CODE =
            "oprm-general-audience-discovery-pipeline";
    private static final String OPRM_GENERAL_AUDIENCE_DISCOVERY_CANONICAL_VERSION =
            "oprm-general-audience-discovery-canon.v1";
    private static final String MOIS_SALES_PAGE_LIBRARY_PIPELINE_CODE = "mois-sales-page-library-pipeline";
    private static final String MOIS_MODULE = "MOIS";
    private static final String MOIS_SALES_PAGE_LIBRARY_CANONICAL_VERSION = "mois-sales-page-library-canon.v1";
    private static final String MOIS_SALES_PAGE_LIBRARY_BACKEND_ROOT_PACKAGE =
            "com.marketinghub.mois.bibliotecapaginavenda.worker.v1";
    private static final String MOIS_SALES_PAGE_LIBRARY_WORKER_ROOT_PACKAGE =
            "com.marketinghub.mois.bibliotecapaginavenda.worker.v1";
    private static final String MOIS_SALES_PAGE_LIBRARY_WORKER_MODULE = "mois-sales-library-worker";
    private static final String MOIS_MARKET_WARMUP_WORKER_MODULE = "mois-market-warmup-worker";
    private static final String MOIS_MARKET_WARMUP_WORKER_ROOT_PACKAGE =
            "com.marketinghub.mois.marketwarmup.worker.v1";
    private static final String FACEBOOK_ADS_PUBLICATION_METRICS_PIPELINE_CODE =
            "facebook-ads-publication-metrics-pipeline";
    private static final String FACEBOOK_ADS_MODULE = "FACEBOOK_ADS";
    private static final String FACEBOOK_ADS_CANONICAL_VERSION = "facebook-campaign-publication-canon.v1";
    private static final String FACEBOOK_ADS_WORKER_ROOT_PACKAGE = "com.marketinghub.facebookadsworker.facebookcampaign";
    private static final String FACEBOOK_ADS_WORKER_MODULE = "facebook-ads-worker";

    private final List<PipelineDefinition> officialPipelines;
    private final Set<String> validModules;

    /**
     * Inicializa o registro oficial a partir das etapas canônicas implementadas no código.
     */
    public PipelineDefinitionRegistry() {
        this.officialPipelines = List.of(
                buildExperimentPipeline(),
                buildOprmNichoCnaePipeline(),
                buildOprmGeneralAudienceDiscoveryPipeline(),
                buildMoisSalesPageLibraryPipeline(),
                buildFacebookAdsPublicationMetricsPipeline());
        this.validModules = Set.of(
                EXPERIMENT_MODULE, "GERALANDING", "MDS", MOIS_MODULE, OPRM_MODULE, FACEBOOK_ADS_MODULE);
    }

    /**
     * Lista todos os pipelines oficiais que a tela pode consultar como contrato permitido.
     */
    public List<PipelineDefinition> officialPipelines() {
        return officialPipelines;
    }

    /**
     * Lista os módulos válidos conhecidos para pipelines administráveis.
     */
    public Set<String> validModules() {
        return validModules;
    }

    /**
     * Localiza a definição oficial associada ao código operacional do banco.
     */
    public Optional<PipelineDefinition> findByPipelineCode(String code) {
        String normalized = normalize(code);
        return officialPipelines.stream()
                .filter(definition -> definition.matchesCode(normalized))
                .findFirst();
    }

    /**
     * Indica se o código informado pertence a um pipeline oficial protegido.
     */
    public boolean isOfficialPipelineCode(String code) {
        return findByPipelineCode(code).isPresent();
    }

    /**
     * Localiza uma etapa canônica pelo código operacional ou por alias explícito.
     */
    public Optional<PipelineStageDefinition> findStage(PipelineDefinition pipelineDefinition, String stageCode) {
        String normalized = normalize(stageCode);
        return pipelineDefinition.stages().stream()
                .filter(stage -> stage.matchesCode(normalized))
                .findFirst();
    }

    /**
     * Normaliza códigos estruturais para comparação sem variações de caixa ou separador.
     */
    public String normalize(String code) {
        if (!StringUtils.hasText(code)) {
            return "";
        }
        return code.trim().toLowerCase(Locale.ROOT).replace('_', '-');
    }

    /**
     * Monta a definição oficial do pipeline de experimento separando etapas iniciais e etapas do domínio GeraLanding.
     */
    private PipelineDefinition buildExperimentPipeline() {
        List<PipelineStageDefinition> stages = List.of(
                experimentPipelineStage(ExperimentPipelineSection.CAMPAIGN_ANGLE, 1),
                experimentPipelineStage(ExperimentPipelineSection.AD_COPY, 2),
                experimentPipelineStage(ExperimentPipelineSection.AD_IMAGE_BRIEFING, 3),
                geralandingStage(
                        "LANDING_PAGE_WIREFRAME",
                        "landing-page-wireframe",
                        "Landing Page Wireframe",
                        4,
                        "wireframe",
                        Set.of("landing-page-wireframe", "landing-wireframe")),
                geralandingStage(
                        "LANDING_PAGE_COPY",
                        "landing-page-copy",
                        "Landing Page Copy",
                        5,
                        "copy",
                        Set.of("landing-page-copy", "landing-copy")),
                geralandingStage(
                        "LANDING_PAGE_IMAGE_PLANNING",
                        "landing-page-image-planning",
                        "Landing Page Image Planning",
                        6,
                        "imageplanning",
                        Set.of("landing-page-image-planning", "image-planning", "landing-image-planning")),
                geralandingStage(
                        "LANDING_PAGE_IMAGE_GENERATION",
                        "landing-page-image-generation",
                        "Landing Page Image Generation",
                        7,
                        "imagegeneration",
                        Set.of(
                                "landing-page-image-generation",
                                "image-generation",
                                "landing-image-generation",
                                "framework-image-generation")),
                geralandingStage(
                        "LANDING_PAGE_DESIGN_PRESET",
                        "landing-page-design-preset",
                        "Landing Page Design Preset",
                        8,
                        "presetdesign",
                        Set.of("landing-page-design-preset", "preset-design", "design-preset")),
                geralandingStage(
                        "LANDING_PAGE_QUALITY_REVIEW",
                        "landing-page-quality-review",
                        "Landing Page Quality Review",
                        9,
                        "qualityreview",
                        Set.of("landing-page-quality-review", "quality-review", "landing-quality-review")),
                new PipelineStageDefinition(
                        "LANDING_PAGE_DELIVERABLES",
                        "landing-page-deliverables",
                        "Landing Page Deliverables",
                        10,
                        true,
                        true,
                        true,
                        null,
                        GERALANDING_BACKEND_ROOT_PACKAGE + ".deliverables",
                        GERALANDING_DELIVERABLES_MODULE_PACKAGE,
                        Set.of("landing-page-deliverables", "deliverables", "landing-deliverables", "landing-page-html")));
        return new PipelineDefinition(
                EXPERIMENT_MODULE,
                EXPERIMENT_PIPELINE_CODE,
                "Pipeline de Experimento",
                EXPERIMENT_CANONICAL_VERSION,
                true,
                Set.of(EXPERIMENT_PIPELINE_CODE, "experiment_pipeline"),
                PipelineFieldPolicy.officialDefault(),
                StageFieldPolicy.officialDefault(),
                stages);
    }

    /**
     * Converte uma etapa inicial do experimento para definição canônica com implementação no pacote do pipeline inicial.
     */
    private PipelineStageDefinition experimentPipelineStage(ExperimentPipelineSection section, int position) {
        return new PipelineStageDefinition(
                section.name(),
                section.path(),
                displayName(section.path()),
                position,
                true,
                true,
                true,
                null,
                EXPERIMENT_BACKEND_ROOT_PACKAGE,
                EXPERIMENT_MODULE_ROOT_PACKAGE,
                Set.of(section.path(), section.name(), section.name().toLowerCase(Locale.ROOT)));
    }

    /**
     * Converte uma etapa do núcleo GeraLanding para definição canônica apontando aos pacotes reais da implementação.
     */
    private PipelineStageDefinition geralandingStage(
            String canonicalCode,
            String operationalCode,
            String name,
            int position,
            String implementationPackage,
            Set<String> aliases) {
        return new PipelineStageDefinition(
                canonicalCode,
                operationalCode,
                name,
                position,
                true,
                true,
                true,
                null,
                GERALANDING_BACKEND_ROOT_PACKAGE + "." + implementationPackage,
                GERALANDING_OPENAI_CORE_ROOT_PACKAGE + "." + implementationPackage,
                aliases);
    }

    /**
     * Monta a definição oficial do pipeline OPRM NichoCNAE a partir das etapas implementadas no backend/coletor.
     */
    private PipelineDefinition buildOprmNichoCnaePipeline() {
        List<PipelineStageDefinition> stages = Arrays.stream(OprmNichoCnaePipelineSection.values())
                .map(section -> new PipelineStageDefinition(
                        section.name(),
                        section.path(),
                        section.displayName(),
                        section.position(),
                        true,
                        section.requiresOpenAiModel(),
                        true,
                        section.executionModule(),
                        section.rootPackage(),
                        section.modulePackage(),
                        section.aliases()))
                .toList();
        return new PipelineDefinition(
                OPRM_MODULE,
                OPRM_NICHO_CNAE_PIPELINE_CODE,
                "Pipeline Nicho CNAE",
                OPRM_NICHO_CNAE_CANONICAL_VERSION,
                true,
                Set.of(OPRM_NICHO_CNAE_PIPELINE_CODE, "nicho-cnae-pipeline", "oprm_nicho_cnae_pipeline"),
                PipelineFieldPolicy.officialDefault(),
                StageFieldPolicy.officialDefault(),
                stages);
    }


    /**
     * Monta a definição oficial do pipeline OPRM de descoberta de públicos gerais sem dependência de CNAE.
     */
    private PipelineDefinition buildOprmGeneralAudienceDiscoveryPipeline() {
        List<PipelineStageDefinition> stages = Arrays.stream(OprmGeneralAudienceDiscoveryPipelineSection.values())
                .map(section -> new PipelineStageDefinition(
                        section.name(),
                        section.path(),
                        section.displayName(),
                        section.position(),
                        true,
                        section.requiresOpenAiModel(),
                        true,
                        section.executionModule(),
                        section.rootPackage(),
                        section.modulePackage(),
                        section.aliases()))
                .toList();
        return new PipelineDefinition(
                OPRM_MODULE,
                OPRM_GENERAL_AUDIENCE_DISCOVERY_PIPELINE_CODE,
                "Pipeline de Descoberta de Públicos Gerais",
                OPRM_GENERAL_AUDIENCE_DISCOVERY_CANONICAL_VERSION,
                true,
                Set.of(
                        OPRM_GENERAL_AUDIENCE_DISCOVERY_PIPELINE_CODE,
                        "OPRM_GENERAL_AUDIENCE_DISCOVERY",
                        "general-audience-discovery",
                        "publicos-gerais-pipeline"),
                PipelineFieldPolicy.officialDefault(),
                StageFieldPolicy.officialDefault(),
                stages);
    }

    /**
     * Monta a definição oficial do pipeline da Biblioteca de Páginas de Vendas do MOIS.
     */
    private PipelineDefinition buildMoisSalesPageLibraryPipeline() {
        List<PipelineStageDefinition> stages = List.of(
                moisSalesPageLibraryStage(
                        "HTML_ACQUISITION",
                        "html-acquisition",
                        "Obtenção de HTML",
                        1,
                        false,
                        MOIS_SALES_PAGE_LIBRARY_WORKER_MODULE,
                        "service",
                        MOIS_SALES_PAGE_LIBRARY_WORKER_ROOT_PACKAGE + ".pipeline.htmlcapture",
                        Set.of("html-capture", "page-snapshot", "capture", "obtencao-html")),
                moisSalesPageLibraryStage(
                        "COMMERCIAL_PAGE_ANALYSIS",
                        "commercial-page-analysis",
                        "Análise Comercial da Página",
                        2,
                        true,
                        MOIS_SALES_PAGE_LIBRARY_WORKER_MODULE,
                        "service",
                        MOIS_SALES_PAGE_LIBRARY_WORKER_ROOT_PACKAGE + ".openai",
                        Set.of("page-analysis", "analysis", "commercial-analysis", "analise-comercial")),
                moisSalesPageLibraryStage(
                        "MARKET_WARMUP_RESEARCH",
                        "market-warmup-research",
                        "Investigação de Sucesso do Produto",
                        3,
                        true,
                        MOIS_MARKET_WARMUP_WORKER_MODULE,
                        "service.marketwarmup",
                        MOIS_MARKET_WARMUP_WORKER_ROOT_PACKAGE,
                        Set.of(
                                "market-warmup",
                                "warmup-research",
                                "ecosystem-research",
                                "aquecimento-mercado",
                                "pesquisa-aquecimento",
                                "product-success-research",
                                "investigacao-sucesso-produto")));
        return new PipelineDefinition(
                MOIS_MODULE,
                MOIS_SALES_PAGE_LIBRARY_PIPELINE_CODE,
                "Pipeline Biblioteca de Páginas de Vendas",
                MOIS_SALES_PAGE_LIBRARY_CANONICAL_VERSION,
                true,
                Set.of(
                        MOIS_SALES_PAGE_LIBRARY_PIPELINE_CODE,
                        "mois_sales_page_library_pipeline",
                        "sales-pages-library-pipeline",
                        "biblioteca-paginas-vendas-pipeline"),
                PipelineFieldPolicy.officialDefault(),
                StageFieldPolicy.officialDefault(),
                stages);
    }

    /**
     * Converte uma etapa da biblioteca de páginas de vendas MOIS para definição canônica.
     */
    private PipelineStageDefinition moisSalesPageLibraryStage(
            String canonicalCode,
            String operationalCode,
            String name,
            int position,
            boolean requiresOpenAiModel,
            String executionModule,
            String backendPackage,
            String modulePackage,
            Set<String> aliases) {
        return new PipelineStageDefinition(
                canonicalCode,
                operationalCode,
                name,
                position,
                true,
                requiresOpenAiModel,
                true,
                executionModule,
                MOIS_SALES_PAGE_LIBRARY_BACKEND_ROOT_PACKAGE + "." + backendPackage,
                modulePackage,
                aliases);
    }

    /**
     * Monta a definição oficial do pipeline de publicação e medição de campanhas Facebook Ads.
     */
    private PipelineDefinition buildFacebookAdsPublicationMetricsPipeline() {
        List<PipelineStageDefinition> stages = List.of(
                facebookAdsStage(
                        "WORKER_CONFIGURATION",
                        "worker-configuration",
                        "Configuração da conta Facebook",
                        1,
                        "configuration",
                        Set.of("facebook-worker-config", "accounts-facebook-worker-config", "configuracao-facebook")),
                facebookAdsStage(
                        "EXPERIMENT_READINESS",
                        "experiment-readiness",
                        "Prontidão do experimento",
                        2,
                        "readiness",
                        Set.of("experiments-ready", "facebook-readiness", "prontidao-experimento")),
                facebookAdsStage(
                        "CREATIVE_CONSUMPTION",
                        "creative-consumption",
                        "Criativos aprovados",
                        3,
                        "creative",
                        Set.of("creatives-ready", "approved-creatives", "criativos-aprovados")),
                facebookAdsStage(
                        "REACH_VALIDATION",
                        "reach-validation",
                        "Validação de alcance",
                        4,
                        "publication",
                        Set.of("reachestimate", "alcance-publico", "validacao-alcance")),
                facebookAdsStage(
                        "CAMPAIGN_HIERARCHY_PUBLICATION",
                        "campaign-hierarchy-publication",
                        "Publicação da hierarquia de campanha",
                        5,
                        "publication",
                        Set.of("campaign-publication", "meta-campaign-creation", "publicacao-campanha")),
                facebookAdsStage(
                        "PUBLICATION_REGISTRATION",
                        "publication-registration",
                        "Registro da publicação no backend",
                        6,
                        "publication",
                        Set.of("campaign-registration", "facebook-campaigns-post", "registro-publicacao")),
                facebookAdsStage(
                        "METRICS_SYNC_TARGET_SELECTION",
                        "metrics-sync-target-selection",
                        "Seleção de campanhas para métricas",
                        7,
                        "metrics",
                        Set.of("metrics-sync-targets", "campaign-metrics-targets", "alvos-metricas")),
                facebookAdsStage(
                        "META_INSIGHTS_COLLECTION",
                        "meta-insights-collection",
                        "Coleta de insights na Meta",
                        8,
                        "metrics",
                        Set.of("campaign-insights", "meta-insights", "coleta-insights")),
                facebookAdsStage(
                        "METRICS_PERSISTENCE",
                        "metrics-persistence",
                        "Persistência das métricas",
                        9,
                        "metrics",
                        Set.of("experiment-campaign-metric", "campaign-metrics-post", "persistencia-metricas")),
                facebookAdsStage(
                        "METRICS_ERROR_HANDLING",
                        "metrics-error-handling",
                        "Tratamento de falhas de métricas",
                        10,
                        "metrics",
                        Set.of("metrics-error", "campaign-metrics-error", "falhas-metricas")));
        return new PipelineDefinition(
                FACEBOOK_ADS_MODULE,
                FACEBOOK_ADS_PUBLICATION_METRICS_PIPELINE_CODE,
                "Pipeline Facebook Ads: Publicação e Métricas",
                FACEBOOK_ADS_CANONICAL_VERSION,
                true,
                Set.of(
                        FACEBOOK_ADS_PUBLICATION_METRICS_PIPELINE_CODE,
                        "facebook_ads_publication_metrics_pipeline",
                        "facebook-ads-pipeline",
                        "facebook-campaign-publication-pipeline"),
                PipelineFieldPolicy.officialDefault(),
                StageFieldPolicy.officialDefault(),
                stages);
    }

    /**
     * Converte uma tarefa do Facebook Ads Worker para definição canônica exibida na tela de pipelines.
     */
    private PipelineStageDefinition facebookAdsStage(
            String canonicalCode, String operationalCode, String name, int position, String workerPackage, Set<String> aliases) {
        return new PipelineStageDefinition(
                canonicalCode,
                operationalCode,
                name,
                position,
                true,
                false,
                true,
                FACEBOOK_ADS_WORKER_MODULE,
                FACEBOOK_ADS_WORKER_ROOT_PACKAGE,
                FACEBOOK_ADS_WORKER_ROOT_PACKAGE + "." + workerPackage,
                aliases);
    }

    /**
     * Converte um código canônico em nome legível para diagnóstico e metadados da tela.
     */
    private String displayName(String code) {
        return Arrays.stream(code.split("-"))
                .map(part -> part.substring(0, 1).toUpperCase(Locale.ROOT) + part.substring(1))
                .collect(Collectors.joining(" "));
    }

    /**
     * Definição oficial de um pipeline protegido pelo contrato operacional.
     */
    public record PipelineDefinition(
            String module,
            String code,
            String name,
            String canonicalVersion,
            boolean official,
            Set<String> aliases,
            PipelineFieldPolicy pipelineFieldPolicy,
            StageFieldPolicy stageFieldPolicy,
            List<PipelineStageDefinition> stages) {
        /**
         * Informa se o código recebido é o código oficial ou um alias permitido.
         */
        public boolean matchesCode(String normalizedCode) {
            return normalizeLocal(code).equals(normalizedCode)
                    || aliases.stream().map(PipelineDefinition::normalizeLocal).anyMatch(normalizedCode::equals);
        }

        /**
         * Retorna um mapa de etapas por posição canônica.
         */
        public Map<Integer, PipelineStageDefinition> stagesByPosition() {
            return stages.stream().collect(Collectors.toUnmodifiableMap(PipelineStageDefinition::position, stage -> stage));
        }

        /**
         * Normaliza códigos locais do record sem depender da instância do registry.
         */
        private static String normalizeLocal(String code) {
            if (!StringUtils.hasText(code)) {
                return "";
            }
            return code.trim().toLowerCase(Locale.ROOT).replace('_', '-');
        }
    }


    /**
     * Política que separa campos estruturais de pipeline dos campos operacionais editáveis.
     */
    public record PipelineFieldPolicy(
            boolean codeStructural,
            boolean moduleStructural,
            boolean nameStructural,
            boolean descriptionOperational,
            boolean activeOperational) {
        /**
         * Retorna a política padrão para pipelines oficiais versionados no código.
         */
        public static PipelineFieldPolicy officialDefault() {
            return new PipelineFieldPolicy(true, true, true, true, true);
        }
    }

    /**
     * Política que separa campos estruturais de etapa dos campos operacionais configuráveis.
     */
    public record StageFieldPolicy(
            boolean codeStructural,
            boolean positionStructural,
            boolean nameStructural,
            boolean requiredStructural,
            boolean descriptionOperational,
            boolean activeOperational,
            boolean openAiModelOperational) {
        /**
         * Retorna a política padrão para etapas oficiais versionadas no código.
         */
        public static StageFieldPolicy officialDefault() {
            return new StageFieldPolicy(true, true, true, true, true, true, true);
        }
    }

    /**
     * Definição oficial de uma etapa canônica para um pipeline protegido.
     */
    public record PipelineStageDefinition(
            String canonicalCode,
            String operationalCode,
            String name,
            int position,
            boolean required,
            boolean requiresOpenAiModel,
            boolean configurable,
            String executionModule,
            String rootPackage,
            String modulePackage,
            Set<String> aliases) {
        /**
         * Informa se o código recebido é o código operacional ou alias da etapa.
         */
        public boolean matchesCode(String normalizedCode) {
            return normalizeLocal(operationalCode).equals(normalizedCode)
                    || normalizeLocal(canonicalCode).equals(normalizedCode)
                    || aliases.stream().map(PipelineStageDefinition::normalizeLocal).anyMatch(normalizedCode::equals);
        }

        /**
         * Normaliza códigos locais do record sem depender da instância do registry.
         */
        private static String normalizeLocal(String code) {
            if (!StringUtils.hasText(code)) {
                return "";
            }
            return code.trim().toLowerCase(Locale.ROOT).replace('_', '-');
        }
    }
}
