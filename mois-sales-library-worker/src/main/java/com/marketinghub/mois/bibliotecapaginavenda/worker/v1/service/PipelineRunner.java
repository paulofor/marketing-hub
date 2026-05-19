package com.marketinghub.mois.bibliotecapaginavenda.worker.v1.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.client.BackendClient;
import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.config.WorkerProperties;
import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.model.WorkerDtos.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service @Slf4j @RequiredArgsConstructor
public class PipelineRunner {
    private final BackendClient backendClient;
    private final WorkerProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Scheduled(fixedDelayString = "${worker.poll-interval-ms:15000}")
    public void runCycle() {
        log.info("MOIS sales-library worker cycle started. workspaceId={}, source={}, pollIntervalMs={}, requestTimeoutMs={}",
                properties.workspaceId(), properties.source(), properties.pollIntervalMs(), properties.requestTimeoutMs());
        ClaimResponse claim = backendClient.claim(new ClaimRequest(properties.workspaceId(), properties.source()));
        if (claim == null || !claim.claimed() || claim.job() == null) {
            log.info("MOIS sales-library worker cycle finished without claimed job.");
            return;
        }
        long jobId = claim.job().jobId();
        try {
            log.info("MOIS sales-library worker claimed job. jobId={}, pageId={}, urlCanonical={}", jobId, claim.job().pageId(), claim.job().urlCanonical());
            var doc = Jsoup.connect(claim.job().urlCanonical()).timeout(properties.requestTimeoutMs()).get();
            String text = doc.body() != null ? doc.body().text() : "";
            Map<String, Object> sections = new HashMap<>();
            sections.put("hasHeadline", doc.select("h1,h2").size() > 0);
            sections.put("hasCta", text.toLowerCase().contains("comprar") || text.toLowerCase().contains("quero") || text.toLowerCase().contains("inscreva"));
            sections.put("hasProof", text.toLowerCase().contains("depoimento") || text.toLowerCase().contains("prova") || text.toLowerCase().contains("resultado"));
            BigDecimal score = BigDecimal.valueOf((Boolean.TRUE.equals(sections.get("hasHeadline")) ? 33 : 0) + (Boolean.TRUE.equals(sections.get("hasCta")) ? 33 : 0) + (Boolean.TRUE.equals(sections.get("hasProof")) ? 34 : 0));
            String sectionsJson = objectMapper.writeValueAsString(sections);
            backendClient.complete(jobId, new CompleteRequest(score, sectionsJson, "{}", "{}", "{}", "Análise inicial automatizada", "html-v1", "heuristic-v1", "local-jsoup", Instant.now()));
            log.info("MOIS library job {} completed for page {}", jobId, claim.job().pageId());
        } catch (Exception ex) {
            backendClient.fail(jobId, new FailRequest("PIPELINE_ERROR", ex.getMessage()));
            log.warn("MOIS library job {} failed: {}", jobId, ex.getMessage(), ex);
        }
    }
}
