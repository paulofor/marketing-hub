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
import com.marketinghub.feo.infrastructure.config.FeoProperties;
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
        assertThat(contentPackage.reviewerNotes()).anyMatch(note -> note.contains("aprendizados"));
        assertThat(contentPackage.deliverables().getFirst().sections()).hasSizeGreaterThanOrEqualTo(4);
        assertThat(contentPackage.deliverables().getFirst().appliedPrinciple()).isNotBlank();
        assertThat(contentPackage.deliverables().getFirst().readyToUseAsset()).isNotBlank();
        assertThat(contentPackage.deliverables().getFirst().tangibleProof()).isNotBlank();
        assertThat(contentPackage.deliverables().getFirst().ritualStep()).isNotBlank();
        assertThat(contentPackage.deliverables().getFirst().antiObjectionBonus()).isNotBlank();
        assertThat(contentPackage.deliverables().getFirst().templateFields()).contains("Situação atual");
        assertThat(contentPackage.deliverables().getFirst().templateFields()).contains("Regra simples");
    }

    /**
     * Confirma que a etapa visual gera imagens obrigatórias antes da montagem.
     */
    @Test
    void geracaoVisualDeveGerarCapaInfograficosEFiguras() {
        PackageAssemblyInput input = redacaoCompleta();
        GeracaoAtivosVisuaisProcessor processor = new GeracaoAtivosVisuaisProcessor(
                new FakeVisualAssetGenerator(),
                new ObjectMapper(),
                feoProperties(true));

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
     * Confirma que imagens externas desabilitadas nao bloqueiam a montagem do pacote final.
     */
    @Test
    void geracaoVisualDesabilitadaDevePermitirMontagemSemImagens() {
        PackageAssemblyInput input = redacaoCompleta();
        GeracaoAtivosVisuaisProcessor processor = new GeracaoAtivosVisuaisProcessor(
                new FakeVisualAssetGenerator(),
                new ObjectMapper(),
                feoProperties(false));

        StageResult<PackageAssemblyInput> result = processor.process(new StageContext<>(
                new StageExecution<>("job-1", "exec-3", StageCode.GERACAO_ATIVOS_VISUAIS, input, Map.of()),
                input,
                new InMemoryArtifactStore()));

        assertThat(result.status()).isEqualTo(StageStatus.COMPLETED);
        assertThat(result.nextStageCode()).isEqualTo(StageCode.MONTAGEM_PACOTE);
        assertThat(result.output().visualAssets()).isEmpty();
        assertThat(result.metrics()).containsEntry("qualityGate", "VISUAL_ASSETS_OPTIONAL_SKIPPED");
    }

    /**
     * Confirma que a montagem gera experiência guiada, PDF, planilha CSV e ZIP com conteúdo de produto final.
     */
    @Test
    void montagemDeveGerarPdfPlanilhaEZip() throws java.io.IOException {
        PackageAssemblyInput input = new GeracaoAtivosVisuaisProcessor(
                        new FakeVisualAssetGenerator(),
                        new ObjectMapper(),
                        feoProperties(true))
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
                .contains("Método MUSA")
                .contains("Dia 0 - Diagnóstico MUSA")
                .contains("Seu mapa de aplicação")
                .contains("Jornada guiada de 7 dias")
                .contains("Biblioteca de apoio")
                .contains("02-ebook-principal.pdf")
                .contains("03-plano-checklists-e-templates.csv")
                .doesNotContain("Mecanismo aplicado")
                .doesNotContain("pesquisa")
                .doesNotContain("cliente")
                .doesNotContain("comprador")
                .doesNotContain("Score FEO")
                .doesNotContain("Fabricado pela FEO")
                .doesNotContain("MDS");
        assertThat(new String(result.output().spreadsheet().content(), java.nio.charset.StandardCharsets.UTF_8))
                .contains("Primeira vitória")
                .contains("Material pronto")
                .contains("Quando bater dúvida")
                .contains("Quando considerar feito")
                .doesNotContain("primeira_vitoria")
                .doesNotContain("prova_tangivel")
                .doesNotContain("bonus_anti_objecao")
                .doesNotContain("criterio_conclusao")
                .doesNotContain("criterios_qualidade");
        assertThat(result.output().spreadsheet().name()).endsWith(".csv");
        assertThat(result.output().zipPackage().contentType()).isEqualTo("application/zip");
        assertThat(zipEntries(result.output().zipPackage().content()))
                .anyMatch(name -> name.startsWith("imagens/vis-"))
                .contains("01-experiencia-guiada/index.html", "02-ebook-principal.pdf", "03-plano-checklists-e-templates.csv", "README.txt")
                .doesNotContain("00-fonte-editorial-interna.html", "manifesto.txt", "relatorio-fabricacao.txt")
                .noneMatch(name -> name.startsWith("entregaveis/"));
        assertThat(zipText(result.output().zipPackage().content()).toLowerCase())
                .doesNotContain("mecanismo")
                .doesNotContain("pesquisa")
                .doesNotContain("princípio científico")
                .doesNotContain("entregável")
                .doesNotContain("entregaveis")
                .doesNotContain("ativo")
                .doesNotContain("cliente")
                .doesNotContain("comprador")
                .doesNotContain("criterios_qualidade")
                .doesNotContain("criterio_conclusao")
                .doesNotContain("bonus_anti_objecao");
        assertThat(result.artifacts()).extracting("type").contains("FINAL_EXPERIENCE_SITE", "FINAL_PDF", "FINAL_SPREADSHEET", "FINAL_ZIP");
    }

    /**
     * Gera o pacote local do experimento 66 para leitura manual do PDF e da experiência.
     */
    @Test
    void deveGerarMaterialLocalDoExperimento66ParaRevisao() throws java.io.IOException {
        PackageAssemblyInput input = new GeracaoAtivosVisuaisProcessor(
                        new FakeVisualAssetGenerator(),
                        new ObjectMapper(),
                        feoProperties(true))
                .process(new StageContext<>(
                        new StageExecution<>(
                                "job-exp-66",
                                "exec-exp-66-visual",
                                StageCode.GERACAO_ATIVOS_VISUAIS,
                                redacaoCompleta(experimento66Context()),
                                Map.of()),
                        redacaoCompleta(experimento66Context()),
                        new InMemoryArtifactStore()))
                .output();
        PackageAssemblyOutput output = new PackageAssetAssembler().assemble(input);
        java.nio.file.Path dir = java.nio.file.Path.of("target", "experimento-66-musa");
        java.nio.file.Files.createDirectories(dir);
        java.nio.file.Files.write(dir.resolve("02-ebook-principal.pdf"), output.pdf().content());
        java.nio.file.Files.write(dir.resolve("01-experiencia-guiada.html"), output.experienceSite().content());
        java.nio.file.Files.write(dir.resolve("03-plano-checklists-e-templates.csv"), output.spreadsheet().content());
        java.nio.file.Files.write(dir.resolve("00-metodo-musa-produto-digital-experiencial.zip"), output.zipPackage().content());

        String publicZipText = zipText(output.zipPackage().content()).toLowerCase();
        assertThat(publicZipText)
                .contains("para quando você se olha pronta")
                .contains("está ok, mas ainda não está marcante")
                .contains("luxo caro")
                .contains("menos dúvida e mais presença")
                .doesNotContain("experimento")
                .doesNotContain("feo")
                .doesNotContain("json")
                .doesNotContain("ctr")
                .doesNotContain("cpl")
                .doesNotContain("lead")
                .doesNotContain("mecanismo")
                .doesNotContain("comprador")
                .doesNotContain("cliente");
    }

    /**
     * Monta configuracao de teste com geracao visual controlada.
     */
    private FeoProperties feoProperties(boolean visualAssetsEnabled) {
        return new FeoProperties(
                "feo-test",
                "http://backend",
                1,
                "/tmp/feo",
                "https://api.openai.com",
                "test-key",
                "",
                "fake-image-model",
                "low",
                visualAssetsEnabled);
    }

    /**
     * Executa planejamento e redação para reaproveitar entrada válida nos testes.
     */
    private PackageAssemblyInput redacaoCompleta() {
        return redacaoCompleta(FabricationContext.sample());
    }

    /**
     * Executa planejamento e redação para um contexto específico de revisão local.
     */
    private PackageAssemblyInput redacaoCompleta(FabricationContext context) {
        PlanejamentoEntregaveisProcessor planejamento = new PlanejamentoEntregaveisProcessor(new ObjectMapper());
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
     * Monta o contexto comercial validado do experimento 66.
     */
    private FabricationContext experimento66Context() {
        return new FabricationContext(
                "fabricacao-experimento-66",
                "66",
                "Método MUSA - Presença Elegante em 7 Dias",
                "Mulheres urbanas que querem uma presença mais marcante sem gastar muito",
                "Monte em 7 dias uma presença mais elegante, marcante e coerente sem depender de luxo caro, compras impulsivas ou transformação radical.",
                "Sentir-se mais segura, alinhada e memorável ao entrar em uma situação real usando escolhas acessíveis.",
                "Mapa de Presença Elegante: reduzir ruído visual, coordenar sinais pessoais e criar uma assinatura repetível com cabelo, pele, roupa, perfume, acessórios e ocasião.",
                "A jornada usa microajustes, comparação antes/depois e decisão anti-impulso para reduzir esforço e aumentar percepção de presença.",
                List.of(
                        "Experiência guiada de 7 dias",
                        "E-book principal",
                        "Checklist de presença em 12 minutos",
                        "Cartões de decisão",
                        "Lista anti-impulso",
                        "Exemplo preenchido"),
                List.of(
                        "Dor validada: sentir que está arrumada, mas sem presença marcante",
                        "Oferta definida: Método MUSA por R$47",
                        "Direção de produto: experiência guiada com e-book, checklists e templates"));
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
     * Extrai texto dos arquivos publicos do ZIP para validar linguagem final.
     */
    private String zipText(byte[] content) throws java.io.IOException {
        StringBuilder text = new StringBuilder();
        try (ZipInputStream zip = new ZipInputStream(new java.io.ByteArrayInputStream(content))) {
            java.util.zip.ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.getName().endsWith(".html")
                        || entry.getName().endsWith(".txt")
                        || entry.getName().endsWith(".csv")) {
                    text.append(new String(zip.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8)).append('\n');
                }
            }
        }
        return text.toString();
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
