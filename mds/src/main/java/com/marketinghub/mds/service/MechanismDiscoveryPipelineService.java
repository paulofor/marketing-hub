package com.marketinghub.mds.service;

import com.marketinghub.mds.client.BackendMdsClient;
import com.marketinghub.mds.dto.BackendArtifactPublishBatchRequestDto;
import com.marketinghub.mds.dto.BackendArtifactPublishBatchRequestDto.ArtifactPayloadDto;
import com.marketinghub.mds.dto.BackendMdsRequestDto;
import com.marketinghub.mds.dto.BackendSourceAccessPublishBatchRequestDto;
import com.marketinghub.mds.search.EvidenceConfidenceService;
import com.marketinghub.mds.search.EvidenceItemBuilder;
import com.marketinghub.mds.search.EvidenceScreeningService;
import com.marketinghub.mds.search.DiscoveryReportBuilder;
import com.marketinghub.mds.search.MechanismQuestion;
import com.marketinghub.mds.search.MechanismQuestionBuilder;
import com.marketinghub.mds.search.MechanismCandidateBuilder;
import com.marketinghub.mds.search.PracticalKnowledgePackBuilder;
import com.marketinghub.mds.search.ScreenedEvidence;
import com.marketinghub.mds.search.SearchExecutionService;
import com.marketinghub.mds.search.SearchQueryPlan;
import com.marketinghub.mds.search.SearchQueryPlanBuilder;
import com.marketinghub.mds.search.SourceDedupService;
import com.marketinghub.mds.search.SourceSearchHit;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class MechanismDiscoveryPipelineService {
    private static final Logger log = LoggerFactory.getLogger(MechanismDiscoveryPipelineService.class);
    private final BackendMdsClient backendMdsClient;
    private final MechanismQuestionBuilder mechanismQuestionBuilder;
    private final SearchQueryPlanBuilder searchQueryPlanBuilder;
    private final SearchExecutionService searchExecutionService;
    private final SourceDedupService sourceDedupService;
    private final EvidenceScreeningService evidenceScreeningService;
    private final EvidenceConfidenceService evidenceConfidenceService;
    private final EvidenceItemBuilder evidenceItemBuilder;
    private final MechanismCandidateBuilder mechanismCandidateBuilder;
    private final PracticalKnowledgePackBuilder practicalKnowledgePackBuilder;
    private final DiscoveryReportBuilder discoveryReportBuilder;
    private final MeterRegistry meterRegistry;

    public MechanismDiscoveryPipelineService(BackendMdsClient backendMdsClient,
                                             MechanismQuestionBuilder mechanismQuestionBuilder,
                                             SearchQueryPlanBuilder searchQueryPlanBuilder,
                                             SearchExecutionService searchExecutionService,
                                             SourceDedupService sourceDedupService,
                                             EvidenceScreeningService evidenceScreeningService,
                                             EvidenceConfidenceService evidenceConfidenceService,
                                             EvidenceItemBuilder evidenceItemBuilder,
                                             MechanismCandidateBuilder mechanismCandidateBuilder,
                                             PracticalKnowledgePackBuilder practicalKnowledgePackBuilder,
                                             DiscoveryReportBuilder discoveryReportBuilder,
                                             MeterRegistry meterRegistry) {
        this.backendMdsClient = backendMdsClient;
        this.mechanismQuestionBuilder = mechanismQuestionBuilder;
        this.searchQueryPlanBuilder = searchQueryPlanBuilder;
        this.searchExecutionService = searchExecutionService;
        this.sourceDedupService = sourceDedupService;
        this.evidenceScreeningService = evidenceScreeningService;
        this.evidenceConfidenceService = evidenceConfidenceService;
        this.evidenceItemBuilder = evidenceItemBuilder;
        this.mechanismCandidateBuilder = mechanismCandidateBuilder;
        this.practicalKnowledgePackBuilder = practicalKnowledgePackBuilder;
        this.discoveryReportBuilder = discoveryReportBuilder;
        this.meterRegistry = meterRegistry;
    }

    public void execute(BackendMdsRequestDto request) {
        Timer.Sample pipelineTimer = Timer.start(meterRegistry);
        try {
            MechanismQuestion question = mechanismQuestionBuilder.build(request);
            List<SearchQueryPlan> plans = searchQueryPlanBuilder.buildPlans(question);
            List<SourceSearchHit> rawHits = timeStage("search", () -> searchExecutionService.execute(plans));

            List<SourceSearchHit> dedupedHits = timeStage("dedup-normalize", () -> sourceDedupService.deduplicate(rawHits));
            backendMdsClient.heartbeat(request.id(), new com.marketinghub.mds.dto.BackendHeartbeatRequestDto(
                    "dedup-normalize",
                    "deduplication finished",
                    Map.of("rawCount", rawHits.size(), "dedupedCount", dedupedHits.size())
            ));

            List<ScreenedEvidence> screened = timeStage("screening", () -> evidenceScreeningService.screen(request, dedupedHits));
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
            var sourcePublishResponse = timeStage("source-publish",
                    () -> backendMdsClient.publishBatch(new BackendArtifactPublishBatchRequestDto(request.id(), sourceBatch)));

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
                var evidencePublishResponse = timeStage("evidence-publish",
                        () -> backendMdsClient.publishBatch(new BackendArtifactPublishBatchRequestDto(request.id(), evidenceItems)));
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

            var mechanismBuild = timeStage("mechanism-building", () -> mechanismCandidateBuilder.build(
                request.id(),
                prioritized,
                confidenceBySourceDocumentId,
                evidenceArtifactIdsBySourceDocumentId
            ));
            Long mechanismSpecArtifactId = null;
            Long practicalKnowledgePackArtifactId = null;
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
                    var mechanismSpecResponse = backendMdsClient.publishBatch(
                            new BackendArtifactPublishBatchRequestDto(request.id(), List.of(mechanismSpec))
                    );
                    if (!mechanismSpecResponse.artifactIds().isEmpty()) {
                        mechanismSpecArtifactId = mechanismSpecResponse.artifactIds().get(0);
                    }

                    List<Long> practicalPackParents = new ArrayList<>(mechanismBuild.supportingEvidenceArtifactIds());
                    if (mechanismSpecArtifactId != null) {
                        practicalPackParents.add(mechanismSpecArtifactId);
                    }
                    ArtifactPayloadDto practicalKnowledgePack = timeStage("pack-building", () -> practicalKnowledgePackBuilder.build(
                            request.id(),
                            mechanismBuild.mechanismSpecDraft(),
                            practicalPackParents
                    ));
                    var practicalKnowledgePackResponse = backendMdsClient.publishBatch(
                            new BackendArtifactPublishBatchRequestDto(request.id(), List.of(practicalKnowledgePack))
                    );
                    if (!practicalKnowledgePackResponse.artifactIds().isEmpty()) {
                        practicalKnowledgePackArtifactId = practicalKnowledgePackResponse.artifactIds().get(0);
                    }
                }
            }

            backendMdsClient.heartbeat(request.id(), new com.marketinghub.mds.dto.BackendHeartbeatRequestDto(
                    "pack-building",
                    "mechanism spec and practical knowledge pack published",
                    Map.of("mechanismCandidateCount", mechanismBuild.candidateArtifacts().size())
            ));

            if (mechanismSpecArtifactId != null && practicalKnowledgePackArtifactId != null) {
                List<Long> reportParents = new ArrayList<>(mechanismBuild.supportingEvidenceArtifactIds());
                reportParents.add(mechanismSpecArtifactId);
                reportParents.add(practicalKnowledgePackArtifactId);

                ArtifactPayloadDto discoveryReport = discoveryReportBuilder.build(
                        request.id(),
                        question.text(),
                        rawHits.size(),
                        dedupedHits.size(),
                        prioritized.size(),
                        evidenceItems.size(),
                        mechanismBuild.candidateArtifacts().size(),
                        mechanismSpecArtifactId,
                        practicalKnowledgePackArtifactId,
                        reportParents
                );
                backendMdsClient.publishBatch(new BackendArtifactPublishBatchRequestDto(request.id(), List.of(discoveryReport)));
                backendMdsClient.heartbeat(request.id(), new com.marketinghub.mds.dto.BackendHeartbeatRequestDto(
                        "reporting",
                        "mechanism discovery report published",
                        Map.of("reportPublished", true)
                ));
            }

            log.info(
                    "mds-pipeline-summary requestId={} market={} problem={} desiredOutcome={} searchSources={} selectedEvidenceCount={} mechanismCandidateCount={} chosenMechanismId={} confidenceLevel={}",
                    request.id(),
                    request.market(),
                    request.problem(),
                    request.desiredOutcome(),
                    plans.stream().map(SearchQueryPlan::source).toList(),
                    prioritized.size(),
                    mechanismBuild.candidateArtifacts().size(),
                    mechanismBuild.mechanismSpecDraft() != null ? mechanismBuild.mechanismSpecDraft().recommendedCandidateKey() : "",
                    mechanismBuild.mechanismSpecDraft() != null ? mechanismBuild.mechanismSpecDraft().confidenceLevel() : ""
            );
        } finally {
            pipelineTimer.stop(meterRegistry.timer("mds.pipeline.duration"));
        }
    }

    private <T> T timeStage(String stage, java.util.function.Supplier<T> supplier) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            T value = supplier.get();
            meterRegistry.counter("mds.pipeline.stage.success", "stage", stage).increment();
            return value;
        } catch (RuntimeException ex) {
            meterRegistry.counter("mds.pipeline.stage.failure", "stage", stage).increment();
            throw ex;
        } finally {
            sample.stop(meterRegistry.timer("mds.pipeline.stage.duration", "stage", stage));
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
