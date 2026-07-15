package com.marketinghub.feo.fabricacaov1;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.feo.fabricacaov1.contract.DeliverablePlan;
import com.marketinghub.feo.fabricacaov1.contract.DeliverableContentPackage;
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
import com.marketinghub.feo.fabricacaov1.redacaoentregaveis.RedacaoEntregaveisProcessor;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Valida contratos funcionais minimos da FEO v1.
 */
class FeoFabricacaoV1ContractTest {

    /**
     * Confirma que o planejamento cria etapa de redação e artefato auditável.
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
        assertThat(result.nextStageCode()).isEqualTo(StageCode.REDACAO_ENTREGAVEIS);
        assertThat(result.output().deliverables()).hasSizeGreaterThanOrEqualTo(3);
        assertThat(result.artifacts()).extracting("name").contains("feo-offer-deliverable-plan.json");
    }

    /**
     * Confirma que a redação gera conteúdos finais antes da montagem.
     */
    @Test
    void redacaoDeveGerarConteudoAplicavelComGateDeQualidade() {
        PlanejamentoEntregaveisProcessor planejamento = new PlanejamentoEntregaveisProcessor(new ObjectMapper());
        FabricationContext context = FabricationContext.sample();
        DeliverablePlan plan = planejamento.process(new StageContext<>(
                        new StageExecution<>("job-1", "exec-1", StageCode.PLANEJAMENTO_ENTREGAVEIS, context, Map.of()),
                        context,
                        new InMemoryArtifactStore()))
                .output();
        RedacaoEntregaveisProcessor processor = new RedacaoEntregaveisProcessor(new ObjectMapper());
        PackageAssemblyInput input = new PackageAssemblyInput(context, plan, null);

        StageResult<PackageAssemblyInput> result = processor.process(new StageContext<>(
                new StageExecution<>("job-1", "exec-2", StageCode.REDACAO_ENTREGAVEIS, input, Map.of()),
                input,
                new InMemoryArtifactStore()));

        assertThat(result.status()).isEqualTo(StageStatus.COMPLETED);
        assertThat(result.nextStageCode()).isEqualTo(StageCode.MONTAGEM_PACOTE);
        DeliverableContentPackage contentPackage = result.output().contentPackage();
        assertThat(contentPackage.qualityScore()).isGreaterThanOrEqualTo(80);
        assertThat(contentPackage.deliverables().getFirst().sections()).hasSizeGreaterThanOrEqualTo(4);
        assertThat(contentPackage.deliverables().getFirst().templateFields()).contains("Situação atual");
    }

    /**
     * Confirma que a montagem gera PDF, planilha CSV e ZIP com conteúdo de produto final.
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
        RedacaoEntregaveisProcessor redacao = new RedacaoEntregaveisProcessor(new ObjectMapper());
        PackageAssemblyInput redacaoInput = new PackageAssemblyInput(context, plan, null);
        PackageAssemblyInput input = redacao.process(new StageContext<>(
                        new StageExecution<>("job-1", "exec-2", StageCode.REDACAO_ENTREGAVEIS, redacaoInput, Map.of()),
                        redacaoInput,
                        new InMemoryArtifactStore()))
                .output();
        MontagemPacoteProcessor processor = new MontagemPacoteProcessor(new PackageAssetAssembler());

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
                .contains("Workbooks de aplicacao por entregavel")
                .contains("Erros a evitar")
                .contains("Template preenchivel")
                .contains("Gate de qualidade comercial");
        assertThat(new String(result.output().spreadsheet().content(), java.nio.charset.StandardCharsets.UTF_8))
                .contains("primeira_vitoria")
                .contains("criterio_conclusao");
        assertThat(result.output().spreadsheet().name()).endsWith(".csv");
        assertThat(result.output().zipPackage().contentType()).isEqualTo("application/zip");
        assertThat(result.artifacts()).extracting("type").contains("FINAL_HTML", "FINAL_PDF", "FINAL_SPREADSHEET", "FINAL_ZIP");
    }
}
