package com.marketinghub.mois.bibliotecapaginavenda.worker.v1.service;

import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.client.BackendClient;
import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.config.WorkerProperties;
import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.model.WorkerDtos.*;
import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.pipeline.PipelineWorker;
import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.pipeline.htmlcapture.HtmlCaptureInput;
import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.pipeline.htmlcapture.HtmlCaptureOutput;
import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.pipeline.pageanalysis.PageAnalysisInput;
import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.pipeline.pageanalysis.PageAnalysisOutput;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Executa os ciclos assíncronos do worker MOIS para captura de HTML bruto e análise de páginas de vendas.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PipelineRunner {
    private final BackendClient backendClient;
    private final WorkerProperties properties;
    private final PipelineWorker<HtmlCaptureInput, HtmlCaptureOutput> htmlCapturePipelineWorker;
    private final PipelineWorker<PageAnalysisInput, PageAnalysisOutput> pageAnalysisPipelineWorker;
    private final java.util.concurrent.atomic.AtomicInteger rawHtmlSourceCursor = new java.util.concurrent.atomic.AtomicInteger(0);


    /**
     * Executa a primeira etapa do pipeline da biblioteca: obter HTML bruto versionado para URLs normalizadas.
     */
    @Scheduled(fixedDelayString = "${worker.html-capture-poll-interval-ms:20000}")
    public void runHtmlCapturePipelineCycle() {
        log.info("MOIS htmlcapture pipeline cycle started. workspaceId={}, limit={}, force={}, pollIntervalMs={}",
                properties.workspaceId(), properties.htmlCaptureLimit(), properties.htmlCaptureForce(), properties.htmlCapturePollIntervalMs());
        boolean processed = htmlCapturePipelineWorker.processNext();
        log.info("MOIS htmlcapture pipeline cycle finished. workspaceId={}, processed={}", properties.workspaceId(), processed);
    }

    /**
     * Executa um ciclo da primeira etapa: reservar URL coletada, buscar HTML completo na internet e persistir no backend.
     */
    @Scheduled(fixedDelayString = "${worker.raw-html-poll-interval-ms:30000}")
    public void runRawHtmlCaptureCycle() {
        String sourceForCycle = resolveRawHtmlSourceForCycle();
        log.info("MOIS raw-html capture cycle started. workspaceId={}, source={}, rawHtmlPollIntervalMs={}, requestTimeoutMs={}",
                properties.workspaceId(), sourceForCycle, properties.rawHtmlPollIntervalMs(), properties.requestTimeoutMs());
        CollectedReferenceHtmlClaimResponse claim = backendClient.claimCollectedReferenceHtml(
                new CollectedReferenceHtmlClaimRequest(properties.workspaceId(), sourceForCycle));
        if (claim == null || !claim.claimed() || claim.job() == null) {
            log.info("MOIS raw-html capture cycle finished without claimed reference.");
            return;
        }

        long captureId = claim.job().captureId();
        try {
            log.info("MOIS raw-html worker claimed reference. captureId={}, collectedReferenceId={}, collectionJobId={}, referenceId={}, url={}",
                    captureId, claim.job().collectedReferenceId(), claim.job().collectionJobId(), claim.job().referenceId(), claim.job().url());
            Connection.Response response = Jsoup.connect(claim.job().url())
                    .timeout(properties.requestTimeoutMs())
                    .ignoreContentType(true)
                    .followRedirects(true)
                    .maxBodySize(0)
                    .execute();
            String rawHtml = response.body() == null ? "" : response.body();
            if (rawHtml.isBlank()) {
                throw new IllegalStateException("Resposta sem HTML bruto capturável");
            }
            String finalUrl = response.url() == null ? claim.job().url() : response.url().toString();
            backendClient.completeCollectedReferenceHtml(captureId, new CollectedReferenceHtmlCompleteRequest(
                    rawHtml,
                    finalUrl,
                    response.statusCode(),
                    response.contentType(),
                    Instant.now()));
            log.info("MOIS raw-html capture completed. captureId={}, httpStatus={}, finalUrl={}, rawHtmlChars={}",
                    captureId, response.statusCode(), finalUrl, rawHtml.length());
        } catch (Exception ex) {
            backendClient.failCollectedReferenceHtml(captureId, new CollectedReferenceHtmlFailRequest("RAW_HTML_FETCH_ERROR", ex.getMessage()));
            log.warn("MOIS raw-html capture failed. captureId={}, url={}, errorClass={}, errorMessage={}",
                    captureId, claim.job().url(), ex.getClass().getName(), ex.getMessage(), ex);
        }
    }

    /**
     * Executa um ciclo da etapa de análise de páginas já ingeridas na biblioteca.
     */
    @Scheduled(fixedDelayString = "${worker.poll-interval-ms:15000}")
    public void runCycle() {
        log.info("MOIS pageanalysis pipeline cycle started. workspaceId={}, pollIntervalMs={}, requestTimeoutMs={}",
                properties.workspaceId(), properties.pollIntervalMs(), properties.requestTimeoutMs());
        boolean processed = pageAnalysisPipelineWorker.processNext();
        log.info("MOIS pageanalysis pipeline cycle finished. workspaceId={}, processed={}", properties.workspaceId(), processed);
    }

    /**
     * Escolhe a fonte do ciclo de captura de HTML bruto, com padrão independente da etapa de análise.
     */
    String resolveRawHtmlSourceForCycle() {
        List<String> configuredSources = parseSources(properties.rawHtmlSources());
        if (!configuredSources.isEmpty()) {
            int index = Math.floorMod(rawHtmlSourceCursor.getAndIncrement(), configuredSources.size());
            return configuredSources.get(index);
        }
        return normalizeSource(properties.rawHtmlSource());
    }

    /**
     * Converte a lista textual de fontes em valores normalizados e únicos.
     */
    static List<String> parseSources(String rawSources) {
        if (rawSources == null || rawSources.isBlank()) {
            return List.of();
        }
        return Arrays.stream(rawSources.split(","))
                .map(PipelineRunner::normalizeSource)
                .filter(source -> !source.isBlank())
                .distinct()
                .toList();
    }

    /**
     * Normaliza a fonte para o padrão usado no backend.
     */
    private static String normalizeSource(String source) {
        return source == null ? "" : source.trim().toUpperCase(Locale.ROOT);
    }
}
