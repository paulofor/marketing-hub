package com.marketinghub.gerasalespage.v1.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.gerasalespage.v1.GeraSalesPagePublicationAudit;
import com.marketinghub.gerasalespage.v1.GeraSalesPagePublicationStageAudit;
import com.marketinghub.gerasalespage.v1.GeraSalesPageStageCode;
import com.marketinghub.gerasalespage.v1.GeraSalesPageStageExecution;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.gerasalespage.v1.GeraSalesPagePublicationAuditRepository;
import com.marketinghub.repository.jpa.gerasalespage.v1.GeraSalesPagePublicationStageAuditRepository;
import com.marketinghub.repository.jpa.gerasalespage.v1.GeraSalesPageStageExecutionRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

/** Valida o snapshot historico de publicacoes do GeraSalesPage v1. */
@ExtendWith(MockitoExtension.class)
class GeraSalesPagePublicationAuditServiceTest {
    @Mock
    private ExperimentRepository experimentRepository;
    @Mock
    private GeraSalesPageStageExecutionRepository executionRepository;
    @Mock
    private GeraSalesPagePublicationAuditRepository publicationRepository;
    @Mock
    private GeraSalesPagePublicationStageAuditRepository publicationStageRepository;
    @Spy
    private ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    @InjectMocks
    private GeraSalesPagePublicationAuditService service;

    /** Deve salvar a pagina final junto dos prompts e schemas usados pelas etapas. */
    @Test
    void shouldSnapshotPublicationWithStagePromptsAndSchemas() {
        Experiment experiment = new Experiment();
        experiment.setId(53L);
        experiment.setFollowUpActionUrl("https://pagamentopalf.site/sales-page-exp53.html");
        GeraSalesPageStageExecution offer = execution(
                "job-offer",
                GeraSalesPageStageCode.OFFER_BRIEF.code(),
                "{\"summary\":\"oferta\"}",
                Instant.parse("2026-07-01T10:00:00Z"));
        GeraSalesPageStageExecution publication = execution(
                "job-publication",
                GeraSalesPageStageCode.PUBLICATION_PACKAGE.code(),
                "{\"html\":\"<html>Venda</html>\",\"checkoutUrl\":\"https://mp.test/checkout\"}",
                Instant.parse("2026-07-01T10:10:00Z"));

        when(publicationRepository.existsByPublicationJobId("job-publication")).thenReturn(false);
        when(experimentRepository.findById(53L)).thenReturn(Optional.of(experiment));
        when(executionRepository.findByExperimentIdOrderByExecutionRequestedAtAsc(53L))
                .thenReturn(List.of(offer, publication));
        when(publicationRepository.save(any(GeraSalesPagePublicationAudit.class)))
                .thenAnswer(invocation -> {
                    GeraSalesPagePublicationAudit audit = invocation.getArgument(0);
                    audit.setId(10L);
                    return audit;
                });

        service.snapshotPublication(publication);

        ArgumentCaptor<GeraSalesPagePublicationAudit> audit =
                ArgumentCaptor.forClass(GeraSalesPagePublicationAudit.class);
        verify(publicationRepository).save(audit.capture());
        ArgumentCaptor<List<GeraSalesPagePublicationStageAudit>> stages = ArgumentCaptor.forClass(List.class);
        verify(publicationStageRepository).saveAll(stages.capture());
        assertThat(audit.getValue().getPublicationJobId()).isEqualTo("job-publication");
        assertThat(audit.getValue().getSalesPageUrl()).isEqualTo("https://pagamentopalf.site/sales-page-exp53.html");
        assertThat(audit.getValue().getCheckoutUrl()).isEqualTo("https://mp.test/checkout");
        assertThat(audit.getValue().getHtml()).contains("Venda");
        assertThat(stages.getValue()).hasSize(2);
        assertThat(stages.getValue().getFirst().getPublicationAuditId()).isEqualTo(10L);
        assertThat(stages.getValue().getFirst().getStageCode()).isEqualTo("sales-page-offer-brief");
        assertThat(stages.getValue().getFirst().getPrompt()).isEqualTo("prompt job-offer");
        assertThat(stages.getValue().getFirst().getSchemaJson()).isEqualTo("{\"type\":\"object\"}");
    }

    /** Cria execução concluída mínima para snapshot de teste. */
    private GeraSalesPageStageExecution execution(String idJob, String stageCode, String response, Instant requestedAt) {
        return GeraSalesPageStageExecution.builder()
                .idJob(idJob)
                .experimentId(53L)
                .stageCode(stageCode)
                .status("CONCLUIDO")
                .executionRequestedAt(requestedAt)
                .completedAt(requestedAt.plusSeconds(5))
                .promptTemplateKey("template-" + stageCode)
                .prompt("prompt " + idJob)
                .promptMarkdownContent("# Prompt " + idJob)
                .schemaJson("{\"type\":\"object\"}")
                .openAiModel("gpt-test")
                .openAiRequestBody("{\"model\":\"gpt-test\"}")
                .modelResponse(response)
                .rawResponse("{\"raw\":true}")
                .inputTokens(10)
                .outputTokens(5)
                .build();
    }
}
