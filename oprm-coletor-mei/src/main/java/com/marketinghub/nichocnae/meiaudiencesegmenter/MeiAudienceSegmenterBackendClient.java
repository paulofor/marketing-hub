package com.marketinghub.nichocnae.meiaudiencesegmenter;

import com.marketinghub.oprmcoletormei.marketimport.config.OprmMarketImportCollectorProperties;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Chama exclusivamente endpoints OPRM nichocnae do backend para executar a segmentação MEI/autônomo. */
@Component
public class MeiAudienceSegmenterBackendClient {
    private static final Logger log = LoggerFactory.getLogger(MeiAudienceSegmenterBackendClient.class);
    private static final String PENDING_PATH = "/api/internal/oprm/nichocnae/mei-audience-segmenter/stage-executions/pending";
    private static final String COMPLETE_PATH_PREFIX = "/api/internal/oprm/nichocnae/mei-audience-segmenter/stage-executions/";
    private static final String COMPLETE_PATH_SUFFIX = "/complete";
    private static final String FAIL_PATH_SUFFIX = "/fail";
    private static final String SEGMENTED_BY = "oprmMeiAudienceSegmenter";

    private final OprmMarketImportCollectorProperties collectorProperties;
    private final RestClient restClient;

    /** Inicializa o cliente com a URL base do backend e RestClient compartilhado do coletor. */
    public MeiAudienceSegmenterBackendClient(OprmMarketImportCollectorProperties collectorProperties, RestClient restClient) {
        this.collectorProperties = collectorProperties;
        this.restClient = restClient;
    }

    /** Lista ciclos pendentes que a etapa deve segmentar em perfis comportamentais MEI/autônomo. */
    public List<MeiAudienceSegmenterPending> listPendingCycles() {
        String url = collectorProperties.backendBaseUrl() + PENDING_PATH;
        try {
            MeiAudienceSegmenterPending[] response = restClient.get().uri(url).retrieve().body(MeiAudienceSegmenterPending[].class);
            List<MeiAudienceSegmenterPending> pendingCycles = response == null ? List.of() : Arrays.asList(response);
            log.info("Pendências da segmentação MEI/autônomo carregadas (endpoint={}, pendingCount={})", url, pendingCycles.size());
            return pendingCycles;
        } catch (RestClientException ex) {
            log.error("Erro ao listar pendências da segmentação MEI/autônomo (endpoint={})", url, ex);
            throw ex;
        }
    }

    /** Envia ao backend o perfil segmentado para persistência contratual. */
    public MeiAudienceSegmenterOutput completeStageExecution(MeiAudienceSegmenterPending pending, MeiAudienceSegmentDraft draft) {
        String url = collectorProperties.backendBaseUrl() + COMPLETE_PATH_PREFIX + pending.researchCycleId() + COMPLETE_PATH_SUFFIX;
        MeiAudienceSegmenterCompletionRequest request = toCompletionRequest(pending, draft);
        try {
            MeiAudienceSegmenterCompletionResponse response = restClient.post()
                    .uri(url)
                    .body(request)
                    .retrieve()
                    .body(MeiAudienceSegmenterCompletionResponse.class);
            if (response == null) {
                throw new IllegalStateException("Backend OPRM nichocnae retornou corpo vazio ao concluir segmentação MEI/autônomo.");
            }
            return new MeiAudienceSegmenterOutput(
                    response.profileId(), response.researchCycleId(), response.routineCardId(), response.cycleStatus(), response.audienceName(),
                    response.autonomousProfessionalFitScore(), response.behavioralEvidenceScore(), response.sourceFreshnessScore(), response.updatedAt());
        } catch (RestClientException | IllegalStateException ex) {
            log.error(
                    "Erro ao concluir segmentação MEI/autônomo no backend (endpoint={}, researchCycleId={}, routineCardId={})",
                    url,
                    pending.researchCycleId(),
                    pending.routineCardId(),
                    ex);
            throw ex;
        }
    }

    /** Notifica o backend que a segmentação MEI/autônomo falhou. */
    public void failStageExecution(MeiAudienceSegmenterPending pending, RuntimeException error) {
        String url = collectorProperties.backendBaseUrl() + COMPLETE_PATH_PREFIX + pending.researchCycleId() + FAIL_PATH_SUFFIX;
        String message = error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
        try {
            restClient.post().uri(url).body(new MeiAudienceSegmenterFailureRequest(message)).retrieve().toBodilessEntity();
        } catch (RestClientException ex) {
            log.error(
                    "Erro ao notificar falha da segmentação MEI/autônomo no backend (endpoint={}, researchCycleId={})",
                    url,
                    pending.researchCycleId(),
                    ex);
            throw ex;
        }
    }

    /** Converte o rascunho segmentado para o contrato do endpoint complete do backend. */
    MeiAudienceSegmenterCompletionRequest toCompletionRequest(MeiAudienceSegmenterPending pending, MeiAudienceSegmentDraft draft) {
        return new MeiAudienceSegmenterCompletionRequest(
                pending.researchCycleId(),
                pending.routineCardId(),
                pending.sourceNicheId(),
                pending.cnaeCode(),
                pending.cnaeDescription(),
                pending.neutralNicheName(),
                draft.audienceName(),
                draft.occupationTerms(),
                draft.workMode(),
                draft.customerAcquisitionBehavior(),
                draft.dailyRoutineSummary(),
                draft.recurringTasksSummary(),
                draft.operationalPainsSummary(),
                draft.emotionalPainsSummary(),
                draft.dreamsSummary(),
                draft.fearsSummary(),
                draft.languagePatterns(),
                draft.channelsUsed(),
                draft.recentSourceSummary(),
                draft.autonomousProfessionalFitScore(),
                draft.behavioralEvidenceScore(),
                draft.sourceFreshnessScore(),
                draft.outdatedSourceRiskScore(),
                draft.structuredBusinessDriftRiskScore(),
                draft.solutionLanguageRiskScore(),
                SEGMENTED_BY);
    }
}
