package com.marketinghub.experiment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.marketinghub.deliverable.Deliverable;
import com.marketinghub.deliverable.DeliverablePackage;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.feo.fabricacao.v1.FeoFabricacaoV1StageExecution;
import com.marketinghub.feo.fabricacao.v1.FeoFabricacaoV1StageStatus;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.repository.jpa.deliverable.DeliverablePackageRepository;
import com.marketinghub.repository.jpa.deliverable.DeliverableRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.feo.fabricacao.v1.FeoFabricacaoV1StageExecutionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.zip.ZipInputStream;
import org.junit.jupiter.api.Test;

/** Responsabilidade: validar a montagem do ZIP de entregáveis do experimento. */
class ExperimentDeliverablesZipServiceTest {

    /** Deve incluir artefato final, entregáveis do nicho e pacotes do experimento no ZIP. */
    @Test
    void generateShouldIncludeExperimentDeliverablesFiles() throws Exception {
        ExperimentRepository experimentRepository = org.mockito.Mockito.mock(ExperimentRepository.class);
        DeliverableRepository deliverableRepository = org.mockito.Mockito.mock(DeliverableRepository.class);
        DeliverablePackageRepository packageRepository = org.mockito.Mockito.mock(DeliverablePackageRepository.class);
        FeoFabricacaoV1StageExecutionRepository feoRepository =
                org.mockito.Mockito.mock(FeoFabricacaoV1StageExecutionRepository.class);
        ObjectMapper objectMapper = new ObjectMapper();
        ExperimentDeliverablesZipService service = new ExperimentDeliverablesZipService(
                experimentRepository,
                deliverableRepository,
                packageRepository,
                feoRepository,
                objectMapper);

        MarketNiche niche = MarketNiche.builder().id(10L).name("Mulheres urbanas").build();
        Experiment experiment = Experiment.builder()
                .id(65L)
                .name("Experimento 65")
                .niche(niche)
                .hypothesis("Sofisticacao acessivel")
                .landingPageDeliverables("{\"sampleDeliverables\":[\"Checklist\"]}")
                .build();
        Deliverable deliverable = Deliverable.builder()
                .id(7L)
                .niche(niche)
                .title("Checklist de elegancia")
                .description("Entrega principal")
                .content("Conteudo aprovado")
                .prompt("Prompt usado")
                .build();
        DeliverablePackage pack = DeliverablePackage.builder()
                .id(3L)
                .experiment(experiment)
                .name("Pacote inicial")
                .description("Curadoria")
                .prompt("Prompt do pacote")
                .deliverables(new LinkedHashSet<>(List.of(deliverable)))
                .build();
        FeoFabricacaoV1StageExecution feoExecution = FeoFabricacaoV1StageExecution.builder()
                .id(22L)
                .experiment(experiment)
                .stageCode("montagem-pacote")
                .status(FeoFabricacaoV1StageStatus.COMPLETED)
                .artifactsPayload(objectMapper.writeValueAsString(List.of(Map.of(
                        "type",
                        "FINAL_HTML",
                        "name",
                        "01-pacote-final.html",
                        "content",
                        Base64.getEncoder().encodeToString("<html>Premium FEO</html>".getBytes(StandardCharsets.UTF_8))))))
                .build();

        when(experimentRepository.findById(65L)).thenReturn(Optional.of(experiment));
        when(deliverableRepository.findByNicheIdOrderByCreatedAtDesc(10L)).thenReturn(List.of(deliverable));
        when(packageRepository.findByExperimentIdOrderByCreatedAtDesc(65L)).thenReturn(List.of(pack));
        when(feoRepository.findFirstByExperimentIdAndStageCodeAndStatusOrderByFinishedAtDesc(
                        65L,
                        "montagem-pacote",
                        FeoFabricacaoV1StageStatus.COMPLETED))
                .thenReturn(Optional.of(feoExecution));

        byte[] zip = service.generate(65L);

        java.util.Map<String, String> entries = readZip(zip);
        assertThat(entries)
                .containsKey("README.txt")
                .containsKey("landing-page-deliverables.json")
                .containsKey("entregaveis/007-checklist-de-elegancia.html")
                .containsKey("pacotes/003-pacote-inicial.html")
                .containsKey("feo/01-pacote-final-html.html");
        assertThat(entries.get("README.txt")).contains("Experimento 65");
        assertThat(entries.get("entregaveis/007-checklist-de-elegancia.html"))
                .contains("<html")
                .contains("Conteudo aprovado")
                .contains("Prompt usado");
        assertThat(entries.keySet()).noneMatch(name -> name.endsWith(".md"));
        assertThat(entries.get("feo/01-pacote-final-html.html")).contains("Premium FEO");
    }

    /** Lê o ZIP gerado em memória para facilitar asserções de conteúdo. */
    private java.util.Map<String, String> readZip(byte[] zip) throws Exception {
        java.util.Map<String, String> entries = new java.util.LinkedHashMap<>();
        try (ZipInputStream input = new ZipInputStream(new ByteArrayInputStream(zip), StandardCharsets.UTF_8)) {
            java.util.zip.ZipEntry entry;
            while ((entry = input.getNextEntry()) != null) {
                entries.put(entry.getName(), new String(input.readAllBytes(), StandardCharsets.UTF_8));
            }
        }
        return entries;
    }
}
