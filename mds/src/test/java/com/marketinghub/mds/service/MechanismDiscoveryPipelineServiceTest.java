package com.marketinghub.mds.service;

import com.marketinghub.mds.client.BackendMdsClient;
import com.marketinghub.mds.dto.BackendArtifactPublishBatchResponseDto;
import com.marketinghub.mds.dto.BackendMdsRequestDto;
import com.marketinghub.mds.search.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MechanismDiscoveryPipelineServiceTest {
    @Mock
    private BackendMdsClient backendMdsClient;
    @Mock
    private MechanismQuestionBuilder mechanismQuestionBuilder;
    @Mock
    private SearchQueryPlanBuilder searchQueryPlanBuilder;
    @Mock
    private SearchExecutionService searchExecutionService;
    @Mock
    private SourceDedupService sourceDedupService;
    @Mock
    private EvidenceScreeningService evidenceScreeningService;
    @Mock
    private EvidenceConfidenceService evidenceConfidenceService;
    @Mock
    private EvidenceItemBuilder evidenceItemBuilder;

    @InjectMocks
    private MechanismDiscoveryPipelineService service;

    @Test
    void shouldPublishSourceDocumentsAndEvidenceItemsWithLineage() {
        BackendMdsRequestDto request = new BackendMdsRequestDto(
                10L,
                "IN_PROGRESS",
                "weight-loss",
                "plateau",
                "consistent fat loss",
                "corr-123"
        );
        SearchQueryPlan plan = new SearchQueryPlan("pubmed", "q1", 10);
        SourceSearchHit hit = new SourceSearchHit(
                "pubmed",
                "pubmed:1",
                "10.1000/xyz",
                "title",
                "abstract",
                "https://pubmed.ncbi.nlm.nih.gov/1/",
                "2024",
                "CC-BY",
                true,
                true,
                false,
                List.of("A Author")
        );
        ScreenedEvidence screenedEvidence = new ScreenedEvidence(
                "pubmed",
                "pubmed:1",
                "title",
                "abstract",
                "10.1000/xyz",
                "https://pubmed.ncbi.nlm.nih.gov/1/",
                "2024",
                List.of("pilot"),
                "alta",
                "moderada",
                List.of("randomized"),
                0.7,
                0.3,
                0.62
        );

        when(mechanismQuestionBuilder.build(request)).thenReturn(new MechanismQuestion("q1"));
        when(searchQueryPlanBuilder.buildPlans(new MechanismQuestion("q1"))).thenReturn(List.of(plan));
        when(searchExecutionService.execute(List.of(plan))).thenReturn(List.of(hit));
        when(sourceDedupService.deduplicate(List.of(hit))).thenReturn(List.of(hit));
        when(evidenceScreeningService.screen(request, List.of(hit))).thenReturn(List.of(screenedEvidence));
        when(evidenceScreeningService.prioritize(List.of(screenedEvidence), 12)).thenReturn(List.of(screenedEvidence));
        when(evidenceConfidenceService.classify(screenedEvidence)).thenReturn("moderada");
        when(evidenceItemBuilder.build(eq(10L), eq(screenedEvidence), eq("moderada"), eq(List.of(101L))))
                .thenReturn(new com.marketinghub.mds.dto.BackendArtifactPublishBatchRequestDto.ArtifactPayloadDto(
                        "evidenceItem", "v1", "v1", "DRAFT", "mds", "mds", java.util.Map.of("id", "ev-1"), null, List.of(101L)
                ));

        when(backendMdsClient.publishBatch(any()))
                .thenReturn(new BackendArtifactPublishBatchResponseDto(10L, 2, List.of(100L, 101L)))
                .thenReturn(new BackendArtifactPublishBatchResponseDto(10L, 1, List.of(102L)));

        service.execute(request);

        ArgumentCaptor<com.marketinghub.mds.dto.BackendArtifactPublishBatchRequestDto> batchCaptor =
                ArgumentCaptor.forClass(com.marketinghub.mds.dto.BackendArtifactPublishBatchRequestDto.class);
        verify(backendMdsClient, times(2)).publishBatch(batchCaptor.capture());

        List<com.marketinghub.mds.dto.BackendArtifactPublishBatchRequestDto> batches = batchCaptor.getAllValues();
        assertThat(batches.get(0).artifacts()).extracting(a -> a.artifactType())
                .containsExactly("mechanismEvidenceSearch", "sourceDocument");
        assertThat(batches.get(1).artifacts()).extracting(a -> a.artifactType())
                .containsExactly("evidenceItem");

        ArgumentCaptor<com.marketinghub.mds.dto.BackendSourceAccessPublishBatchRequestDto> sourceAccessCaptor =
                ArgumentCaptor.forClass(com.marketinghub.mds.dto.BackendSourceAccessPublishBatchRequestDto.class);
        verify(backendMdsClient).publishSourceAccessBatch(sourceAccessCaptor.capture());
        assertThat(sourceAccessCaptor.getValue().records()).hasSize(1);

        verify(backendMdsClient, times(3)).heartbeat(eq(10L), any());
    }
}
