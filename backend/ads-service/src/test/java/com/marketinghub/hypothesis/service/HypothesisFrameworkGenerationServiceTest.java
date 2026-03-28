package com.marketinghub.hypothesis.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.ai.generation.service.AiWorkerGenerationService;
import com.marketinghub.hypothesis.Hypothesis;
import com.marketinghub.hypothesis.HypothesisFrameworkGenerationJob;
import com.marketinghub.hypothesis.HypothesisFrameworkGenerationJobStatus;
import com.marketinghub.hypothesis.dto.HypothesisDto;
import com.marketinghub.hypothesis.dto.HypothesisFrameworkDto;
import com.marketinghub.hypothesis.dto.HypothesisFrameworkGenerationRequest;
import com.marketinghub.hypothesis.dto.internal.HypothesisFrameworkGenerationJobCompletionRequest;
import com.marketinghub.hypothesis.framework.HypothesisFrameworkMapperSupport;
import com.marketinghub.hypothesis.framework.HypothesisFrameworkSection;
import com.marketinghub.hypothesis.mapper.HypothesisMapper;
import com.marketinghub.hypothesis.repository.HypothesisFrameworkGenerationJobRepository;
import com.marketinghub.hypothesis.repository.HypothesisRepository;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HypothesisFrameworkGenerationServiceTest {

    @Mock
    private HypothesisRepository hypothesisRepository;

    @Mock
    private HypothesisFrameworkGenerationJobRepository jobRepository;

    @Mock
    private HypothesisMapper mapper;

    @Mock
    private HypothesisFrameworkMapperSupport frameworkSupport;

    @Mock
    private AiWorkerGenerationService generationService;

    @Captor
    private ArgumentCaptor<HypothesisFrameworkGenerationJob> jobCaptor;

    private HypothesisFrameworkGenerationService service;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        service = new HypothesisFrameworkGenerationService(
                hypothesisRepository,
                jobRepository,
                mapper,
                frameworkSupport,
                generationService,
                objectMapper
        );
    }

    @Test
    @SuppressWarnings("unchecked")
    void generateEnqueuesJobWithTextJsonSchemaFormat() throws Exception {
        UUID hypothesisId = UUID.randomUUID();
        Hypothesis hypothesis = new Hypothesis();
        hypothesis.setId(hypothesisId);
        hypothesis.setTitle("Test");

        when(hypothesisRepository.findById(hypothesisId)).thenReturn(Optional.of(hypothesis));
        when(jobRepository.findByHypothesisIdAndSectionAndStatusInOrderByCreatedAtDesc(
                eq(hypothesisId), eq(HypothesisFrameworkSection.PAIN), any()))
                .thenReturn(List.of());
        when(frameworkSupport.resolve(hypothesis)).thenReturn(new HypothesisFrameworkDto());
        when(mapper.toDto(hypothesis)).thenReturn(new HypothesisDto());

        HypothesisFrameworkGenerationRequest request = new HypothesisFrameworkGenerationRequest();
        request.setModel("gpt-test");

        service.generate(hypothesisId, HypothesisFrameworkSection.PAIN, request);

        verify(jobRepository).save(jobCaptor.capture());
        Map<String, Object> body = objectMapper.readValue(jobCaptor.getValue().getRequestBodyJson(), new TypeReference<>() {
        });
        assertThat(body).containsKey("text");
        assertThat(body).doesNotContainKey("response_format");

        Map<String, Object> text = (Map<String, Object>) body.get("text");
        Map<String, Object> format = (Map<String, Object>) text.get("format");
        assertThat(format.get("type")).isEqualTo("json_schema");
        assertThat(format.get("name")).isEqualTo("hypothesis_framework_pain");
        assertThat(format.get("schema")).isInstanceOf(Map.class);

        Map<String, Object> jsonSchema = (Map<String, Object>) format.get("json_schema");
        assertThat(jsonSchema.get("name")).isEqualTo("hypothesis_framework_pain");
    }

    @Test
    void completeJobParsesWrappedMechanismPayload() {
        UUID jobId = UUID.randomUUID();
        Hypothesis hypothesis = new Hypothesis();
        hypothesis.setId(UUID.randomUUID());
        HypothesisFrameworkGenerationJob job = HypothesisFrameworkGenerationJob.builder()
                .id(jobId)
                .hypothesis(hypothesis)
                .section(HypothesisFrameworkSection.MECHANISM)
                .status(HypothesisFrameworkGenerationJobStatus.PROCESSING)
                .model("gpt-test")
                .prompt("prompt")
                .build();

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(frameworkSupport.resolve(hypothesis)).thenReturn(new HypothesisFrameworkDto());

        service.completeJob(jobId, new HypothesisFrameworkGenerationJobCompletionRequest(
                "{\"mechanism\":{\"core\":\"Core\",\"differential\":\"Diff\",\"believable\":\"Proof\"}}",
                "{}",
                "{\"model\":\"gpt-test\"}",
                10,
                20,
                null
        ));

        ArgumentCaptor<HypothesisFrameworkDto> snapshotCaptor = ArgumentCaptor.forClass(HypothesisFrameworkDto.class);
        ArgumentCaptor<HypothesisFrameworkDto> partialCaptor = ArgumentCaptor.forClass(HypothesisFrameworkDto.class);
        verify(frameworkSupport).storeSnapshot(eq(hypothesis), snapshotCaptor.capture(), partialCaptor.capture());

        assertThat(snapshotCaptor.getValue().getMechanism().getCore()).isEqualTo("Core");
        assertThat(snapshotCaptor.getValue().getMechanism().getUnique()).isEqualTo("Diff");
        assertThat(snapshotCaptor.getValue().getMechanism().getBelievability()).isEqualTo("Proof");
        assertThat(partialCaptor.getValue().getMechanism().getCore()).isEqualTo("Core");
        assertThat(job.getRequestBodyJson()).isEqualTo("{\"model\":\"gpt-test\"}");
    }
}
