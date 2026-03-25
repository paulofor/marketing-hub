package com.marketinghub.worker.report;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.experiment.report.ExperimentReportStatus;
import com.marketinghub.experiment.report.dto.ExperimentReportMaterialDto;
import com.marketinghub.experiment.report.dto.ExperimentReportRequestDetailDto;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Orquestra o pipeline de geração do relatório objetivo.
 */
@Service
public class ExperimentReportService {

    private static final Logger log = LoggerFactory.getLogger(ExperimentReportService.class);
    private static final int MAX_FAILURE_REASON = 2_000;

    private final ExperimentReportBackendClient backendClient;
    private final ExperimentReportRenderer renderer;
    private final ExperimentReportStorageClient storageClient;
    private final ObjectMapper objectMapper;
    private final ExperimentReportProperties properties;

    public ExperimentReportService(ExperimentReportBackendClient backendClient,
                                   ExperimentReportRenderer renderer,
                                   ExperimentReportStorageClient storageClient,
                                   ObjectMapper objectMapper,
                                   ExperimentReportProperties properties) {
        this.backendClient = backendClient;
        this.renderer = renderer;
        this.storageClient = storageClient;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    public void processPendingRequests() {
        if (!properties.isEnabled()) {
            log.debug("Geração de relatórios desabilitada via configuração");
            return;
        }
        List<ExperimentReportRequestDetailDto> pending = backendClient.fetchPendingRequests();
        if (pending.isEmpty()) {
            return;
        }
        int limit = properties.getMaxRequestsPerRun() <= 0 ? Integer.MAX_VALUE : properties.getMaxRequestsPerRun();
        AtomicInteger processed = new AtomicInteger();
        pending.stream()
                .limit(limit)
                .forEach(request -> {
                    if (processSingleRequest(request)) {
                        processed.incrementAndGet();
                    }
                });
        if (processed.get() > 0) {
            log.info("Relatórios de experimento processados: {}", processed.get());
        }
    }

    private boolean processSingleRequest(ExperimentReportRequestDetailDto request) {
        boolean processingStarted = false;
        try {
            backendClient.updateStatus(request.getId(), ExperimentReportStatus.PROCESSING, null, null);
            processingStarted = true;
            ExperimentReportMaterialDto material = parseSnapshot(request);
            ExperimentReportRenderer.RenderedExperimentReport rendered = renderer.render(request, material);
            ExperimentReportStorageClient.StoredReport stored =
                    storageClient.upload(rendered.content(), rendered.filename(), rendered.contentType());
            backendClient.updateStatus(request.getId(), ExperimentReportStatus.READY, stored.publicUrl(), null);
            return true;
        } catch (Exception ex) {
            log.error("Falha ao gerar relatório do experimento {} (request {})", request.getExperimentId(), request.getId(), ex);
            if (processingStarted) {
                backendClient.updateStatus(request.getId(), ExperimentReportStatus.FAILED, null, failureReason(ex));
            }
            return false;
        }
    }

    private ExperimentReportMaterialDto parseSnapshot(ExperimentReportRequestDetailDto request) throws JsonProcessingException {
        if (!StringUtils.hasText(request.getPayloadSnapshot())) {
            throw new IllegalStateException("Snapshot de dados ausente para a solicitação " + request.getId());
        }
        return objectMapper.readValue(request.getPayloadSnapshot(), ExperimentReportMaterialDto.class);
    }

    private String failureReason(Exception ex) {
        String message = ex.getMessage();
        if (!StringUtils.hasText(message) && ex.getCause() != null) {
            message = ex.getCause().getMessage();
        }
        if (!StringUtils.hasText(message)) {
            message = ex.getClass().getSimpleName();
        }
        String reason = "Falha ao montar o relatório: " + message;
        if (reason.length() > MAX_FAILURE_REASON) {
            return reason.substring(0, MAX_FAILURE_REASON) + "...";
        }
        return reason;
    }
}
