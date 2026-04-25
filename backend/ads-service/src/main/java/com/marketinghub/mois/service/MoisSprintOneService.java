package com.marketinghub.mois.service;

import com.marketinghub.mois.dto.MoisWorkspaceDtos;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
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
    private final ConcurrentMap<String, MoisWorkspaceDtos.LibraryBlockResponse> libraryBlocksById = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, MoisWorkspaceDtos.ComparisonResponse> comparisonsById = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, MoisWorkspaceDtos.BuildOfferResponse> offersById = new ConcurrentHashMap<>();

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

    public MoisWorkspaceDtos.LibraryBlockListResponse listLibraryBlocks(String workspaceId, String niche, String formatType) {
        seedLibraryIfNeeded(workspaceId);
        List<MoisWorkspaceDtos.LibraryBlockResponse> blocks = new ArrayList<>();
        for (MoisWorkspaceDtos.LibraryBlockResponse block : libraryBlocksById.values()) {
            boolean sameWorkspace = workspaceId == null || workspaceId.isBlank() || workspaceId.equals(block.workspaceId());
            boolean matchesNiche = niche == null || niche.isBlank() || block.tags().contains(niche);
            boolean matchesFormat = formatType == null || formatType.isBlank() || block.tags().contains(formatType);
            if (sameWorkspace && matchesNiche && matchesFormat) {
                blocks.add(block);
            }
        }

        blocks.sort(Comparator.comparing(MoisWorkspaceDtos.LibraryBlockResponse::updatedAt).reversed());
        return new MoisWorkspaceDtos.LibraryBlockListResponse(blocks);
    }

    public MoisWorkspaceDtos.LibraryBlockActionResponse favoriteLibraryBlock(String blockId) {
        MoisWorkspaceDtos.LibraryBlockResponse block = getLibraryBlockOrThrow(blockId);
        MoisWorkspaceDtos.LibraryBlockResponse updated = new MoisWorkspaceDtos.LibraryBlockResponse(
                block.blockId(),
                block.workspaceId(),
                block.type(),
                block.summary(),
                block.tags(),
                block.score(),
                block.origin(),
                true,
                Instant.now()
        );
        libraryBlocksById.put(blockId, updated);
        return new MoisWorkspaceDtos.LibraryBlockActionResponse(blockId, "FAVORITE", "OK", updated.updatedAt());
    }

    public MoisWorkspaceDtos.LibraryBlockActionResponse duplicateLibraryBlock(String blockId) {
        MoisWorkspaceDtos.LibraryBlockResponse source = getLibraryBlockOrThrow(blockId);
        String duplicateId = UUID.randomUUID().toString();
        MoisWorkspaceDtos.LibraryBlockResponse duplicate = new MoisWorkspaceDtos.LibraryBlockResponse(
                duplicateId,
                source.workspaceId(),
                source.type(),
                source.summary() + " (cópia)",
                source.tags(),
                source.score(),
                "DUPLICATED_FROM_" + source.blockId(),
                false,
                Instant.now()
        );
        libraryBlocksById.put(duplicateId, duplicate);
        return new MoisWorkspaceDtos.LibraryBlockActionResponse(duplicateId, "DUPLICATE", "OK", duplicate.updatedAt());
    }

    public MoisWorkspaceDtos.ComparisonResponse createComparison(MoisWorkspaceDtos.CreateComparisonRequest request) {
        String comparisonId = UUID.randomUUID().toString();
        List<MoisWorkspaceDtos.ComparisonDimensionResponse> dimensions = List.of(
                new MoisWorkspaceDtos.ComparisonDimensionResponse(
                        "PROMESSA", "Resultado em 8 semanas", "Resultado sem prazo", "Adicionar prazo específico"),
                new MoisWorkspaceDtos.ComparisonDimensionResponse(
                        "MECANISMO", "Protocolo em 3 etapas", "Método genérico", "Explicitar etapas"),
                new MoisWorkspaceDtos.ComparisonDimensionResponse(
                        "PROVA", "Depoimentos com números", "Sem dados concretos", "Anexar provas mensuráveis"),
                new MoisWorkspaceDtos.ComparisonDimensionResponse(
                        "LAYOUT", "Fluxo com CTA principal", "Múltiplos CTAs", "Reduzir atrito visual")
        );
        List<MoisWorkspaceDtos.ComparisonScorecardResponse> scorecards = List.of(
                new MoisWorkspaceDtos.ComparisonScorecardResponse("clareza", 72, "Promessa compreensível, porém ampla."),
                new MoisWorkspaceDtos.ComparisonScorecardResponse("prova", 46, "Faltam evidências verificáveis."),
                new MoisWorkspaceDtos.ComparisonScorecardResponse("coerencia", 69, "Narrativa parcialmente alinhada ao mecanismo."),
                new MoisWorkspaceDtos.ComparisonScorecardResponse("atrito", 58, "Existem etapas redundantes no fluxo.")
        );
        List<MoisWorkspaceDtos.ComparisonImprovementResponse> improvements = List.of(
                new MoisWorkspaceDtos.ComparisonImprovementResponse(UUID.randomUUID().toString(), "HIGH", "Adicionar seção de prova com antes/depois."),
                new MoisWorkspaceDtos.ComparisonImprovementResponse(UUID.randomUUID().toString(), "MEDIUM", "Reescrever headline com prazo e público."),
                new MoisWorkspaceDtos.ComparisonImprovementResponse(UUID.randomUUID().toString(), "MEDIUM", "Consolidar CTAs em um único caminho.")
        );
        MoisWorkspaceDtos.ComparisonResponse comparison = new MoisWorkspaceDtos.ComparisonResponse(
                comparisonId,
                request.workspaceId(),
                dimensions,
                scorecards,
                improvements
        );
        comparisonsById.put(comparisonId, comparison);
        return comparison;
    }

    public MoisWorkspaceDtos.BuildOfferResponse buildOffer(MoisWorkspaceDtos.BuildOfferRequest request) {
        boolean hasPain = request.currentVersion().toLowerCase().contains("dor");
        boolean hasResult = request.currentVersion().toLowerCase().contains("resultado");
        boolean hasMechanism = request.currentVersion().toLowerCase().contains("mecanismo");
        boolean hasProof = request.currentVersion().toLowerCase().contains("prova");
        boolean hasOffer = request.currentVersion().toLowerCase().contains("oferta");

        Map<String, Boolean> checklist = Map.of(
                "dor", hasPain,
                "resultado", hasResult,
                "mecanismo", hasMechanism,
                "prova", hasProof,
                "oferta", hasOffer
        );
        String proposed = request.currentVersion()
                + "\n\n## Versão proposta\n- Blocos selecionados: "
                + (request.selectedBlockIds() == null ? 0 : request.selectedBlockIds().size())
                + "\n- Reforçar Dor → Resultado → Mecanismo → Prova → Oferta.";
        MoisWorkspaceDtos.BuildOfferResponse response = new MoisWorkspaceDtos.BuildOfferResponse(
                UUID.randomUUID().toString(),
                request.workspaceId(),
                checklist.containsValue(false) ? "INCOMPLETE_CHECKLIST" : "READY_TO_EXPORT",
                proposed,
                checklist,
                Instant.now()
        );
        offersById.put(response.offerId(), response);
        return response;
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

    private MoisWorkspaceDtos.LibraryBlockResponse getLibraryBlockOrThrow(String blockId) {
        MoisWorkspaceDtos.LibraryBlockResponse block = libraryBlocksById.get(blockId);
        if (block == null) {
            throw new IllegalArgumentException("library block not found");
        }
        return block;
    }

    private void seedLibraryIfNeeded(String workspaceId) {
        if (!libraryBlocksById.isEmpty()) {
            return;
        }
        String effectiveWorkspace = workspaceId == null || workspaceId.isBlank() ? "workspace-default" : workspaceId;
        Instant now = Instant.now();
        MoisWorkspaceDtos.LibraryBlockResponse promise = new MoisWorkspaceDtos.LibraryBlockResponse(
                UUID.randomUUID().toString(),
                effectiveWorkspace,
                "PROMISE",
                "Headline com resultado e prazo explícito.",
                List.of("nutricao-esportiva", "CURSO"),
                0.82,
                "MARKET_REFERENCE",
                false,
                now
        );
        MoisWorkspaceDtos.LibraryBlockResponse proof = new MoisWorkspaceDtos.LibraryBlockResponse(
                UUID.randomUUID().toString(),
                effectiveWorkspace,
                "PROOF",
                "Prova social com dados antes/depois auditáveis.",
                List.of("nutricao-esportiva", "MENTORIA"),
                0.77,
                "MARKET_REFERENCE",
                false,
                now.minusSeconds(300)
        );
        libraryBlocksById.put(promise.blockId(), promise);
        libraryBlocksById.put(proof.blockId(), proof);
    }

}
