package com.marketinghub.mois.bibliotecapaginavenda.worker.v1.service;

import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.client.BackendClient;
import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.config.WorkerProperties;
import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.model.WorkerDtos.*;
import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.openai.OpenAiSalesPageAnalyzer;
import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.openai.SalesPageAnalysisResult;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class PipelineRunner {
    private final BackendClient backendClient;
    private final WorkerProperties properties;
    private final OpenAiSalesPageAnalyzer openAiSalesPageAnalyzer;
    private final AtomicInteger sourceCursor = new AtomicInteger(0);

    @Scheduled(fixedDelayString = "${worker.poll-interval-ms:15000}")
    public void runCycle() {
        String sourceForCycle = resolveSourceForCycle();
        log.info("MOIS sales-library worker cycle started. workspaceId={}, source={}, pollIntervalMs={}, requestTimeoutMs={}",
                properties.workspaceId(), sourceForCycle, properties.pollIntervalMs(), properties.requestTimeoutMs());
        ClaimResponse claim = backendClient.claim(new ClaimRequest(properties.workspaceId(), sourceForCycle));
        if (claim == null || !claim.claimed() || claim.job() == null) {
            log.info("MOIS sales-library worker cycle finished without claimed job.");
            return;
        }

        long jobId = claim.job().jobId();
        try {
            log.info("MOIS sales-library worker claimed job. jobId={}, pageId={}, urlCanonical={}",
                    jobId, claim.job().pageId(), claim.job().urlCanonical());
            var doc = Jsoup.connect(claim.job().urlCanonical()).timeout(properties.requestTimeoutMs()).get();
            String text = doc.body() != null ? doc.body().text() : "";
            SalesPageAnalysisResult analysis =
                    openAiSalesPageAnalyzer.analyze(claim.job().pageId(), claim.job().urlCanonical(), text);
            backendClient.complete(jobId, new CompleteRequest(
                    analysis.scoreTotal(),
                    analysis.sectionsJson(),
                    analysis.copyJson(),
                    analysis.visualJson(),
                    analysis.imageJson(),
                    analysis.analysisNotes(),
                    analysis.parserVersion(),
                    analysis.promptVersion(),
                    analysis.modelName(),
                    Instant.now()));
            log.info("MOIS library job {} completed for page {}", jobId, claim.job().pageId());
        } catch (Exception ex) {
            backendClient.fail(jobId, new FailRequest("PIPELINE_ERROR", ex.getMessage()));
            log.warn("MOIS library job {} failed: {}", jobId, ex.getMessage(), ex);
        }
    }

    String resolveSourceForCycle() {
        List<String> configuredSources = parseSources(properties.sources());
        if (!configuredSources.isEmpty()) {
            int index = Math.floorMod(sourceCursor.getAndIncrement(), configuredSources.size());
            return configuredSources.get(index);
        }
        return normalizeSource(properties.source());
    }

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

    private static String normalizeSource(String source) {
        return source == null ? "" : source.trim().toUpperCase(Locale.ROOT);
    }
}
