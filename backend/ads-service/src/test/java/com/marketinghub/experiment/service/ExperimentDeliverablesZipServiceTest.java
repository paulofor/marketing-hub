package com.marketinghub.experiment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.marketinghub.deliverable.Deliverable;
import com.marketinghub.deliverable.DeliverablePackage;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.repository.jpa.deliverable.DeliverablePackageRepository;
import com.marketinghub.repository.jpa.deliverable.DeliverableRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.List;
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
        ExperimentDeliverablesZipService service = new ExperimentDeliverablesZipService(
                experimentRepository,
                deliverableRepository,
                packageRepository);

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

        when(experimentRepository.findById(65L)).thenReturn(Optional.of(experiment));
        when(deliverableRepository.findByNicheIdOrderByCreatedAtDesc(10L)).thenReturn(List.of(deliverable));
        when(packageRepository.findByExperimentIdOrderByCreatedAtDesc(65L)).thenReturn(List.of(pack));

        byte[] zip = service.generate(65L);

        java.util.Map<String, String> entries = readZip(zip);
        assertThat(entries)
                .containsKey("README.txt")
                .containsKey("landing-page-deliverables.json")
                .containsKey("entregaveis/007-checklist-de-elegancia.md")
                .containsKey("pacotes/003-pacote-inicial.md");
        assertThat(entries.get("README.txt")).contains("Experimento 65");
        assertThat(entries.get("entregaveis/007-checklist-de-elegancia.md"))
                .contains("Conteudo aprovado")
                .contains("Prompt usado");
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
