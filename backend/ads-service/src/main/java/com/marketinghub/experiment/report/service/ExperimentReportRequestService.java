package com.marketinghub.experiment.report.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.report.ExperimentReportRequest;
import com.marketinghub.experiment.report.ExperimentReportStatus;
import com.marketinghub.experiment.report.dto.UpdateExperimentReportRequest;
import com.marketinghub.repository.jpa.experiment.report.ExperimentReportRequestRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Camada de orquestração das solicitações de relatório.
 */
@Service
public class ExperimentReportRequestService {

    private static final Set<ExperimentReportStatus> ACTIVE_STATUSES = EnumSet.of(
            ExperimentReportStatus.PENDING,
            ExperimentReportStatus.PROCESSING
    );

    private final ExperimentReportRequestRepository repository;
    private final ExperimentRepository experimentRepository;
    private final ExperimentReportMaterialService materialService;
    private final ObjectMapper objectMapper;

    public ExperimentReportRequestService(ExperimentReportRequestRepository repository,
                                          ExperimentRepository experimentRepository,
                                          ExperimentReportMaterialService materialService,
                                          ObjectMapper objectMapper) {
        this.repository = repository;
        this.experimentRepository = experimentRepository;
        this.materialService = materialService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ExperimentReportRequest create(Long experimentId, String requestedBy) {
        Experiment experiment = experimentRepository.findById(experimentId)
                .orElseThrow(() -> new IllegalArgumentException("Experimento não encontrado: " + experimentId));
        if (repository.existsByExperimentIdAndStatusIn(experimentId, ACTIVE_STATUSES)) {
            throw new IllegalStateException("Já existe uma solicitação de relatório em andamento para este experimento.");
        }
        ExperimentReportRequest request = ExperimentReportRequest.builder()
                .experiment(experiment)
                .status(ExperimentReportStatus.PENDING)
                .requestedAt(Instant.now())
                .requestedBy(normalize(requestedBy))
                .payloadSnapshot(serializePayload(experimentId))
                .build();
        return repository.save(request);
    }

    @Transactional(readOnly = true)
    public List<ExperimentReportRequest> listLatestByExperiment(Long experimentId) {
        return repository.findTop5ByExperimentIdOrderByRequestedAtDesc(experimentId);
    }

    @Transactional(readOnly = true)
    public List<ExperimentReportRequest> listByExperiment(Long experimentId) {
        return repository.findByExperimentIdOrderByRequestedAtDesc(experimentId);
    }

    @Transactional(readOnly = true)
    public List<ExperimentReportRequest> listByStatus(Collection<ExperimentReportStatus> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return repository.findAllByOrderByRequestedAtDesc();
        }
        return repository.findByStatusInOrderByRequestedAtAsc(statuses);
    }

    @Transactional(readOnly = true)
    public ExperimentReportRequest get(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Solicitação não encontrada: " + id));
    }

    @Transactional
    public ExperimentReportRequest updateStatus(Long requestId, UpdateExperimentReportRequest input) {
        ExperimentReportRequest request = get(requestId);
        ExperimentReportStatus newStatus = input.status();
        if (newStatus == null) {
            throw new IllegalArgumentException("Status é obrigatório");
        }
        switch (newStatus) {
            case PENDING -> applyRequeue(request);
            case PROCESSING -> applyProcessing(request);
            case READY -> applyReady(request, input.downloadUrl());
            case FAILED -> applyFailed(request, input.downloadUrl(), input.failureReason());
            default -> throw new IllegalStateException("Status não suportado: " + newStatus);
        }
        return repository.save(request);
    }

    private void applyRequeue(ExperimentReportRequest request) {
        request.setStatus(ExperimentReportStatus.PENDING);
        request.setRequestedAt(Instant.now());
        request.setCompletedAt(null);
        request.setDownloadUrl(null);
        request.setFailureReason(null);
        request.setPayloadSnapshot(serializePayload(request.getExperiment().getId()));
    }

    private void applyProcessing(ExperimentReportRequest request) {
        request.setStatus(ExperimentReportStatus.PROCESSING);
        request.setCompletedAt(null);
        request.setFailureReason(null);
    }

    private void applyReady(ExperimentReportRequest request, String downloadUrl) {
        String sanitizedUrl = normalize(downloadUrl);
        if (sanitizedUrl == null) {
            throw new IllegalArgumentException("O link de download é obrigatório quando o relatório está pronto.");
        }
        request.setStatus(ExperimentReportStatus.READY);
        request.setDownloadUrl(sanitizedUrl);
        request.setCompletedAt(Instant.now());
        request.setFailureReason(null);
    }

    private void applyFailed(ExperimentReportRequest request, String downloadUrl, String failureReason) {
        if (failureReason == null || failureReason.isBlank()) {
            throw new IllegalArgumentException("Descreva o motivo da falha para acompanhamento.");
        }
        request.setStatus(ExperimentReportStatus.FAILED);
        request.setFailureReason(failureReason.trim());
        request.setCompletedAt(Instant.now());
        request.setDownloadUrl(normalize(downloadUrl));
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String serializePayload(Long experimentId) {
        try {
            var material = materialService.build(experimentId);
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(material);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Não foi possível salvar o snapshot de dados do relatório", ex);
        }
    }
}
