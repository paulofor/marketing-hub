package com.marketinghub.experiment.learning.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.experiment.report.dto.ExperimentReportMaterialDto;
import com.marketinghub.experiment.report.service.ExperimentReportMaterialService;
import com.marketinghub.experiment.repository.ExperimentRepository;
import com.marketinghub.experiment.learning.ExperimentLearningRequest;
import com.marketinghub.experiment.learning.ExperimentLearningStatus;
import com.marketinghub.experiment.learning.dto.CreateExperimentLearningRequest;
import com.marketinghub.experiment.learning.dto.ExperimentLearningPayloadDto;
import com.marketinghub.experiment.learning.repository.ExperimentLearningRequestRepository;
import com.marketinghub.experiment.learning.ExperimentLearning;
import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Orquestra a criação e atualização das solicitações de aprendizado automatizado.
 */
@Service
public class ExperimentLearningRequestService {

    private static final Set<ExperimentLearningStatus> ACTIVE_STATUSES = EnumSet.of(
            ExperimentLearningStatus.PENDING,
            ExperimentLearningStatus.PROCESSING
    );

    private final ExperimentLearningRequestRepository repository;
    private final ExperimentRepository experimentRepository;
    private final ExperimentReportMaterialService materialService;
    private final ExperimentLearningService learningService;
    private final ExperimentLearningJsonCodec codec;
    private final ObjectMapper objectMapper;

    public ExperimentLearningRequestService(ExperimentLearningRequestRepository repository,
                                            ExperimentRepository experimentRepository,
                                            ExperimentReportMaterialService materialService,
                                            ExperimentLearningService learningService,
                                            ExperimentLearningJsonCodec codec,
                                            ObjectMapper objectMapper) {
        this.repository = repository;
        this.experimentRepository = experimentRepository;
        this.materialService = materialService;
        this.learningService = learningService;
        this.codec = codec;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ExperimentLearningRequest create(Long experimentId, CreateExperimentLearningRequest payload) {
        var experiment = experimentRepository.findById(experimentId)
                .orElseThrow(() -> new EntityNotFoundException("Experimento não encontrado: " + experimentId));
        if (repository.existsByExperimentIdAndStatusIn(experimentId, ACTIVE_STATUSES)) {
            throw new IllegalStateException("Já existe uma leitura em andamento para este experimento.");
        }
        ExperimentReportMaterialDto material = materialService.build(experimentId);
        ExperimentLearningRequest request = ExperimentLearningRequest.builder()
                .experiment(experiment)
                .status(ExperimentLearningStatus.PENDING)
                .requestedAt(Instant.now())
                .requestedBy(normalize(payload != null ? payload.requestedBy() : null))
                .payloadSnapshot(serialize(material))
                .build();
        return repository.save(request);
    }

    @Transactional(readOnly = true)
    public List<ExperimentLearningRequest> listByExperiment(Long experimentId) {
        return repository.findTop5ByExperimentIdOrderByRequestedAtDesc(experimentId);
    }

    @Transactional(readOnly = true)
    public List<ExperimentLearningRequest> listByStatus(Collection<ExperimentLearningStatus> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return repository.findAllByOrderByRequestedAtDesc();
        }
        return repository.findByStatusInOrderByRequestedAtAsc(statuses);
    }

    @Transactional
    public ExperimentLearningRequest updateStatus(Long requestId, ExperimentLearningStatus status, ExperimentLearningPayloadDto payload, String failureReason) {
        ExperimentLearningRequest request = repository.findById(requestId)
                .orElseThrow(() -> new EntityNotFoundException("Solicitação não encontrada: " + requestId));
        switch (status) {
            case PENDING -> applyRequeue(request);
            case PROCESSING -> applyProcessing(request);
            case READY -> applyReady(request, payload);
            case FAILED -> applyFailed(request, failureReason);
            default -> throw new IllegalStateException("Status não suportado: " + status);
        }
        return repository.save(request);
    }

    private void applyRequeue(ExperimentLearningRequest request) {
        request.setStatus(ExperimentLearningStatus.PENDING);
        request.setRequestedAt(Instant.now());
        request.setCompletedAt(null);
        request.setFailureReason(null);
        request.setResultPayload(null);
    }

    private void applyProcessing(ExperimentLearningRequest request) {
        request.setStatus(ExperimentLearningStatus.PROCESSING);
        request.setCompletedAt(null);
        request.setFailureReason(null);
    }

    private void applyReady(ExperimentLearningRequest request, ExperimentLearningPayloadDto payload) {
        if (payload == null) {
            throw new IllegalArgumentException("Payload do aprendizado é obrigatório quando o status é READY");
        }
        ExperimentLearning learning = learningService.registerResult(request, payload);
        request.setStatus(ExperimentLearningStatus.READY);
        request.setCompletedAt(learning.getCompletedAt());
        request.setFailureReason(null);
        request.setResultPayload(codec.writePayload(payload));
    }

    private void applyFailed(ExperimentLearningRequest request, String failureReason) {
        request.setStatus(ExperimentLearningStatus.FAILED);
        request.setCompletedAt(Instant.now());
        request.setFailureReason(StringUtils.hasText(failureReason) ? failureReason : "Falha desconhecida");
    }

    private String serialize(ExperimentReportMaterialDto material) {
        try {
            return objectMapper.writeValueAsString(material);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Falha ao serializar snapshot do experimento", ex);
        }
    }

    private String normalize(String input) {
        return StringUtils.hasText(input) ? input.trim() : null;
    }
}
