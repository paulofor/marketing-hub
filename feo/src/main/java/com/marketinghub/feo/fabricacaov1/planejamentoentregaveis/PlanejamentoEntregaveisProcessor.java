package com.marketinghub.feo.fabricacaov1.planejamentoentregaveis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.feo.fabricacaov1.contract.DeliverablePlan;
import com.marketinghub.feo.fabricacaov1.contract.DeliverableSpec;
import com.marketinghub.feo.fabricacaov1.contract.FabricationContext;
import com.marketinghub.feo.fabricacaov1.pipeline.StageArtifact;
import com.marketinghub.feo.fabricacaov1.pipeline.StageCode;
import com.marketinghub.feo.fabricacaov1.pipeline.StageContext;
import com.marketinghub.feo.fabricacaov1.pipeline.StageProcessor;
import com.marketinghub.feo.fabricacaov1.pipeline.StageResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Planeja entregaveis digitais profissionais a partir da oferta validada.
 */
@Component
public class PlanejamentoEntregaveisProcessor implements StageProcessor<FabricationContext, DeliverablePlan> {

    private final ObjectMapper objectMapper;

    /**
     * Recebe serializador para publicar plano auditavel.
     */
    public PlanejamentoEntregaveisProcessor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Retorna a etapa canonica de planejamento dos entregaveis.
     */
    @Override
    public StageCode stageCode() {
        return StageCode.PLANEJAMENTO_ENTREGAVEIS;
    }

    /**
     * Cria um plano de pacote final sem alterar promessa, nicho ou mecanismo validados.
     */
    @Override
    public StageResult<DeliverablePlan> process(StageContext<FabricationContext> context) {
        FabricationContext input = context.input();
        List<String> missing = requiredMissing(input);
        if (!missing.isEmpty()) {
            return StageResult.blocked("Contexto FEO incompleto: " + String.join(", ", missing), List.of());
        }

        DeliverablePlan plan = new DeliverablePlan(
                input.requestId(),
                "Pacote Final - " + input.offerName(),
                buildSpecs(input));
        StageArtifact artifact = context.artifactStore().store(
                "FEO_OFFER_DELIVERABLE_PLAN",
                "feo-offer-deliverable-plan.json",
                "application/json",
                toJson(plan));
        return StageResult.completedWithNext(
                plan,
                List.of(artifact),
                Map.of("deliverableCount", plan.deliverables().size(), "qualityGate", "PLAN_READY"),
                StageCode.MONTAGEM_PACOTE);
    }

    /**
     * Verifica campos obrigatorios para preservar a promessa validada.
     */
    private List<String> requiredMissing(FabricationContext input) {
        List<String> missing = new ArrayList<>();
        if (isBlank(input.offerName())) {
            missing.add("offerName");
        }
        if (isBlank(input.centralPromise())) {
            missing.add("centralPromise");
        }
        if (isBlank(input.promisedResult())) {
            missing.add("promisedResult");
        }
        if (isBlank(input.coreMechanism())) {
            missing.add("coreMechanism");
        }
        if (input.deliverables() == null || input.deliverables().isEmpty()) {
            missing.add("deliverables");
        }
        return missing;
    }

    /**
     * Monta especificacoes de entregaveis com formatos de produto final.
     */
    private List<DeliverableSpec> buildSpecs(FabricationContext input) {
        List<DeliverableSpec> specs = new ArrayList<>();
        int index = 1;
        for (String deliverable : input.deliverables()) {
            String code = "ENT-" + String.format("%02d", index);
            specs.add(new DeliverableSpec(
                    code,
                    deliverable,
                    inferFormat(deliverable),
                    roleFor(index, input),
                    String.valueOf(index),
                    List.of(
                            "Conecta explicitamente com a promessa validada",
                            "Entrega acao pratica sem depender de explicacao tecnica",
                            "Pode ser consumido pelo cliente final sem ler logs ou markdown cru",
                            "Mantem limites da promessa e nao cria claims novos"),
                    List.of(
                            "Objetivo do entregavel",
                            "Quando usar",
                            "Passo a passo",
                            "Modelo preenchivel",
                            "Criterio de conclusao")));
            index++;
        }
        return specs;
    }

    /**
     * Infere o formato final mais apropriado pelo nome do entregavel.
     */
    private String inferFormat(String deliverable) {
        String lower = deliverable.toLowerCase();
        if (lower.contains("planilha") || lower.contains("calculadora") || lower.contains("mapa")) {
            return "CSV_PLANILHA";
        }
        if (lower.contains("checklist") || lower.contains("roteiro") || lower.contains("guia") || lower.contains("plano")) {
            return "PDF";
        }
        return "HTML_PDF";
    }

    /**
     * Define o papel comercial de cada entregavel no pacote.
     */
    private String roleFor(int index, FabricationContext input) {
        if (index == 1) {
            return "Ativo principal para gerar a primeira percepcao de valor: " + input.promisedResult();
        }
        if (index == 2) {
            return "Reduz esforco de aplicacao e remove friccao operacional.";
        }
        return "Aumenta profundidade percebida sem alterar a promessa validada.";
    }

    /**
     * Serializa plano como JSON auditavel.
     */
    private byte[] toJson(DeliverablePlan plan) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(plan);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Falha ao serializar plano FEO", ex);
        }
    }

    /**
     * Indica se um texto obrigatorio esta ausente.
     */
    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
