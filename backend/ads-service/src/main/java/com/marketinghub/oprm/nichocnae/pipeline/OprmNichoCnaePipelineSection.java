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
            "com.marketinghub.oprm.nichocnae.routineresearchorchestrator",
            Set.of("routineresearchorchestrator", "oprmRoutineResearchOrchestrator")),
    ROUTINE_RESEARCH_CYCLE(
            2,
            "routine-research-cycle",
            "Controle do ciclo de rotina",
            "com.marketinghub.oprm.nichocnae.routineresearchcycle",
            Set.of("routineresearchcycle", "oprmRoutineResearchCycle")),
    NICHE_RESEARCH_SEED_BUILDER(
            3,
            "niche-research-seed-builder",
            "Seed e queries de pesquisa",
            "com.marketinghub.oprm.nichocnae.nicheresearchseedbuilder",
            Set.of("nicheresearchseedbuilder", "oprmNicheResearchSeedBuilder")),
    SOURCE_SEARCHER(
            4,
            "source-searcher",
            "Busca de fontes públicas",
            "com.marketinghub.oprm.nichocnae.sourcesearcher",
            Set.of("sourcesearcher", "oprmSourceSearcher")),
    SOURCE_FETCHER(
            5,
            "source-fetcher",
            "Coleta curta de fontes",
            "com.marketinghub.oprm.nichocnae.sourcefetcher",
            Set.of("sourcefetcher", "oprmSourceFetcher")),
    SIGNAL_EXTRACTOR(
            6,
            "signal-extractor",
            "Extração de sinais de rotina",
            "com.marketinghub.oprm.nichocnae.signalextractor",
            Set.of("signalextractor", "oprmSignalExtractor")),
    ROUTINE_SYNTHESIZER(
            7,
            "routine-synthesizer",
            "Síntese do cartão de rotina",
            "com.marketinghub.oprm.nichocnae.routinesynthesizer",
            Set.of("routinesynthesizer", "oprmRoutineSynthesizer")),
    ROUTINE_QUALITY_GATE(
            8,
            "routine-quality-gate",
            "Gate de qualidade da rotina",
            "com.marketinghub.oprm.nichocnae.routinequalitygate",
            Set.of("routinequalitygate", "oprmRoutineQualityGate")),
    ENRICHED_NICHE_MATERIALIZER(
            9,
            "enriched-niche-materializer",
            "Materialização do nicho enriquecido",
            "com.marketinghub.oprm.nichocnae.enrichednichematerializer",
            Set.of("enrichednichematerializer", "oprmEnrichedNicheMaterializer"));

    private static final String COLLECTOR_EXECUTION_MODULE = "oprm-coletor-mei";

    private final int position;
    private final String path;
    private final String displayName;
    private final String rootPackage;
    private final Set<String> aliases;

    /**
     * Inicializa uma etapa oficial com posição, código operacional e localização de implementação.
     */
    OprmNichoCnaePipelineSection(
            int position,
            String path,
            String displayName,
            String rootPackage,
            Set<String> aliases) {
        this.position = position;
        this.path = path;
        this.displayName = displayName;
        this.rootPackage = rootPackage;
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
