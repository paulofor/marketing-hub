package com.marketinghub.worker.learning;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.experiment.learning.ExperimentLearningStatus;
import com.marketinghub.experiment.learning.dto.ExperimentLearningPayloadDto;
import com.marketinghub.experiment.learning.dto.ExperimentLearningRequestDetailDto;
import com.marketinghub.experiment.report.dto.ExperimentReportMaterialDto;
import com.marketinghub.worker.learning.exception.BackendClientException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Coordena o fluxo completo: buscar solicitações, gerar aprendizado com IA e atualizar o backend.
 */
@Service
public class ExperimentLearningJobService {

    private static final Logger log = LoggerFactory.getLogger(ExperimentLearningJobService.class);
    private static final int MAX_FAILURE_REASON = 2_000;

    private final ExperimentLearningBackendClient backendClient;
    private final ExperimentLearningChatGptClient chatGptClient;
    private final ExperimentLearningProperties properties;
    private final ObjectMapper objectMapper;

    public ExperimentLearningJobService(ExperimentLearningBackendClient backendClient,
                                        ExperimentLearningChatGptClient chatGptClient,
                                        ExperimentLearningProperties properties,
                                        ObjectMapper objectMapper) {
        this.backendClient = backendClient;
        this.chatGptClient = chatGptClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public void processPendingRequests() {
        if (!properties.isEnabled()) {
            log.debug("Processamento de aprendizados desabilitado via configuração");
            return;
        }
        List<ExperimentLearningRequestDetailDto> pending = backendClient.fetchPendingRequests();
        if (pending.isEmpty()) {
            return;
        }
        int max = properties.getMaxRequestsPerRun() > 0 ? properties.getMaxRequestsPerRun() : Integer.MAX_VALUE;
        AtomicInteger processed = new AtomicInteger();
        pending.stream()
                .limit(max)
                .forEach(request -> {
                    if (handleSingleRequest(request)) {
                        processed.incrementAndGet();
                    }
                });
        if (processed.get() > 0) {
            log.info("Aprendizados processados pelo worker: {}", processed.get());
        }
    }

    private boolean handleSingleRequest(ExperimentLearningRequestDetailDto request) {
        boolean processingStarted = false;
        try {
            backendClient.updateStatus(request.getId(), ExperimentLearningStatus.PROCESSING, null, null);
            processingStarted = true;
            ExperimentReportMaterialDto material = parseSnapshot(request);
            ExperimentLearningChatGptClient.GenerationResult generation =
                    chatGptClient.generateLearning(request.getId(), material);
            backendClient.updateStatus(request.getId(), ExperimentLearningStatus.READY, generation.payload(), null);
            log.info("Aprendizado concluído para experimento {}", request.getExperimentId());
            return true;
        } catch (Exception ex) {
            log.error("Falha ao gerar aprendizado para experimento {} (request {})", request.getExperimentId(), request.getId(), ex);
            if (processingStarted) {
                backendClient.updateStatus(request.getId(), ExperimentLearningStatus.FAILED, null, failureReason(ex));
            }
            return false;
        }
    }

    private ExperimentReportMaterialDto parseSnapshot(ExperimentLearningRequestDetailDto request) {
        if (!StringUtils.hasText(request.getPayloadSnapshot())) {
            throw new BackendClientException("Snapshot de dados ausente para a solicitação " + request.getId());
        }
        try {
            return objectMapper.readValue(request.getPayloadSnapshot(), ExperimentReportMaterialDto.class);
        } catch (JsonProcessingException ex) {
            throw new BackendClientException("Snapshot inválido para a solicitação " + request.getId());
        }
    }

    private String failureReason(Exception ex) {
        String message = ex.getMessage();
        if (!StringUtils.hasText(message) && ex.getCause() != null) {
            message = ex.getCause().getMessage();
        }
        if (!StringUtils.hasText(message)) {
            message = ex.getClass().getSimpleName();
        }
        String reason = "Falha ao gerar aprendizado: " + message;
        if (reason.length() > MAX_FAILURE_REASON) {
            return reason.substring(0, MAX_FAILURE_REASON) + "...";
        }
        return reason;
    }
}
