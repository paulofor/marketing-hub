package com.marketinghub.mds.service;

import com.marketinghub.mds.client.BackendMdsClient;
import com.marketinghub.mds.dto.BackendArtifactPublishBatchRequestDto;
import com.marketinghub.mds.dto.BackendArtifactPublishBatchRequestDto.ArtifactPayloadDto;
import com.marketinghub.mds.dto.BackendMdsRequestDto;
import com.marketinghub.mds.dto.BackendSourceAccessPublishBatchRequestDto;
import com.marketinghub.mds.search.EvidenceConfidenceService;
import com.marketinghub.mds.search.EvidenceItemBuilder;
import com.marketinghub.mds.search.EvidenceScreeningService;
import com.marketinghub.mds.search.MechanismQuestion;
import com.marketinghub.mds.search.MechanismQuestionBuilder;
import com.marketinghub.mds.search.MechanismCandidateBuilder;
import com.marketinghub.mds.search.ScreenedEvidence;
import com.marketinghub.mds.search.SearchExecutionService;
import com.marketinghub.mds.search.SearchQueryPlan;
import com.marketinghub.mds.search.SearchQueryPlanBuilder;
import com.marketinghub.mds.search.SourceDedupService;
import com.marketinghub.mds.search.SourceSearchHit;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class MechanismDiscoveryPipelineService {
    private final BackendMdsClient backendMdsClient;
    private final MechanismQuestionBuilder mechanismQuestionBuilder;
    private final SearchQueryPlanBuilder searchQueryPlanBuilder;
    private final SearchExecutionService searchExecutionService;
    private final SourceDedupService sourceDedupService;
    private final EvidenceScreeningService evidenceScreeningService;
    private final EvidenceConfidenceService evidenceConfidenceService;
    private final EvidenceItemBuilder evidenceItemBuilder;
    private final MechanismCandidateBuilder mechanismCandidateBuilder;

    public MechanismDiscoveryPipelineService(BackendMdsClient backendMdsClient,
                                             MechanismQuestionBuilder mechanismQuestionBuilder,
                                             SearchQueryPlanBuilder searchQueryPlanBuilder,
                                             SearchExecutionService searchExecutionService,
                                             SourceDedupService sourceDedupService,
                                             EvidenceScreeningService evidenceScreeningService,
                                             EvidenceConfidenceService evidenceConfidenceService,
                                             EvidenceItemBuilder evidenceItemBuilder,
                                             MechanismCandidateBuilder mechanismCandidateBuilder) {
        this.backendMdsClient = backendMdsClient;
        this.mechanismQuestionBuilder = mechanismQuestionBuilder;
        this.searchQueryPlanBuilder = searchQueryPlanBuilder;
        this.searchExecutionService = searchExecutionService;
        this.sourceDedupService = sourceDedupService;
        this.evidenceScreeningService = evidenceScreeningService;
        this.evidenceConfidenceService = evidenceConfidenceService;
        this.evidenceItemBuilder = evidenceItemBuilder;
        this.mechanismCandidateBuilder = mechanismCandidateBuilder;
    }

    public void execute(BackendMdsRequestDto request) {
        MechanismQuestion question = mechanismQuestionBuilder.build(request);
        List<SearchQueryPlan> plans = searchQueryPlanBuilder.buildPlans(question);
        List<SourceSearchHit> rawHits = searchExecutionService.execute(plans);

        List<SourceSearchHit> dedupedHits = sourceDedupService.deduplicate(rawHits);
        backendMdsClient.heartbeat(request.id(), new com.marketinghub.mds.dto.BackendHeartbeatRequestDto(
                "dedup-normalize",
                "deduplication finished",
                Map.of("rawCount", rawHits.size(), "dedupedCount", dedupedHits.size())
        ));

        List<ScreenedEvidence> screened = evidenceScreeningService.screen(request, dedupedHits);
        List<ScreenedEvidence> prioritized = evidenceScreeningService.prioritize(screened, 12);
        backendMdsClient.heartbeat(request.id(), new com.marketinghub.mds.dto.BackendHeartbeatRequestDto(
                "screening",
                "screening finished",
                Map.of("eligibleCount", screened.size(), "prioritizedCount", prioritized.size())
        ));

        ArtifactPayloadDto evidenceSearch = new ArtifactPayloadDto(
                "mechanismEvidenceSearch",
                "v1",
                "v1",
                "DRAFT",
                "mds",
                "mds",
                Map.of(
                        "requestId", request.id(),
                        "question", question.text(),
                        "queries", plans.stream().map(p -> Map.of("source", p.source(), "query", p.query(), "limit", p.limit())).toList(),
                        "rawResultCount", rawHits.size(),
                        "dedupedResultCount", dedupedHits.size(),
                        "screenedResultCount", prioritized.size()
                ),
                null,
                List.of()
        );

        List<ArtifactPayloadDto> sourceDocumentArtifacts = new ArrayList<>();
        List<BackendSourceAccessPublishBatchRequestDto.SourceAccessPayloadDto> sourceAccessRecords = new ArrayList<>();

        for (SourceSearchHit hit : dedupedHits) {
            String accessClass = resolveAccessClass(hit);
            String permissionState = resolvePermissionState(hit);

            java.util.Map<String, Object> sourceDocumentContent = new java.util.LinkedHashMap<>();
            sourceDocumentContent.put("requestId", request.id());
            sourceDocumentContent.put("source", hit.source());
            sourceDocumentContent.put("sourceDocumentId", hit.sourceDocumentId());
            sourceDocumentContent.put("title", nvl(hit.title()));
            sourceDocumentContent.put("doi", nvl(hit.doi()));
            sourceDocumentContent.put("url", nvl(hit.url()));
            sourceDocumentContent.put("publicationYear", nvl(hit.publicationYear()));
            sourceDocumentContent.put("authors", hit.authors() == null ? List.of() : hit.authors());
            sourceDocumentContent.put("abstract", nvl(hit.abstractText()));
            sourceDocumentContent.put("accessClass", accessClass);
            sourceDocumentContent.put("permissionState", permissionState);

            sourceDocumentArtifacts.add(new ArtifactPayloadDto(
                    "sourceDocument",
                    "v1",
                    "v1",
                    "DRAFT",
                    "mds",
                    "mds",
                    sourceDocumentContent,
                    null,
                    List.of()
            ));

            sourceAccessRecords.add(new BackendSourceAccessPublishBatchRequestDto.SourceAccessPayloadDto(
                    hit.sourceDocumentId(),
                    accessClass,
                    permissionState,
                    hit.licenseText(),
                    hit.url()
            ));
        }

        List<ArtifactPayloadDto> sourceBatch = new ArrayList<>();
        sourceBatch.add(evidenceSearch);
        sourceBatch.addAll(sourceDocumentArtifacts);
        var sourcePublishResponse = backendMdsClient.publishBatch(new BackendArtifactPublishBatchRequestDto(request.id(), sourceBatch));

        if (!sourceAccessRecords.isEmpty()) {
            backendMdsClient.publishSourceAccessBatch(new BackendSourceAccessPublishBatchRequestDto(sourceAccessRecords));
        }

        Map<String, Long> sourceArtifactIdsBySourceDocumentId = new LinkedHashMap<>();
        List<Long> sourceArtifactIds = sourcePublishResponse.artifactIds().stream().skip(1).toList();
        for (int i = 0; i < dedupedHits.size() && i < sourceArtifactIds.size(); i++) {
            sourceArtifactIdsBySourceDocumentId.put(dedupedHits.get(i).sourceDocumentId(), sourceArtifactIds.get(i));
        }

        List<ArtifactPayloadDto> evidenceItems = new ArrayList<>();
        Map<String, String> confidenceBySourceDocumentId = new LinkedHashMap<>();
        for (ScreenedEvidence screenedEvidence : prioritized) {
            String confidenceLevel = evidenceConfidenceService.classify(screenedEvidence);
            confidenceBySourceDocumentId.put(screenedEvidence.sourceDocumentId(), confidenceLevel);
            Long parentArtifactId = sourceArtifactIdsBySourceDocumentId.get(screenedEvidence.sourceDocumentId());
            evidenceItems.add(evidenceItemBuilder.build(request.id(), screenedEvidence, confidenceLevel,
                    parentArtifactId == null ? List.of() : List.of(parentArtifactId)));
        }

        Map<String, Long> evidenceArtifactIdsBySourceDocumentId = new LinkedHashMap<>();
        if (!evidenceItems.isEmpty()) {
            var evidencePublishResponse =
                    backendMdsClient.publishBatch(new BackendArtifactPublishBatchRequestDto(request.id(), evidenceItems));
            List<Long> evidenceArtifactIds = evidencePublishResponse.artifactIds();
            for (int i = 0; i < prioritized.size() && i < evidenceArtifactIds.size(); i++) {
                evidenceArtifactIdsBySourceDocumentId.put(prioritized.get(i).sourceDocumentId(), evidenceArtifactIds.get(i));
            }
        }

        backendMdsClient.heartbeat(request.id(), new com.marketinghub.mds.dto.BackendHeartbeatRequestDto(
                "evidence-analysis",
                "evidence items published",
                Map.of("evidenceItemCount", evidenceItems.size())
        ));

        var mechanismBuild = mechanismCandidateBuilder.build(
                request.id(),
                prioritized,
                confidenceBySourceDocumentId,
                evidenceArtifactIdsBySourceDocumentId
        );
        if (!mechanismBuild.candidateArtifacts().isEmpty()) {
            var candidatePublishResponse = backendMdsClient.publishBatch(
                    new BackendArtifactPublishBatchRequestDto(request.id(), mechanismBuild.candidateArtifacts())
            );
            Long selectedCandidateArtifactId = candidatePublishResponse.artifactIds().isEmpty()
                    ? null
                    : candidatePublishResponse.artifactIds().get(0);

            ArtifactPayloadDto mechanismSpec = mechanismCandidateBuilder.buildMechanismSpec(
                    mechanismBuild.mechanismSpecDraft(),
                    selectedCandidateArtifactId,
                    mechanismBuild.supportingEvidenceArtifactIds()
            );
            if (mechanismSpec != null) {
                backendMdsClient.publishBatch(new BackendArtifactPublishBatchRequestDto(request.id(), List.of(mechanismSpec)));
            }
        }

        backendMdsClient.heartbeat(request.id(), new com.marketinghub.mds.dto.BackendHeartbeatRequestDto(
                "mechanism-building",
                "mechanism candidates and mechanism spec published",
                Map.of("mechanismCandidateCount", mechanismBuild.candidateArtifacts().size())
        ));
    }

    private String resolveAccessClass(SourceSearchHit hit) {
        if (hit.openAccess()) {
            return "open_access";
        }
        if (hit.url() == null || hit.url().isBlank()) {
            return "metadata_only";
        }
        return "restricted";
    }

    private String resolvePermissionState(SourceSearchHit hit) {
        if (hit.canDownload() && hit.canTextMine()) {
            return "can_text_mine";
        }
        if (hit.canDownload()) {
            return "can_download";
        }
        return "link_only";
    }

    private String nvl(String value) {
        return value == null ? "" : value;
    }
}
