package com.marketinghub.mds.service;

import com.marketinghub.mds.client.BackendMdsClient;
import com.marketinghub.mds.dto.BackendArtifactPublishBatchRequestDto;
import com.marketinghub.mds.dto.BackendArtifactPublishBatchRequestDto.ArtifactPayloadDto;
import com.marketinghub.mds.dto.BackendMdsRequestDto;
import com.marketinghub.mds.dto.BackendSourceAccessPublishBatchRequestDto;
import com.marketinghub.mds.search.MechanismQuestion;
import com.marketinghub.mds.search.MechanismQuestionBuilder;
import com.marketinghub.mds.search.SearchExecutionService;
import com.marketinghub.mds.search.SearchQueryPlan;
import com.marketinghub.mds.search.SearchQueryPlanBuilder;
import com.marketinghub.mds.search.SourceSearchHit;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class MechanismDiscoveryPipelineService {
    private final BackendMdsClient backendMdsClient;
    private final MechanismQuestionBuilder mechanismQuestionBuilder;
    private final SearchQueryPlanBuilder searchQueryPlanBuilder;
    private final SearchExecutionService searchExecutionService;

    public MechanismDiscoveryPipelineService(BackendMdsClient backendMdsClient,
                                             MechanismQuestionBuilder mechanismQuestionBuilder,
                                             SearchQueryPlanBuilder searchQueryPlanBuilder,
                                             SearchExecutionService searchExecutionService) {
        this.backendMdsClient = backendMdsClient;
        this.mechanismQuestionBuilder = mechanismQuestionBuilder;
        this.searchQueryPlanBuilder = searchQueryPlanBuilder;
        this.searchExecutionService = searchExecutionService;
    }

    public void execute(BackendMdsRequestDto request) {
        MechanismQuestion question = mechanismQuestionBuilder.build(request);
        List<SearchQueryPlan> plans = searchQueryPlanBuilder.buildPlans(question);
        List<SourceSearchHit> hits = searchExecutionService.execute(plans);

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
                        "resultCount", hits.size()
                ),
                null,
                List.of()
        );

        List<ArtifactPayloadDto> sourceDocumentArtifacts = new ArrayList<>();
        List<BackendSourceAccessPublishBatchRequestDto.SourceAccessPayloadDto> sourceAccessRecords = new ArrayList<>();

        for (SourceSearchHit hit : hits) {
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

        List<ArtifactPayloadDto> allArtifacts = new ArrayList<>();
        allArtifacts.add(evidenceSearch);
        allArtifacts.addAll(sourceDocumentArtifacts);

        backendMdsClient.publishBatch(new BackendArtifactPublishBatchRequestDto(request.id(), allArtifacts));

        if (!sourceAccessRecords.isEmpty()) {
            backendMdsClient.publishSourceAccessBatch(new BackendSourceAccessPublishBatchRequestDto(sourceAccessRecords));
        }
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
