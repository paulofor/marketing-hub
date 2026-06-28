package com.marketinghub.pipelines.dossie.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.marketinghub.pipelines.dossie.v1.dossiersynthesis.DossierDossierSynthesisProcessor;
import com.marketinghub.pipelines.dossie.v1.intake.DossierIntakeProcessor;
import com.marketinghub.pipelines.dossie.v1.investigationanchorbuilder.DossierInvestigationAnchorBuilderProcessor;
import com.marketinghub.pipelines.dossie.v1.productunderstanding.DossierProductUnderstandingProcessor;
import com.marketinghub.pipelines.dossie.v1.sourceproductmatch.DossierSourceProductMatchProcessor;
import com.marketinghub.pipelines.dossie.v1.warmupmapbuilder.DossierWarmupMapBuilderProcessor;
import com.marketinghub.pipelines.dossie.v1.warmupresourcediscovery.DossierWarmupResourceDiscoveryProcessor;
import com.marketinghub.pipelines.dossie.v1.warmupsignalextraction.DossierWarmupSignalExtractionProcessor;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Valida o catálogo executável do protocolo padrão módulo para o dossiê de produto v1. */
class PipelineWorkerTest {

    /** Garante que todas as etapas canônicas do dossieproduto.v1 executem pelo núcleo genérico. */
    @Test
    void deveExecutarTodasAsEtapasCanonicasComSaidaEstruturada() {
        List<StageProcessor> processors = processorsCanonicos();
        PipelineWorker worker = new PipelineWorker(processors);

        for (StageProcessor processor : processors) {
            StageContext context = contextFor(processor);

            StageResult result = worker.execute(context);

            assertThat(result.status()).isEqualTo("DONE");
            assertThat(result.output()).containsKey(processor.stageName());
            Object stageOutput = result.output().get(processor.stageName());
            assertThat(stageOutput).hasFieldOrPropertyWithValue("status", "OBJECTIVE_FULFILLED");
            assertThat(stageOutput).hasFieldOrProperty("businessDecision");
            assertThat(String.valueOf(stageOutput)).contains(objetivoEsperado(processor.stageName()));
            assertThat(result.artifacts()).hasSize(1);
            if ("dossier-synthesis".equals(processor.stageName())) {
                assertThat(result.artifacts().get(0).type()).isEqualTo("dossier-synthesis-final-dossier");
                assertThat(result.artifacts().get(0).payload()).contains("O dossiê foi consolidado");
            } else {
                assertThat(result.artifacts().get(0).type()).isEqualTo(processor.stageName() + "-objective");
                assertThat(result.artifacts().get(0).payload()).contains("objective=", "inputAvailable=true");
            }
            assertThat(result.errorMessage()).isNull();
        }
    }

    /** Garante que a síntese final bloqueia conclusão falsa quando recebe apenas metadados operacionais. */
    @Test
    void deveBloquearSinteseFinalSemEvidenciasComerciais() {
        PipelineWorker worker = new PipelineWorker(processorsCanonicos());
        StageContext context = new StageContext(10L, 20L, "workspace-mois", "dossier-synthesis", Map.of(
                "previousStageResponses", List.of("{auditDecision=ok, inputKeys=[jobId], stageExecutionId=20}")));

        StageResult result = worker.execute(context);

        assertThat(result.status()).isEqualTo("FAILED");
        assertThat(result.errorMessage()).contains("Dossiê final bloqueado");
        assertThat(String.valueOf(result.output())).contains("BLOCKED_INSUFFICIENT_CONTEXT");
    }

    /** Garante bloqueio explícito quando o backend enviar etapa sem processor registrado no executor. */
    @Test
    void deveBloquearEtapaSemProcessorRegistrado() {
        PipelineWorker worker = new PipelineWorker(processorsCanonicos());
        StageContext context = new StageContext(10L, 20L, "workspace-mois", "etapa-sem-contrato", Map.of());

        assertThatThrownBy(() -> worker.execute(context))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Etapa de dossiê não suportada");
    }

    /** Monta contexto suficiente para cada processor cumprir seu contrato funcional. */
    private StageContext contextFor(StageProcessor processor) {
        if ("dossier-synthesis".equals(processor.stageName())) {
            return new StageContext(10L, 20L, "workspace-mois", processor.stageName(), Map.of(
                    "previousStageResponses", List.of(
                            "Produto resolve dor clara do público com promessa específica.",
                            "Evidência externa indica prova social e autoridade.",
                            "Recomendação: avançar com risco controlado.")));
        }
        return new StageContext(10L, 20L, "workspace-mois", processor.stageName(), Map.of("origem", "teste"));
    }

    /** Devolve trecho do objetivo que deve aparecer na saída funcional de cada etapa. */
    private String objetivoEsperado(String stageName) {
        return switch (stageName) {
            case "intake" -> "contexto mínimo";
            case "product-understanding" -> "produto, público, dor";
            case "investigation-anchor-builder" -> "âncoras confiáveis";
            case "warmup-resource-discovery" -> "provas sociais que aquecem";
            case "source-product-match" -> "fonte externa";
            case "warmup-signal-extraction" -> "sinais de autoridade";
            case "warmup-map-builder" -> "Organizar os recursos externos";
            case "dossier-synthesis" -> "recomendação final";
            default -> throw new IllegalArgumentException("Etapa sem objetivo esperado: " + stageName);
        };
    }

    /** Monta o catálogo mínimo de processors esperados para o dossiê de produto v1. */
    private List<StageProcessor> processorsCanonicos() {
        return List.of(
                new DossierIntakeProcessor(),
                new DossierProductUnderstandingProcessor(),
                new DossierInvestigationAnchorBuilderProcessor(),
                new DossierWarmupResourceDiscoveryProcessor(),
                new DossierSourceProductMatchProcessor(),
                new DossierWarmupSignalExtractionProcessor(),
                new DossierWarmupMapBuilderProcessor(),
                new DossierDossierSynthesisProcessor());
    }
}
