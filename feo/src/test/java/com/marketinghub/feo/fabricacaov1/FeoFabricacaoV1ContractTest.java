package com.marketinghub.feo.fabricacaov1;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.feo.fabricacaov1.contract.DeliverablePlan;
import com.marketinghub.feo.fabricacaov1.contract.DeliverableContentPackage;
import com.marketinghub.feo.fabricacaov1.contract.FabricationContext;
import com.marketinghub.feo.fabricacaov1.contract.PackageAssemblyInput;
import com.marketinghub.feo.fabricacaov1.contract.PackageAssemblyOutput;
import com.marketinghub.feo.fabricacaov1.contract.VisualAsset;
import com.marketinghub.feo.fabricacaov1.contract.VisualAssetSpec;
import com.marketinghub.feo.fabricacaov1.geracaoativosvisuais.GeracaoAtivosVisuaisProcessor;
import com.marketinghub.feo.fabricacaov1.geracaoativosvisuais.VisualAssetGenerator;
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
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipInputStream;
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
        assertThat(result.output().deliverables()).hasSizeGreaterThanOrEqualTo(9);
        assertThat(result.output().deliverables()).extracting("componentType")
                .contains(
                        "DIAGNOSTICO_GUIADO",
                        "MISSOES_7_DIAS",
                        "PAINEL_PROGRESSO",
                        "PLANO_EXECUCAO_RAPIDA",
                        "TEMPLATES_PRONTOS",
                        "PROVA_TANGIVEL",
                        "BIBLIOTECA_APOIO",
                        "RITUAL_ACOMPANHAMENTO",
                        "BONUS_ANTI_OBJECAO");
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
        PackageAssemblyInput input = new PackageAssemblyInput(context, plan, null, List.of());

        StageResult<PackageAssemblyInput> result = processor.process(new StageContext<>(
                new StageExecution<>("job-1", "exec-2", StageCode.REDACAO_ENTREGAVEIS, input, Map.of()),
                input,
                new InMemoryArtifactStore()));

        assertThat(result.status()).isEqualTo(StageStatus.COMPLETED);
        assertThat(result.nextStageCode()).isEqualTo(StageCode.GERACAO_ATIVOS_VISUAIS);
        DeliverableContentPackage contentPackage = result.output().contentPackage();
        assertThat(contentPackage.qualityScore()).isGreaterThanOrEqualTo(80);
        assertThat(contentPackage.visualAssets()).hasSizeGreaterThanOrEqualTo(4);
        assertThat(contentPackage.reviewerNotes().getFirst()).contains("método", "plano", "materiais prontos");
        assertThat(contentPackage.reviewerNotes()).anyMatch(note -> note.contains("MDS"));
        assertThat(contentPackage.deliverables().getFirst().sections()).hasSizeGreaterThanOrEqualTo(4);
        assertThat(contentPackage.deliverables().getFirst().appliedPrinciple()).isNotBlank();
        assertThat(contentPackage.deliverables().getFirst().readyToUseAsset()).isNotBlank();
        assertThat(contentPackage.deliverables().getFirst().tangibleProof()).isNotBlank();
        assertThat(contentPackage.deliverables().getFirst().ritualStep()).isNotBlank();
        assertThat(contentPackage.deliverables().getFirst().antiObjectionBonus()).isNotBlank();
        assertThat(contentPackage.deliverables().getFirst().templateFields()).contains("Situação atual");
        assertThat(contentPackage.deliverables().getFirst().templateFields()).contains("Princípio aplicado");
    }

    /**
     * Confirma que a etapa visual gera imagens obrigatórias antes da montagem.
     */
    @Test
    void geracaoVisualDeveGerarCapaInfograficosEFiguras() {
        PackageAssemblyInput input = redacaoCompleta();
        GeracaoAtivosVisuaisProcessor processor = new GeracaoAtivosVisuaisProcessor(new FakeVisualAssetGenerator(), new ObjectMapper());

        StageResult<PackageAssemblyInput> result = processor.process(new StageContext<>(
                new StageExecution<>("job-1", "exec-3", StageCode.GERACAO_ATIVOS_VISUAIS, input, Map.of()),
                input,
                new InMemoryArtifactStore()));

        assertThat(result.status()).isEqualTo(StageStatus.COMPLETED);
        assertThat(result.nextStageCode()).isEqualTo(StageCode.MONTAGEM_PACOTE);
        assertThat(result.output().visualAssets()).hasSize(input.contentPackage().visualAssets().size());
        assertThat(result.output().visualAssets()).extracting("assetType")
                .contains("EBOOK_COVER", "INFOGRAPHIC", "CONCEPT_MAP", "BEFORE_AFTER");
    }

    /**
     * Confirma que a montagem gera experiência guiada, PDF, planilha CSV e ZIP com conteúdo de produto final.
     */
    @Test
    void montagemDeveGerarPdfPlanilhaEZip() throws java.io.IOException {
        PackageAssemblyInput input = new GeracaoAtivosVisuaisProcessor(new FakeVisualAssetGenerator(), new ObjectMapper())
                .process(new StageContext<>(
                        new StageExecution<>("job-1", "exec-3", StageCode.GERACAO_ATIVOS_VISUAIS, redacaoCompleta(), Map.of()),
                        redacaoCompleta(),
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
        assertThat(new String(result.output().experienceSite().content(), java.nio.charset.StandardCharsets.UTF_8))
                .contains("Produto Digital Experiencial")
                .contains("Dia 0 - Diagnóstico MUSA")
                .contains("Mecanismo aplicado")
                .contains("Jornada guiada de 7 dias")
                .contains("Biblioteca de apoio")
                .contains("02-ebook-principal.pdf")
                .contains("03-plano-checklists-e-templates.csv")
                .doesNotContain("Score FEO")
                .doesNotContain("Fabricado pela FEO");
        assertThat(new String(result.output().spreadsheet().content(), java.nio.charset.StandardCharsets.UTF_8))
                .contains("primeira_vitoria")
                .contains("material_pronto")
                .contains("bonus_anti_objecao")
                .contains("criterio_conclusao");
        assertThat(result.output().spreadsheet().name()).endsWith(".csv");
        assertThat(result.output().zipPackage().contentType()).isEqualTo("application/zip");
        assertThat(zipEntries(result.output().zipPackage().content()))
                .anyMatch(name -> name.startsWith("imagens/vis-"))
                .contains("01-experiencia-guiada/index.html", "02-ebook-principal.pdf", "03-plano-checklists-e-templates.csv", "README.txt")
                .doesNotContain("00-fonte-editorial-interna.html", "manifesto.txt", "relatorio-fabricacao.txt");
        assertThat(result.artifacts()).extracting("type").contains("FINAL_EXPERIENCE_SITE", "FINAL_PDF", "FINAL_SPREADSHEET", "FINAL_ZIP");
    }

    /**
     * Executa planejamento e redação para reaproveitar entrada válida nos testes.
     */
    private PackageAssemblyInput redacaoCompleta() {
        PlanejamentoEntregaveisProcessor planejamento = new PlanejamentoEntregaveisProcessor(new ObjectMapper());
        FabricationContext context = FabricationContext.sample();
        DeliverablePlan plan = planejamento.process(new StageContext<>(
                        new StageExecution<>("job-1", "exec-1", StageCode.PLANEJAMENTO_ENTREGAVEIS, context, Map.of()),
                        context,
                        new InMemoryArtifactStore()))
                .output();
        RedacaoEntregaveisProcessor redacao = new RedacaoEntregaveisProcessor(new ObjectMapper());
        PackageAssemblyInput redacaoInput = new PackageAssemblyInput(context, plan, null, List.of());
        return redacao.process(new StageContext<>(
                        new StageExecution<>("job-1", "exec-2", StageCode.REDACAO_ENTREGAVEIS, redacaoInput, Map.of()),
                        redacaoInput,
                        new InMemoryArtifactStore()))
                .output();
    }

    /**
     * Lista entradas de um ZIP gerado em memória.
     */
    private java.util.List<String> zipEntries(byte[] content) throws java.io.IOException {
        java.util.List<String> names = new java.util.ArrayList<>();
        try (ZipInputStream zip = new ZipInputStream(new java.io.ByteArrayInputStream(content))) {
            java.util.zip.ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                names.add(entry.getName());
            }
        }
        return names;
    }

    /**
     * Gera uma imagem PNG mínima para validar montagem sem chamar OpenAI.
     */
    private static class FakeVisualAssetGenerator implements VisualAssetGenerator {

        private static final byte[] PNG = Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAgAAAAICAIAAABLbSncAAAAEUlEQVR4nGP48PohVsQwtCQAZISvAYTTpXsAAAAASUVORK5CYII=");

        /**
         * Retorna ativo visual determinístico para testes de contrato.
         */
        @Override
        public VisualAsset generate(VisualAssetSpec spec) {
            return new VisualAsset(
                    spec.code(),
                    spec.title(),
                    spec.assetType(),
                    "imagens/" + spec.code().toLowerCase() + ".png",
                    "image/png",
                    PNG,
                    spec.prompt(),
                    "fake-image-model",
                    "{}",
                    "{}",
                    List.of("Imagem fake de teste"));
        }
    }
}
