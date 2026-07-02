package com.marketinghub.experiment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentAiPromptSchemaUsage;
import com.marketinghub.gerasalespage.v1.GeraSalesPagePromptSchemaTemplate;
import com.marketinghub.hypothesis.Hypothesis;
import com.marketinghub.hypothesis.pain.HypothesisPainStageExecution;
import com.marketinghub.repository.jpa.experiment.ExperimentAiPromptSchemaUsageRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.gerasalespage.v1.GeraSalesPagePromptSchemaTemplateRepository;
import com.marketinghub.repository.jpa.hypothesis.HypothesisPainStageExecutionRepository;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Responsabilidade: validar a associação de prompts/schemas de IA ao experimento. */
@ExtendWith(MockitoExtension.class)
class ExperimentAiPromptSchemaUsageServiceTest {
    @Mock
    private ExperimentRepository experimentRepository;
    @Mock
    private HypothesisPainStageExecutionRepository hypothesisExecutionRepository;
    @Mock
    private GeraSalesPagePromptSchemaTemplateRepository templateRepository;
    @Mock
    private ExperimentAiPromptSchemaUsageRepository usageRepository;
    @InjectMocks
    private ExperimentAiPromptSchemaUsageService service;

    /** Deve vincular ao experimento os templates usados pelas etapas da hipótese origem. */
    @Test
    void linkHypothesisTemplatesCreatesUsageFromCompletedExecutions() {
        UUID hypothesisId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID jobId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        Hypothesis hypothesis = new Hypothesis();
        hypothesis.setId(hypothesisId);
        Experiment experiment = Experiment.builder()
                .id(52L)
                .hypothesisRef(hypothesis)
                .build();
        HypothesisPainStageExecution execution = HypothesisPainStageExecution.builder()
                .stageCode("hypothesis-pain")
                .promptTemplateId("hypothesis-pipeline:hypothesis-pain:v1")
                .idJob(toBytes(jobId))
                .completedAt(Instant.parse("2026-07-02T12:00:00Z"))
                .build();
        GeraSalesPagePromptSchemaTemplate template = GeraSalesPagePromptSchemaTemplate.builder()
                .templateKey("hypothesis-pipeline:hypothesis-pain:v1")
                .pipelineCode("hypothesis-pipeline")
                .stageCode("hypothesis-pain")
                .version("v1")
                .openAiModel("gpt-5.5")
                .schemaName("hypothesis_pain")
                .promptMarkdownContent("Prompt")
                .schemaJson("{\"type\":\"object\"}")
                .active(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(experimentRepository.findById(52L)).thenReturn(Optional.of(experiment));
        when(hypothesisExecutionRepository.findByHypothesisIdOrderByExecutionRequestedAtAsc(hypothesisId))
                .thenReturn(List.of(execution));
        when(templateRepository.findById("hypothesis-pipeline:hypothesis-pain:v1"))
                .thenReturn(Optional.of(template));
        when(usageRepository.findByExperimentIdAndTemplateKeyAndUsageContextAndStageCode(
                52L,
                "hypothesis-pipeline:hypothesis-pain:v1",
                "HYPOTHESIS_PIPELINE",
                "hypothesis-pain"))
                .thenReturn(Optional.empty());

        service.linkHypothesisTemplates(52L);

        ArgumentCaptor<ExperimentAiPromptSchemaUsage> captor =
                ArgumentCaptor.forClass(ExperimentAiPromptSchemaUsage.class);
        verify(usageRepository).save(captor.capture());
        assertThat(captor.getValue().getExperiment()).isEqualTo(experiment);
        assertThat(captor.getValue().getTemplateKey()).isEqualTo("hypothesis-pipeline:hypothesis-pain:v1");
        assertThat(captor.getValue().getPipelineCode()).isEqualTo("hypothesis-pipeline");
        assertThat(captor.getValue().getUsageContext()).isEqualTo("HYPOTHESIS_PIPELINE");
        assertThat(captor.getValue().getSourceJobId()).isEqualTo(jobId.toString());
    }

    /** Converte UUID textual para o formato binário usado pelo pipeline de hipótese. */
    private byte[] toBytes(UUID uuid) {
        ByteBuffer buffer = ByteBuffer.allocate(16);
        buffer.putLong(uuid.getMostSignificantBits());
        buffer.putLong(uuid.getLeastSignificantBits());
        return buffer.array();
    }
}
