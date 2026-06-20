package com.marketinghub.mois.bibliotecapaginavenda.worker.v1.marketwarmup;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.client.BackendClient;
import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.config.WorkerProperties;
import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.model.WorkerDtos.MarketWarmupClaimRequest;
import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.model.WorkerDtos.MarketWarmupClaimResponse;
import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.model.WorkerDtos.MarketWarmupCompleteRequest;
import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.model.WorkerDtos.MarketWarmupFailRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Executa o ciclo assíncrono da Etapa 3 de aquecimento de mercado via backend principal.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MarketWarmupRunner {
    private final BackendClient backendClient;
    private final WorkerProperties properties;
    private final MarketWarmupProcessor processor;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Reserva um job pendente, coleta fontes públicas, completa o dossiê ou persiste falha operacional.
     */
    @Scheduled(fixedDelayString = "${worker.market-warmup-poll-interval-ms:60000}")
    public void runMarketWarmupCycle() {
        if (!properties.marketWarmupEnabled()) {
            log.debug("MOIS market-warmup worker cycle skipped because it is disabled.");
            return;
        }
        log.info("MOIS market-warmup worker cycle started. workspaceId={}, workerId={}, limit={}, pollIntervalMs={}",
                properties.workspaceId(), properties.marketWarmupWorkerId(), properties.marketWarmupSearchLimit(), properties.marketWarmupPollIntervalMs());
        MarketWarmupClaimResponse claim = backendClient.claimMarketWarmup(new MarketWarmupClaimRequest(properties.workspaceId(), properties.marketWarmupWorkerId()));
        if (claim == null || !claim.claimed() || claim.job() == null) {
            log.info("MOIS market-warmup worker cycle finished without claimed job.");
            return;
        }
        long jobId = claim.job().jobId();
        try {
            log.info("MOIS market-warmup worker claimed job. jobId={}, pageId={}, urlCanonical={}",
                    jobId, claim.job().pageId(), claim.job().urlCanonical());
            MarketWarmupCompleteRequest request = processor.process(claim.job(), resolveSearchLimit());
            log.info("MOIS market-warmup enviando payload ao backend. jobId={}, payload={}", jobId, serializePayload(request));
            backendClient.completeMarketWarmup(jobId, request);
            log.info("MOIS market-warmup worker completed job. jobId={}, pageId={}, sources={}, signals={}",
                    jobId, claim.job().pageId(), request.sources().size(), request.signals().size());
        } catch (Exception ex) {
            backendClient.failMarketWarmup(jobId, new MarketWarmupFailRequest("PUBLIC_SEARCH_ERROR", safeMessage(ex), searchAttempts(ex)));
            log.warn("MOIS market-warmup worker failed job. jobId={}, pageId={}, errorClass={}, errorMessage={}",
                    jobId, claim.job().pageId(), ex.getClass().getName(), ex.getMessage(), ex);
        }
    }

    /**
     * Resolve limite seguro de resultados por query para evitar coleta excessiva na V1.
     */
    private int resolveSearchLimit() {
        Integer configuredLimit = properties.marketWarmupSearchLimit();
        if (configuredLimit == null || configuredLimit < 1) {
            return 6;
        }
        return Math.min(configuredLimit, 20);
    }

    /**
     * Extrai tentativas de busca estruturadas quando a falha ocorreu por falta de fonte qualificada.
     */
    private java.util.List<com.marketinghub.mois.bibliotecapaginavenda.worker.v1.model.WorkerDtos.MarketWarmupSearchAttemptCompleteItem> searchAttempts(Exception ex) {
        if (ex instanceof MarketWarmupNoQualifiedSourcesException noQualifiedSourcesException) {
            return noQualifiedSourcesException.searchAttempts();
        }
        return java.util.List.of();
    }

    /**
     * Garante mensagem operacional persistível mesmo quando a exceção não possui texto.
     */
    private String safeMessage(Exception ex) {
        return ex.getMessage() == null || ex.getMessage().isBlank() ? ex.getClass().getSimpleName() : ex.getMessage();
    }

    /**
     * Serializa o payload final para log operacional antes do envio ao backend.
     */
    private String serializePayload(MarketWarmupCompleteRequest request) {
        try {
            return objectMapper.writeValueAsString(request);
        } catch (Exception ex) {
            log.warn("MOIS market-warmup falhou ao serializar payload para log. errorClass={}, errorMessage={}",
                    ex.getClass().getName(), ex.getMessage(), ex);
            return String.valueOf(request);
        }
    }
}
