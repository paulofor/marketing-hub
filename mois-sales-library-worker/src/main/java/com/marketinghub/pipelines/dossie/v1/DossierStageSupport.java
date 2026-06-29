package com.marketinghub.pipelines.dossie.v1;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/** Centraliza a montagem de evidências funcionais comuns às etapas do dossiê MOIS v1. */
public final class DossierStageSupport {

    /** Impede instanciação de classe utilitária. */
    private DossierStageSupport() {}

    /** Monta evidências objetivas da etapa a partir do contexto recebido do backend. */
    public static Map<String, Object> evidenceFor(StageContext context, String stageName) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("stageName", stageName);
        evidence.put("stageExecutionId", context.stageExecutionId());
        evidence.put("dossierId", context.dossierId());
        evidence.put("workspaceId", context.workspaceId());
        evidence.put("inputKeys", context.input() == null ? java.util.List.of() : context.input().keySet().stream().sorted().toList());
        evidence.put("inputAvailable", context.input() != null && !context.input().isEmpty());
        evidence.put("commercialContextQuality", commercialContextQuality(context));
        evidence.put("commercialSignalsFound", commercialSignalsFound(context));
        evidence.put("stageGuidance", stageGuidance(stageName));
        evidence.put("auditDecision", "Registrar saída estruturada suficiente para o backend persistir relatório e avançar a próxima etapa.");
        return Map.copyOf(evidence);
    }

    /** Classifica a riqueza comercial do contexto recebido para evitar conclusões fortes com matéria-prima fraca. */
    private static String commercialContextQuality(StageContext context) {
        int signals = commercialSignalsFound(context);
        if (signals >= 5) {
            return "RICA";
        }
        if (signals >= 2) {
            return "PARCIAL";
        }
        return "INSUFICIENTE";
    }

    /** Conta sinais comerciais presentes no contexto operacional enviado pelo backend. */
    private static int commercialSignalsFound(StageContext context) {
        if (context.input() == null || context.input().isEmpty()) {
            return 0;
        }
        String flattened = context.input().toString().toLowerCase(java.util.Locale.ROOT);
        return (int) java.util.List.of(
                        "headline",
                        "promessa",
                        "produto",
                        "produtor",
                        "marca",
                        "preço",
                        "preco",
                        "garantia",
                        "cta",
                        "depoimento",
                        "fonte",
                        "prova",
                        "mecanismo",
                        "obje",
                        "review")
                .stream()
                .filter(flattened::contains)
                .count();
    }

    /** Expõe critérios de negócio específicos da etapa para orientar relatório, auditoria e próximos passos. */
    public static Map<String, Object> stageGuidance(String stageName) {
        return switch (stageName) {
            case "intake" -> Map.of(
                    "expectedDecision", "Triar a página como RICA, PARCIAL ou INSUFICIENTE antes de avançar.",
                    "mustPreserve", java.util.List.of("headline", "promessa", "preço", "garantia", "CTA", "produtor", "provas", "links externos"));
            case "investigation-anchor-builder" -> Map.of(
                    "expectedDecision", "Gerar âncoras por intenção de investigação e priorizar vínculo real com produto/produtor/marca.",
                    "anchorTypes", java.util.List.of("produto exato", "produtor", "marca", "reviews", "reclamações", "lives", "afiliados", "mecanismo proprietário"));
            case "warmup-resource-discovery" -> Map.of(
                    "expectedDecision", "Mapear canais que aquecem a decisão, não apenas links com palavra parecida.",
                    "commercialRoles", java.util.List.of("descoberta", "educação", "autoridade", "prova social", "objeção", "demonstração", "oferta direta"));
            case "source-product-match" -> Map.of(
                    "expectedDecision", "Separar evidência direta, provável, indireta, nicho apenas e descartada.",
                    "matchCriteria", java.util.List.of("nome exato", "domínio", "produtor", "marca", "CTA", "checkout", "depoimento", "promessa específica"));
            case "warmup-signal-extraction" -> Map.of(
                    "expectedDecision", "Extrair sinais de persuasão conectados à fonte que sustenta cada conclusão.",
                    "signalGroups", java.util.List.of("dor", "promessa", "mecanismo", "prova", "autoridade", "objeções", "distribuição", "linguagem do público"));
            case "warmup-map-builder" -> Map.of(
                    "expectedDecision", "Transformar sinais em jornada de aquecimento e matriz de força comercial.",
                    "scoreDimensions", java.util.List.of("demanda", "clareza da promessa", "mecanismo", "prova", "distribuição", "objeções", "facilidade de compra"));
            case "dossier-synthesis" -> Map.of(
                    "expectedDecision", "Concluir como FORTE, PROMISSOR_COM_LACUNAS ou FRACO_OU_INSUFICIENTE com evidências e próximos testes.",
                    "mustSeparate", java.util.List.of("sucesso observado", "hipóteses pendentes", "o que adaptar", "o que não copiar"));
            default -> Map.of(
                    "expectedDecision", "Aplicar Dor → Resultado → Mecanismo → Prova → Oferta com evidência rastreável.");
        };
    }

    /** Cria artefato auditável do objetivo executado sem contaminar o artefato final publicável. */
    public static StageArtifact objectiveArtifact(
            StageContext context, String stageName, String objective, Map<String, Object> evidence) {
        String payload = "stage=" + stageName
                + ";dossierId=" + context.dossierId()
                + ";objective=" + objective
                + ";inputAvailable=" + evidence.get("inputAvailable");
        return new StageArtifact(stageName + "-objective", "dossie/v1/" + context.dossierId() + "/" + stageName, payload, Instant.now());
    }
}
