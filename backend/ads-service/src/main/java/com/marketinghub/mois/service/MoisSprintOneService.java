package com.marketinghub.mois.service;

import com.marketinghub.mois.dto.MoisWorkspaceDtos;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Service;

@Service
public class MoisSprintOneService {

    private static final String DEFAULT_STAGE = "COLETA";

    private final ConcurrentMap<String, MoisWorkspaceDtos.ReferenceResponse> referencesById = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, MoisWorkspaceDtos.ExtractionDraftResponse> extractionByReferenceId =
            new ConcurrentHashMap<>();

    public MoisWorkspaceDtos.WorkspaceDashboardResponse getDashboard(String workspaceId) {
        List<MoisWorkspaceDtos.ReferenceResponse> workspaceReferences = listWorkspaceReferences(workspaceId);
        int extractions = (int) workspaceReferences.stream()
                .filter(reference -> extractionByReferenceId.containsKey(reference.referenceId()))
                .count();

        List<MoisWorkspaceDtos.RecentAnalysisResponse> recentAnalyses = workspaceReferences.stream()
                .sorted(Comparator.comparing(MoisWorkspaceDtos.ReferenceResponse::createdAt).reversed())
                .limit(10)
                .map(reference -> new MoisWorkspaceDtos.RecentAnalysisResponse(
                        reference.referenceId(),
                        reference.niche(),
                        extractionByReferenceId.containsKey(reference.referenceId()) ? "EXTRACTION_DRAFT" : "COLETA_CONCLUIDA",
                        reference.createdAt()))
                .toList();

        return new MoisWorkspaceDtos.WorkspaceDashboardResponse(
                workspaceId,
                new MoisWorkspaceDtos.WorkspaceKpisResponse(workspaceReferences.size(), extractions, 0, 0),
                DEFAULT_STAGE,
                recentAnalyses
        );
    }

    public MoisWorkspaceDtos.ReferenceResponse createReference(MoisWorkspaceDtos.CreateReferenceRequest request) {
        Instant now = Instant.now();
        MoisWorkspaceDtos.ReferenceResponse created = new MoisWorkspaceDtos.ReferenceResponse(
                UUID.randomUUID().toString(),
                request.workspaceId(),
                request.niche(),
                request.sourceUrl(),
                request.assetType(),
                request.primaryPromise(),
                request.awarenessStage(),
                request.priceRange(),
                request.formatType(),
                request.notes(),
                now
        );
        referencesById.put(created.referenceId(), created);
        return created;
    }

    public MoisWorkspaceDtos.ReferenceListResponse listReferences(String workspaceId) {
        List<MoisWorkspaceDtos.ReferenceResponse> references = listWorkspaceReferences(workspaceId).stream()
                .sorted(Comparator.comparing(MoisWorkspaceDtos.ReferenceResponse::createdAt).reversed())
                .toList();
        return new MoisWorkspaceDtos.ReferenceListResponse(references);
    }

    public MoisWorkspaceDtos.ExtractionDraftResponse upsertExtractionDraft(
            String referenceId,
            MoisWorkspaceDtos.UpsertExtractionDraftRequest request
    ) {
        MoisWorkspaceDtos.ReferenceResponse reference = referencesById.get(referenceId);
        if (reference == null) {
            throw new IllegalArgumentException("reference not found");
        }

        MoisWorkspaceDtos.ExtractionDraftResponse draft = new MoisWorkspaceDtos.ExtractionDraftResponse(
                UUID.randomUUID().toString(),
                referenceId,
                hasAnyDraftContent(request) ? "DRAFT" : "EMPTY_DRAFT",
                Instant.now()
        );
        extractionByReferenceId.put(referenceId, draft);
        return draft;
    }

    private List<MoisWorkspaceDtos.ReferenceResponse> listWorkspaceReferences(String workspaceId) {
        List<MoisWorkspaceDtos.ReferenceResponse> references = new ArrayList<>();
        for (MoisWorkspaceDtos.ReferenceResponse reference : referencesById.values()) {
            if (reference.workspaceId().equals(workspaceId)) {
                references.add(reference);
            }
        }
        return references;
    }

    private boolean hasAnyDraftContent(MoisWorkspaceDtos.UpsertExtractionDraftRequest request) {
        return isNotBlank(request.pain())
                || isNotBlank(request.result())
                || isNotBlank(request.mechanism())
                || isNotBlank(request.proof())
                || isNotBlank(request.offer())
                || (request.evidenceItems() != null && !request.evidenceItems().isEmpty());
    }

    private boolean isNotBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
