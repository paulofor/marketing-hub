package com.marketinghub.oprm.nichocnae.pipeline;

import java.util.Locale;
import java.util.Set;

/**
 * Define as etapas oficiais do pipeline OPRM NichoCNAE já implementadas no backend e no coletor.
 */
public enum OprmNichoCnaePipelineSection {
    ROUTINE_RESEARCH_ORCHESTRATOR(
            1,
            "routine-research-orchestrator",
            "Seleção de candidato CNAE",
            false,
            "com.marketinghub.oprm.nichocnae.routineresearchorchestrator",
            Set.of("routineresearchorchestrator", "oprmRoutineResearchOrchestrator")),
    ROUTINE_RESEARCH_CYCLE(
            2,
            "routine-research-cycle",
            "Controle do ciclo de rotina",
            false,
            "com.marketinghub.oprm.nichocnae.routineresearchcycle",
            Set.of("routineresearchcycle", "oprmRoutineResearchCycle")),
    NICHE_RESEARCH_SEED_BUILDER(
            3,
            "niche-research-seed-builder",
            "Seed e queries de pesquisa",
            true,
            "com.marketinghub.oprm.nichocnae.nicheresearchseedbuilder",
            Set.of("nicheresearchseedbuilder", "oprmNicheResearchSeedBuilder")),
    SOURCE_SEARCHER(
            4,
            "source-searcher",
            "Busca de fontes públicas",
            false,
            "com.marketinghub.oprm.nichocnae.sourcesearcher",
            Set.of("sourcesearcher", "oprmSourceSearcher")),
    SOURCE_FETCHER(
            5,
            "source-fetcher",
            "Coleta curta de fontes",
            false,
            "com.marketinghub.oprm.nichocnae.sourcefetcher",
            Set.of("sourcefetcher", "oprmSourceFetcher")),
    SIGNAL_EXTRACTOR(
            6,
            "signal-extractor",
            "Extração de sinais de rotina",
            false,
            "com.marketinghub.oprm.nichocnae.signalextractor",
            Set.of("signalextractor", "oprmSignalExtractor")),
    ROUTINE_SYNTHESIZER(
            7,
            "routine-synthesizer",
            "Síntese do cartão de rotina",
            false,
            "com.marketinghub.oprm.nichocnae.routinesynthesizer",
            Set.of("routinesynthesizer", "oprmRoutineSynthesizer")),
    MEI_AUDIENCE_SEGMENTER(
            8,
            "mei-audience-segmenter",
            "Segmentação comportamental MEI/autônomo",
            true,
            "com.marketinghub.oprm.nichocnae.meiaudiencesegmenter",
            Set.of("meiaudiencesegmenter", "oprmMeiAudienceSegmenter", "mei")),
    ROUTINE_QUALITY_GATE(
            9,
            "routine-quality-gate",
            "Gate de qualidade da rotina",
            false,
            "com.marketinghub.oprm.nichocnae.routinequalitygate",
            Set.of("routinequalitygate", "oprmRoutineQualityGate")),
    ENRICHED_NICHE_MATERIALIZER(
            10,
            "enriched-niche-materializer",
            "Materialização do nicho enriquecido",
            false,
            "com.marketinghub.oprm.nichocnae.enrichednichematerializer",
            Set.of("enrichednichematerializer", "oprmEnrichedNicheMaterializer"));

    private static final String COLLECTOR_EXECUTION_MODULE = "oprm-coletor-mei";

    private final int position;
    private final String path;
    private final String displayName;
    private final boolean requiresOpenAiModel;
    private final String rootPackage;
    private final String modulePackage;
    private final Set<String> aliases;

    /**
     * Inicializa uma etapa oficial com posição, código operacional e localização de implementação.
     */
    OprmNichoCnaePipelineSection(
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
        this.modulePackage =
                rootPackage.replace("com.marketinghub.oprm.nichocnae", "com.marketinghub.nichocnae");
        this.aliases = aliases;
    }

    /**
     * Retorna a posição canônica da etapa no fluxo NichoCNAE.
     */
    public int position() {
        return position;
    }

    /**
     * Retorna o código operacional usado pela tela e pela configuração do pipeline.
     */
    public String path() {
        return path;
    }

    /**
     * Retorna o nome legível da etapa para diagnóstico e tela administrativa.
     */
    public String displayName() {
        return displayName;
    }

    /**
     * Informa se a etapa consome diretamente um modelo OpenAI configurável.
     */
    public boolean requiresOpenAiModel() {
        return requiresOpenAiModel;
    }

    /**
     * Retorna o módulo externo que executa a etapa via APIs do backend OPRM.
     */
    public String executionModule() {
        return COLLECTOR_EXECUTION_MODULE;
    }

    /**
     * Retorna o pacote backend que define o contrato e a persistência da etapa.
     */
    public String rootPackage() {
        return rootPackage;
    }

    /**
     * Retorna o pacote no módulo executor que implementa a etapa.
     */
    public String modulePackage() {
        return modulePackage;
    }

    /**
     * Retorna os aliases aceitos para compatibilidade com nomes de pacote e nomes canônicos de etapa.
     */
    public Set<String> aliases() {
        return aliases;
    }

    /**
     * Converte um código operacional ou alias em etapa oficial.
     */
    public static OprmNichoCnaePipelineSection fromCode(String raw) {
        String normalized = normalize(raw);
        for (OprmNichoCnaePipelineSection section : values()) {
            if (normalize(section.path).equals(normalized)
                    || normalize(section.name()).equals(normalized)
                    || section.aliases.stream().map(OprmNichoCnaePipelineSection::normalize).anyMatch(normalized::equals)) {
                return section;
            }
        }
        throw new IllegalArgumentException("Unknown OPRM NichoCNAE pipeline section: " + raw);
    }

    /**
     * Normaliza códigos de etapa para comparação sem variações de caixa ou separador.
     */
    private static String normalize(String code) {
        if (code == null || code.isBlank()) {
            return "";
        }
        return code.trim().toLowerCase(Locale.ROOT).replace('_', '-');
    }
}
