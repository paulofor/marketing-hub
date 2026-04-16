package com.marketinghub.oprm.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.oprm.OprmFeedbackHistory;
import com.marketinghub.oprm.OprmFeedbackSnapshot;
import com.marketinghub.oprm.OprmJob;
import com.marketinghub.oprm.dto.OprmFeedbackHistoryEntryDto;
import com.marketinghub.oprm.dto.OprmFeedbackPublishRequestDto;
import com.marketinghub.oprm.repository.OprmFeedbackHistoryRepository;
import com.marketinghub.oprm.repository.OprmFeedbackSnapshotRepository;
import com.marketinghub.oprm.repository.OprmJobRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Slf4j
public class OprmFeedbackService {
    private final OprmJobRepository jobRepository;
    private final OprmFeedbackSnapshotRepository feedbackSnapshotRepository;
    private final OprmFeedbackHistoryRepository feedbackHistoryRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void publishFeedback(OprmFeedbackPublishRequestDto request) {
        UUID jobId;
        try {
            jobId = UUID.fromString(request.jobId());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "jobId must be a valid UUID");
        }

        OprmJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "OPRM job not found"));

        Instant generatedAt = parseGeneratedAt(request.generatedAt());
        OprmFeedbackSnapshot snapshot = OprmFeedbackSnapshot.builder()
                .job(job)
                .correlationId(request.correlationId())
                .occupationName(request.occupationName())
                .personaLabel(request.personaLabel())
                .baselineRoutineArtifactId(request.baselineRoutineArtifactId())
                .baselineFrameworkArtifactId(request.baselineFrameworkArtifactId())
                .recalibratedPainSignalsJson(toJson(request.recalibratedPainSignals()))
                .recalibratedMechanismSignalsJson(toJson(request.recalibratedMechanismSignals()))
                .hypothesisComparisonJson(toJson(request.hypothesisComparison()))
                .scoreReweightingJson(toJson(request.scoreReweighting()))
                .generatedAt(generatedAt)
                .build();
        feedbackSnapshotRepository.save(snapshot);

        OprmFeedbackHistory history = OprmFeedbackHistory.builder()
                .occupationName(request.occupationName())
                .personaLabel(request.personaLabel())
                .correlationId(request.correlationId())
                .generatedAt(generatedAt)
                .previousRoutineConfidence(decimalFromScore(request.scoreReweighting(), "previous_routine_confidence"))
                .recalibratedRoutineConfidence(decimalFromScore(request.scoreReweighting(), "recalibrated_routine_confidence"))
                .previousFrameworkConfidence(decimalFromScore(request.scoreReweighting(), "previous_framework_confidence"))
                .recalibratedFrameworkConfidence(decimalFromScore(request.scoreReweighting(), "recalibrated_framework_confidence"))
                .averageHypothesisImpact(decimalFromScore(request.scoreReweighting(), "average_hypothesis_impact"))
                .notes("feedback snapshot persisted from OPRM worker")
                .build();
        feedbackHistoryRepository.save(history);
        log.info("oprm-feedback-published jobId={} correlationId={} occupationName={} personaLabel={} generatedAt={}",
                job.getId(), request.correlationId(), request.occupationName(), request.personaLabel(), generatedAt);
    }

    @Transactional(readOnly = true)
    public List<OprmFeedbackHistoryEntryDto> listHistory(String occupationName, String personaLabel) {
        return feedbackHistoryRepository
                .findByOccupationNameIgnoreCaseAndPersonaLabelIgnoreCaseOrderByGeneratedAtAsc(occupationName, personaLabel)
                .stream()
                .map(entry -> new OprmFeedbackHistoryEntryDto(
                        entry.getGeneratedAt().toString(),
                        toDouble(entry.getPreviousRoutineConfidence()),
                        toDouble(entry.getRecalibratedRoutineConfidence()),
                        toDouble(entry.getPreviousFrameworkConfidence()),
                        toDouble(entry.getRecalibratedFrameworkConfidence()),
                        toDouble(entry.getAverageHypothesisImpact()),
                        entry.getNotes()
                ))
                .toList();
    }

    private Instant parseGeneratedAt(String generatedAt) {
        try {
            return Instant.parse(generatedAt);
        } catch (DateTimeParseException ex) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "generatedAt must use ISO-8601 format");
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "unable to serialize OPRM feedback payload");
        }
    }

    private BigDecimal decimalFromScore(java.util.Map<String, Object> scoreReweighting, String key) {
        Object value = scoreReweighting.get(key);
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        return null;
    }

    private double toDouble(BigDecimal value) {
        return value == null ? 0.0 : value.doubleValue();
    }
}
