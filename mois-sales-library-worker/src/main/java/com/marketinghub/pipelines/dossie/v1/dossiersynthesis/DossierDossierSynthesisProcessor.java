package com.marketinghub.pipelines.dossie.v1.dossiersynthesis;

import com.marketinghub.pipelines.dossie.v1.DossierStageSupport;
import com.marketinghub.pipelines.dossie.v1.StageArtifact;
import com.marketinghub.pipelines.dossie.v1.StageContext;
import com.marketinghub.pipelines.dossie.v1.StageProcessor;
import com.marketinghub.pipelines.dossie.v1.StageResult;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Executa a etapa síntese final do dossiê gerando uma entrega de negócio ou bloqueando saída fraca. */
public class DossierDossierSynthesisProcessor implements StageProcessor {

    private static final String STAGE_NAME = "dossier-synthesis";
    private static final String OBJECTIVE = "Consolidar relatório executivo com recomendação final com classificação FORTE, PROMISSOR_COM_LACUNAS ou FRACO_OU_INSUFICIENTE, separando evidências, hipóteses pendentes, oportunidades acionáveis, riscos e próximos testes comerciais.";
    private static final String INSUFFICIENT_CONTEXT_ERROR = "Dossiê final bloqueado: as etapas anteriores não entregaram evidências comerciais suficientes para gerar conclusão, recomendação e próximos passos úteis.";

    /** Informa o nome canônico da etapa síntese final do dossiê. */
    @Override
    public String stageName() {
        return STAGE_NAME;
    }

    /** Produz o dossiê final quando há evidência suficiente ou falha para impedir conclusão vazia. */
    @Override
    public StageResult process(StageContext context) {
        Map<String, Object> auditEvidence = DossierStageSupport.evidenceFor(context, STAGE_NAME);
        List<String> stageResponses = stageResponses(context);
        BusinessEvidenceGate gate = evaluateBusinessEvidence(stageResponses);
        Map<String, Object> enrichedEvidence = enrichedEvidence(auditEvidence, gate);
        if (!gate.approved()) {
            DossierDossierSynthesisOutput blockedOutput = new DossierDossierSynthesisOutput(
                    context.dossierId(),
                    "BLOCKED_INSUFFICIENT_CONTEXT",
                    OBJECTIVE,
                    "Não foi possível gerar um dossiê comercial confiável porque as etapas anteriores não provaram todos os pilares mínimos: produto/público, dor-promessa-mecanismo, prova, fonte/aquecimento, oferta/risco e recomendação.",
                    stageResponses,
                    List.of(
                            "Reexecutar o dossiê após corrigir as etapas anteriores para retornarem evidências comerciais estruturadas e rastreáveis.",
                            "Garantir que a etapa final receba produto, público, promessa, mecanismo, prova, fontes qualificadas, sinais de aquecimento, oferta/risco e recomendação.",
                            "Não usar este resultado para decisão de oferta enquanto o gate estiver bloqueado."),
                    "BLOCKED",
                    enrichedEvidence);
            return StageResult.failed(
                    INSUFFICIENT_CONTEXT_ERROR,
                    Map.of(STAGE_NAME, blockedOutput),
                    List.of(finalArtifact(context, blockedOutput)));
        }

        DossierDossierSynthesisOutput output = new DossierDossierSynthesisOutput(
                context.dossierId(),
                "OBJECTIVE_FULFILLED",
                OBJECTIVE,
                "O dossiê foi consolidado com base nas evidências comerciais recebidas das etapas anteriores. Use as evidências e próximos passos para decidir se o produto merece nova oferta, aquecimento ou descarte.",
                stageResponses,
                List.of(
                        "Validar as evidências principais antes de investir mídia.",
                        "Transformar os sinais de dor, prova e mecanismo em hipótese de oferta.",
                        "Registrar decisão comercial no relatório da página."),
                "APPROVED",
                enrichedEvidence);
        return StageResult.done(Map.of(STAGE_NAME, output), List.of(finalArtifact(context, output)));
    }

