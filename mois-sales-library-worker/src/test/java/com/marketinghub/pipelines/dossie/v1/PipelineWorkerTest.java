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
            StageContext context = new StageContext(
                    10L, 20L, "workspace-mois", processor.stageName(), Map.of("origem", "teste"));

            StageResult result = worker.execute(context);

            assertThat(result.status()).isEqualTo("DONE");
            assertThat(result.output()).containsKey(processor.stageName());
            assertThat(result.artifacts()).isNotNull();
            assertThat(result.errorMessage()).isNull();
        }
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
