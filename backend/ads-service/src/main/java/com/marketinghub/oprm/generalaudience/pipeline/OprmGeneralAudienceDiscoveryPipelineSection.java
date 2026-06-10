package com.marketinghub.oprm.generalaudience.pipeline;

import java.util.Locale;
import java.util.Set;

/** Define as etapas oficiais do pipeline OPRM de descoberta de públicos gerais separado do NichoCNAE. */
public enum OprmGeneralAudienceDiscoveryPipelineSection {
    GENERAL_AUDIENCE_SEED_REVIEW(
            1,
            "general-audience-seed-review",
            "Revisão da semente geral",
            false,
            "com.marketinghub.oprm.generalaudience",
            Set.of("seed-review", "oprmGeneralAudienceSeedReview")),
    GENERAL_AUDIENCE_SUBNICHE_DISCOVERY(
            2,
            "general-audience-subniche-discovery",
            "Descoberta de subnichos",
            true,
            "com.marketinghub.oprm.generalaudience.service",
            Set.of("subniche-discovery", "oprmGeneralAudienceSubnicheDiscovery")),
    GENERAL_AUDIENCE_PAIN_MAPPING(
            3,
            "general-audience-pain-mapping",
            "Mapeamento de dores e linguagem",
            true,
            "com.marketinghub.oprm.generalaudience.service",
            Set.of("pain-mapping", "oprmGeneralAudiencePainMapping")),
    GENERAL_AUDIENCE_ANGLE_BUILDER(
            4,
            "general-audience-angle-builder",
            "Construção de ângulos seguros",
            true,
            "com.marketinghub.oprm.generalaudience.service",
            Set.of("angle-builder", "oprmGeneralAudienceAngleBuilder")),
    GENERAL_AUDIENCE_QUALITY_GATE(
            5,
            "general-audience-quality-gate",
            "Quality gate de público geral",
            false,
            "com.marketinghub.oprm.generalaudience.service",
            Set.of("quality-gate", "oprmGeneralAudienceQualityGate")),
    GENERAL_AUDIENCE_EXPERIMENT_BRIEF(
            6,
            "general-audience-experiment-brief",
            "Brief de experimento de lead",
            false,
            "com.marketinghub.oprm.generalaudience.service",
            Set.of("experiment-brief", "oprmGeneralAudienceExperimentBrief"));

    private static final String EXECUTION_MODULE = "backend";
    private final int position;
    private final String path;
    private final String displayName;
    private final boolean requiresOpenAiModel;
    private final String rootPackage;
    private final Set<String> aliases;

    /** Inicializa a etapa oficial de descoberta de público geral. */
    OprmGeneralAudienceDiscoveryPipelineSection(
            int position,
            String path,
            String displayName,
            boolean requiresOpenAiModel,
            String rootPackage,
            Set<String> aliases) {
        this.position = position;
        this.path = path;
        this.displayName = displayName;
        this.requiresOpenAiModel = requiresOpenAiModel;
        this.rootPackage = rootPackage;
        this.aliases = aliases;
    }

    /** Retorna a posição canônica da etapa. */
    public int position() { return position; }

    /** Retorna o código operacional da etapa. */
    public String path() { return path; }

    /** Retorna o nome legível da etapa. */
    public String displayName() { return displayName; }

    /** Informa se a etapa pode consumir modelo OpenAI em automação futura. */
    public boolean requiresOpenAiModel() { return requiresOpenAiModel; }

    /** Retorna o módulo executor protegido pelo backend principal. */
    public String executionModule() { return EXECUTION_MODULE; }

    /** Retorna o pacote backend responsável pela etapa. */
    public String rootPackage() { return rootPackage; }

    /** Retorna o pacote do módulo executor, sem criar acesso direto fora do backend. */
    public String modulePackage() { return rootPackage; }

    /** Retorna aliases aceitos para localizar a etapa oficial. */
    public Set<String> aliases() { return aliases; }

    /** Converte código operacional ou alias em etapa oficial. */
    public static OprmGeneralAudienceDiscoveryPipelineSection fromCode(String raw) {
        String normalized = normalize(raw);
        for (OprmGeneralAudienceDiscoveryPipelineSection section : values()) {
            if (normalize(section.path).equals(normalized)
                    || normalize(section.name()).equals(normalized)
                    || section.aliases.stream().map(OprmGeneralAudienceDiscoveryPipelineSection::normalize).anyMatch(normalized::equals)) {
                return section;
            }
        }
        throw new IllegalArgumentException("Unknown OPRM general audience discovery section: " + raw);
    }

    /** Normaliza códigos de etapa para comparação tolerante a caixa e separadores. */
    private static String normalize(String code) {
        if (code == null || code.isBlank()) {
            return "";
        }
        return code.trim().toLowerCase(Locale.ROOT).replace('_', '-');
    }
}