    /** Extrai do contexto as respostas anteriores entregues pelo backend para a síntese final. */
    @SuppressWarnings("unchecked")
    private List<String> stageResponses(StageContext context) {
        Object previousStageResponses = context.input() == null ? null : context.input().get("previousStageResponses");
        if (previousStageResponses instanceof List<?> responses) {
            return responses.stream()
                    .map(String::valueOf)
                    .filter(response -> !response.isBlank())
                    .toList();
        }
        Object previousStages = context.input() == null ? null : context.input().get("previousStages");
        if (previousStages instanceof Map<?, ?> stages) {
            List<String> responses = new ArrayList<>();
            stages.values().forEach(value -> {
                if (value instanceof Map<?, ?> stage && stage.get("response") != null) {
                    responses.add(String.valueOf(stage.get("response")));
                }
            });
            return responses;
        }
        return List.of();
    }

    /** Avalia se as respostas anteriores sustentam conclusão comercial comparável e acionável. */
    private BusinessEvidenceGate evaluateBusinessEvidence(List<String> stageResponses) {
        if (stageResponses.size() < 4) {
            return new BusinessEvidenceGate(false, List.of(), List.of("historico_minimo_de_etapas"));
        }
        String joined = String.join(" ", stageResponses).toLowerCase();
        List<String> matchedCategories = requiredCommercialCategories().entrySet().stream()
                .filter(entry -> entry.getValue().stream().anyMatch(joined::contains))
                .map(Map.Entry::getKey)
                .toList();
        List<String> missingCategories = requiredCommercialCategories().keySet().stream()
                .filter(category -> !matchedCategories.contains(category))
                .toList();
        boolean approved = matchedCategories.size() >= 6
                && matchedCategories.containsAll(Set.of("produto_publico", "dor_promessa_mecanismo", "prova", "fonte_aquecimento", "recomendacao"));
        return new BusinessEvidenceGate(approved, matchedCategories, missingCategories);
    }

    /** Define os pilares mínimos que impedem um dossiê operacional genérico de virar conclusão comercial. */
    private Map<String, List<String>> requiredCommercialCategories() {
        return Map.of(
                "produto_publico", List.of("produto", "público", "publico", "persona", "cliente", "produtor", "marca"),
                "dor_promessa_mecanismo", List.of("dor", "promessa", "mecanismo", "resultado", "transformação", "transformacao"),
                "prova", List.of("prova", "evidência", "evidencia", "depoimento", "autoridade", "case", "review"),
                "fonte_aquecimento", List.of("fonte", "canal", "youtube", "instagram", "afiliado", "review", "aquecimento", "comunidade"),
                "oferta_risco", List.of("oferta", "preço", "preco", "garantia", "checkout", "cta", "risco", "objeção", "objecao"),
                "recomendacao", List.of("recomendação", "recomendacao", "próximo teste", "proximo teste", "avançar", "avancar", "descartar"),
                "oportunidade_adaptacao", List.of("adaptar", "oportunidade", "criativo", "funil", "landing", "campanha"));
    }

    /** Enriquece a evidência de auditoria com o resultado objetivo do gate comercial. */
    private Map<String, Object> enrichedEvidence(Map<String, Object> auditEvidence, BusinessEvidenceGate gate) {
        Map<String, Object> enriched = new LinkedHashMap<>(auditEvidence);
        enriched.put("commercialEvidenceGateApproved", gate.approved());
        enriched.put("commercialEvidenceCategoriesFound", gate.matchedCategories());
        enriched.put("commercialEvidenceCategoriesMissing", gate.missingCategories());
        return Map.copyOf(enriched);
    }

    /** Cria o artefato final separado dos metadados técnicos de auditoria. */
    private StageArtifact finalArtifact(StageContext context, DossierDossierSynthesisOutput output) {
        return new StageArtifact(
                STAGE_NAME + "-final-dossier",
                "dossie/v1/" + context.dossierId() + "/" + STAGE_NAME + "/final",
                output.finalConclusion(),
                Instant.now());
    }

    /** Representa o resultado do gate comercial aplicado antes da síntese final. */
    private record BusinessEvidenceGate(boolean approved, List<String> matchedCategories, List<String> missingCategories) {
    }
}
