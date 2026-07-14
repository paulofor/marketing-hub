package com.marketinghub.feo.fabricacaov1;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.feo.fabricacaov1.contract.DeliverablePlan;
import com.marketinghub.feo.fabricacaov1.contract.FabricationContext;
import com.marketinghub.feo.fabricacaov1.contract.PackageAssemblyInput;
import com.marketinghub.feo.fabricacaov1.contract.PackageAssemblyOutput;
import com.marketinghub.feo.fabricacaov1.montagempacote.MontagemPacoteProcessor;
import com.marketinghub.feo.fabricacaov1.montagempacote.PackageAssetAssembler;
import com.marketinghub.feo.fabricacaov1.pipeline.InMemoryArtifactStore;
import com.marketinghub.feo.fabricacaov1.pipeline.StageCode;
import com.marketinghub.feo.fabricacaov1.pipeline.StageContext;
import com.marketinghub.feo.fabricacaov1.pipeline.StageExecution;
import com.marketinghub.feo.fabricacaov1.pipeline.StageResult;
import com.marketinghub.feo.fabricacaov1.pipeline.StageStatus;
import com.marketinghub.feo.fabricacaov1.planejamentoentregaveis.PlanejamentoEntregaveisProcessor;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Valida contratos funcionais minimos da FEO v1.
 */
class FeoFabricacaoV1ContractTest {

    /**
     * Confirma que o planejamento cria proxima etapa contratada e artefato auditavel.
     */
    @Test
    void planejamentoDeveGerarPlanoComProximaEtapaContratada() {
        PlanejamentoEntregaveisProcessor processor = new PlanejamentoEntregaveisProcessor(new ObjectMapper());
        StageExecution<FabricationContext> execution = new StageExecution<>(
                "job-1",
                "exec-1",
                StageCode.PLANEJAMENTO_ENTREGAVEIS,
                FabricationContext.sample(),
                Map.of());

        StageResult<DeliverablePlan> result = processor.process(new StageContext<>(
                execution,
                execution.input(),
                new InMemoryArtifactStore()));

        assertThat(result.status()).isEqualTo(StageStatus.COMPLETED);
        assertThat(result.nextStageCode()).isEqualTo(StageCode.MONTAGEM_PACOTE);
        assertThat(result.output().deliverables()).hasSizeGreaterThanOrEqualTo(3);
        assertThat(result.artifacts()).extracting("name").contains("feo-offer-deliverable-plan.json");
    }

    /**
     * Confirma que a montagem gera PDF, planilha CSV e ZIP de produto final.
     */
    @Test
    void montagemDeveGerarPdfPlanilhaEZip() {
        PlanejamentoEntregaveisProcessor planejamento = new PlanejamentoEntregaveisProcessor(new ObjectMapper());
        FabricationContext context = FabricationContext.sample();
        DeliverablePlan plan = planejamento.process(new StageContext<>(
                        new StageExecution<>("job-1", "exec-1", StageCode.PLANEJAMENTO_ENTREGAVEIS, context, Map.of()),
                        context,
                        new InMemoryArtifactStore()))
                .output();
        MontagemPacoteProcessor processor = new MontagemPacoteProcessor(new PackageAssetAssembler());
        PackageAssemblyInput input = new PackageAssemblyInput(context, plan);

        StageResult<PackageAssemblyOutput> result = processor.process(new StageContext<>(
                new StageExecution<>("job-1", "exec-2", StageCode.MONTAGEM_PACOTE, input, Map.of()),
                input,
                new InMemoryArtifactStore()));

        assertThat(result.status()).isEqualTo(StageStatus.COMPLETED);
        assertThat(result.nextStageCode()).isNull();
        assertThat(result.output().pdf().contentType()).isEqualTo("application/pdf");
        assertThat(result.output().pdf().content()).startsWith("%PDF".getBytes());
        assertThat(new String(result.output().html().content(), java.nio.charset.StandardCharsets.UTF_8))
                .contains("Experiencia de entrega premium")
                .contains("Diagnostico inicial")
                .contains("Template preenchivel")
                .contains("Gate de qualidade comercial");
        assertThat(new String(result.output().spreadsheet().content(), java.nio.charset.StandardCharsets.UTF_8))
                .contains("acao_7_dias")
                .contains("criterio_conclusao");
        assertThat(result.output().spreadsheet().name()).endsWith(".csv");
        assertThat(result.output().zipPackage().contentType()).isEqualTo("application/zip");
        assertThat(result.artifacts()).extracting("type").contains("FINAL_HTML", "FINAL_PDF", "FINAL_SPREADSHEET", "FINAL_ZIP");
    }
}
