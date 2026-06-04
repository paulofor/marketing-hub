package com.marketinghub.nichocnae.signalextractor;

import com.marketinghub.oprmcoletormei.marketimport.config.OprmMarketImportCollectorProperties;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Chama exclusivamente endpoints OPRM nichocnae do backend para executar a etapa cinco pelo coletor. */
@Component
public class SignalExtractorBackendClient {
    private static final Logger log = LoggerFactory.getLogger(SignalExtractorBackendClient.class);
    private static final String PENDING_PATH = "/api/internal/oprm/nichocnae/signal-extractor/stage-executions/pending";
    private static final String COMPLETE_PATH_PREFIX = "/api/internal/oprm/nichocnae/signal-extractor/stage-executions/";
    private static final String COMPLETE_PATH_SUFFIX = "/complete";
    private static final String FAIL_PATH_SUFFIX = "/fail";
    private static final String CREATED_BY = "oprmSignalExtractor";

    private final OprmMarketImportCollectorProperties collectorProperties;
    private final RestClient restClient;

    /** Inicializa o cliente com a URL base do backend e RestClient compartilhado do coletor. */
    public SignalExtractorBackendClient(OprmMarketImportCollectorProperties collectorProperties, RestClient restClient) {
        this.collectorProperties = collectorProperties;
        this.restClient = restClient;
    }

    /** Lista snapshots pendentes que a etapa cinco deve transformar em sinais estruturados. */
    public List<SignalExtractorPending> listPendingSnapshots() {
        String url = collectorProperties.backendBaseUrl() + PENDING_PATH;
        try {
            SignalExtractorPending[] response = restClient.get().uri(url).retrieve().body(SignalExtractorPending[].class);
            List<SignalExtractorPending> pendingSnapshots = response == null ? List.of() : Arrays.asList(response);
            log.info("Pendências da etapa cinco OPRM nichocnae carregadas (endpoint={}, pendingCount={})", url, pendingSnapshots.size());
            return pendingSnapshots;
        } catch (RestClientException ex) {
            log.error("Erro ao listar pendências da etapa cinco OPRM nichocnae (endpoint={})", url, ex);
            throw ex;
        }
    }

    /** Envia ao backend os sinais extraídos para concluir a etapa cinco de um snapshot. */
    public SignalExtractorOutput completeStageExecution(SignalExtractorPending pending, List<ExtractedSignal> signals) {
        String url = collectorProperties.backendBaseUrl()
                + COMPLETE_PATH_PREFIX
                + pending.sourceSnapshotId()
                + COMPLETE_PATH_SUFFIX;
        SignalExtractorCompletionRequest request = toCompletionRequest(pending, signals);
        try {
            SignalExtractorCompletionResponse response = restClient.post()
                    .uri(url)
                    .body(request)
                    .retrieve()
                    .body(SignalExtractorCompletionResponse.class);
            if (response == null) {
                throw new IllegalStateException("Backend OPRM nichocnae retornou corpo vazio ao concluir etapa cinco.");
            }
            return toOutput(response);
        } catch (RestClientException | IllegalStateException ex) {
            log.error(
                    "Erro ao concluir etapa cinco OPRM nichocnae no backend (endpoint={}, sourceSnapshotId={}, researchCycleId={}, signalCount={})",
                    url,
                    pending.sourceSnapshotId(),
                    pending.researchCycleId(),
                    signals == null ? null : signals.size(),
                    ex);
            throw ex;
        }
    }

    /** Notifica o backend que a extração de sinais falhou na etapa cinco. */
    public void failStageExecution(SignalExtractorPending pending, RuntimeException error) {
        String url = collectorProperties.backendBaseUrl()
                + COMPLETE_PATH_PREFIX
                + pending.sourceSnapshotId()
                + FAIL_PATH_SUFFIX;
        String message = error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
        try {
            restClient.post().uri(url).body(new SignalExtractorFailureRequest(message)).retrieve().toBodilessEntity();
        } catch (RestClientException ex) {
            log.error(
                    "Erro ao notificar falha da etapa cinco OPRM nichocnae no backend (endpoint={}, sourceSnapshotId={}, researchCycleId={})",
                    url,
                    pending.sourceSnapshotId(),
                    pending.researchCycleId(),
                    ex);
            throw ex;
        }
    }

    /** Converte os sinais extraídos para o contrato esperado pelo endpoint complete da etapa cinco. */
    SignalExtractorCompletionRequest toCompletionRequest(SignalExtractorPending pending, List<ExtractedSignal> signals) {
        return new SignalExtractorCompletionRequest(
                pending.researchCycleId(),
                pending.sourceCandidateId(),
                pending.sourceDomain(),
                "COMPLETED",
                CREATED_BY,
                signals);
    }

    /** Converte a resposta backend para a saída operacional da etapa cinco no coletor. */
    private SignalExtractorOutput toOutput(SignalExtractorCompletionResponse response) {
        return new SignalExtractorOutput(
                response.sourceSnapshotId(),
                response.researchCycleId(),
                response.signalExtractionStatus(),
                response.extractedSignalCount(),
                response.cycleTotalExtractedSignals(),
                response.signals());
    }
}
