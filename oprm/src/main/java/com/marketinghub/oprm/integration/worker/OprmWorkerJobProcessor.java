package com.marketinghub.oprm.integration.worker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.oprm.application.FeedbackLoopService;
import com.marketinghub.oprm.application.OprmArtifactPipelineService;
import com.marketinghub.oprm.domain.ArtifactEnvelope;
import com.marketinghub.oprm.domain.HypothesisPerformanceSnapshot;
import com.marketinghub.oprm.domain.OccupationFeedbackHistoryEntry;
import com.marketinghub.oprm.domain.OccupationFeedbackLoopPayload;
import com.marketinghub.oprm.integration.client.BackendArtifactPublishClient;
import com.marketinghub.oprm.integration.client.BackendFeedbackClient;
import com.marketinghub.oprm.integration.client.BackendJobClient;
import com.marketinghub.oprm.integration.client.BackendStatusClient;
import com.marketinghub.oprm.integration.contract.OprmArtifactEnvelopeDto;
import com.marketinghub.oprm.integration.contract.OprmArtifactPublishRequest;
import com.marketinghub.oprm.integration.contract.OprmArtifactStatus;
import com.marketinghub.oprm.integration.contract.OprmContractVersion;
import com.marketinghub.oprm.integration.contract.OprmFeedbackHistoryEntryResponse;
import com.marketinghub.oprm.integration.contract.OprmFeedbackPublishRequest;
import com.marketinghub.oprm.integration.contract.OprmJobClaimRequest;
import com.marketinghub.oprm.integration.contract.OprmJobClaimResponse;
import com.marketinghub.oprm.integration.contract.OprmJobDetailResponse;
import com.marketinghub.oprm.integration.contract.OprmJobStatus;
import com.marketinghub.oprm.integration.contract.OprmJobStatusUpdateRequest;
import com.marketinghub.oprm.integration.contract.OprmJobType;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OprmWorkerJobProcessor {
    private static final Logger log = LoggerFactory.getLogger(OprmWorkerJobProcessor.class);

    private final BackendJobClient backendJobClient;
    private final BackendStatusClient backendStatusClient;
    private final BackendArtifactPublishClient backendArtifactPublishClient;
    private final BackendFeedbackClient backendFeedbackClient;
    private final OprmArtifactPipelineService artifactPipelineService;
    private final FeedbackLoopService feedbackLoopService;
    private final ObjectMapper objectMapper;
    private final String workerId;
    private final String workerVersion;
    private final int claimLeaseSeconds;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public OprmWorkerJobProcessor(BackendJobClient backendJobClient,
                                  BackendStatusClient backendStatusClient,
                                  BackendArtifactPublishClient backendArtifactPublishClient,
                                  BackendFeedbackClient backendFeedbackClient,
                                  OprmArtifactPipelineService artifactPipelineService,
                                  FeedbackLoopService feedbackLoopService,
                                  ObjectMapper objectMapper,
                                  @Value("${oprm.worker.id:oprm-worker-local}") String workerId,
                                  @Value("${oprm.worker.version:0.1.0}") String workerVersion,
                                  @Value("${oprm.worker.claim-lease-seconds:120}") int claimLeaseSeconds) {
        this.backendJobClient = backendJobClient;
        this.backendStatusClient = backendStatusClient;
        this.backendArtifactPublishClient = backendArtifactPublishClient;
        this.backendFeedbackClient = backendFeedbackClient;
        this.artifactPipelineService = artifactPipelineService;
        this.feedbackLoopService = feedbackLoopService;
        this.objectMapper = objectMapper;
        this.workerId = workerId;
        this.workerVersion = workerVersion;
        this.claimLeaseSeconds = claimLeaseSeconds;
    }

    @Scheduled(fixedDelayString = "${oprm.worker.loop-delay-ms:15000}")
    public void pollAndProcess() {
        if (!running.compareAndSet(false, true)) {
            return;
        }

        String activeJobId = null;
        try {
            OprmJobClaimRequest claimRequest = new OprmJobClaimRequest(
                    workerId,
                    workerVersion,
                    OprmContractVersion.V1,
                    1,
                    claimLeaseSeconds
            );

            Optional<OprmJobClaimResponse> claimed = backendJobClient.claimNextJob(claimRequest);
            if (claimed.isEmpty()) {
                return;
            }

            OprmJobClaimResponse claimedJob = claimed.get();
            activeJobId = claimedJob.jobId();

            OprmJobDetailResponse detail = backendJobClient.getJobDetail(claimedJob.jobId());
            updateStatus(claimedJob.jobId(), OprmJobStatus.RUNNING, "phase-run", "OPRM job started", null, null, Map.of());

            if (detail.jobType() == OprmJobType.OCCUPATION_MAPPING) {
                processOccupationMapping(claimedJob.jobId(), detail);
            } else if (detail.jobType() == OprmJobType.FEEDBACK_RECALIBRATION) {
                processFeedbackRecalibration(claimedJob.jobId(), detail);
            } else {
                throw new IllegalStateException("unsupported OPRM job type: " + detail.jobType());
            }

            updateStatus(claimedJob.jobId(), OprmJobStatus.SUCCEEDED, "phase-run",
                    "OPRM job completed", null, null, Map.of());
        } catch (Exception ex) {
            if (activeJobId != null) {
                updateStatus(activeJobId, OprmJobStatus.FAILED, "phase-run",
                        "OPRM job failed", "JOB_EXECUTION_ERROR", ex.getMessage(), Map.of());
            }
            log.error("oprm-worker-loop-failure", ex);
        } finally {
            running.set(false);
        }
    }

    private void processOccupationMapping(String jobId, OprmJobDetailResponse detail) {
        List<ArtifactEnvelope> artifacts = artifactPipelineService.runPipeline(
                detail.occupationSeedRef(),
                "default",
                "pt-BR",
                detail.correlationId()
        );

        artifacts.forEach(artifact -> publishArtifact(jobId, detail.correlationId(), artifact));

        updateStatus(jobId, OprmJobStatus.RUNNING, "phase-artifacts",
                "OPRM occupation mapping artifacts published", null, null,
                Map.of("artifactsPublished", artifacts.size()));

        log.info("oprm-job-processed jobId={} correlationId={} artifactsPublished={} jobType={}",
                jobId,
                detail.correlationId(),
                artifacts.size(),
                detail.jobType());
    }

    private void processFeedbackRecalibration(String jobId, OprmJobDetailResponse detail) {
        String occupationName = detail.occupationSeedRef();
        String personaLabel = detail.occupationSeedRef();
        List<OprmFeedbackHistoryEntryResponse> persistedHistory =
                backendFeedbackClient.loadHistory(occupationName, personaLabel);

        List<OccupationFeedbackHistoryEntry> domainHistory = persistedHistory.stream()
                .map(entry -> new OccupationFeedbackHistoryEntry(
                        Instant.parse(entry.generatedAt()),
                        entry.previousRoutineConfidence(),
                        entry.recalibratedRoutineConfidence(),
                        entry.previousFrameworkConfidence(),
                        entry.recalibratedFrameworkConfidence(),
                        entry.averageHypothesisImpact(),
                        entry.notes()
                ))
                .toList();

        List<HypothesisPerformanceSnapshot> downstreamSnapshots = List.of();

        ArtifactEnvelope feedbackArtifact = feedbackLoopService.recalibrateWithFeedback(
                occupationName,
                "default",
                "pt-BR",
                detail.correlationId(),
                downstreamSnapshots,
                domainHistory
        );

        OccupationFeedbackLoopPayload payload = (OccupationFeedbackLoopPayload) feedbackArtifact.payload();
        OprmFeedbackPublishRequest feedbackRequest = new OprmFeedbackPublishRequest(
                jobId,
                detail.correlationId(),
                payload.occupationName(),
                payload.personaLabel(),
                payload.baselineRoutineArtifactId(),
                payload.baselineFrameworkArtifactId(),
                Map.of("items", objectMapper.convertValue(payload.recalibratedPainSignals(), List.class)),
                Map.of("items", objectMapper.convertValue(payload.recalibratedMechanismSignals(), List.class)),
                Map.of("items", objectMapper.convertValue(payload.hypothesisComparison(), List.class)),
                objectMapper.convertValue(payload.scoreReweighting(), Map.class),
                payload.generatedAt().toString()
        );

        backendFeedbackClient.publish(feedbackRequest);

        publishArtifact(jobId, detail.correlationId(), feedbackArtifact);

        updateStatus(jobId, OprmJobStatus.RUNNING, "phase-feedback",
                "OPRM feedback loop persisted in backend", null, null,
                Map.of("historyLoaded", domainHistory.size(), "feedbackPublished", 1));

        log.info("oprm-job-processed jobId={} correlationId={} feedbackHistoryLoaded={} jobType={}",
                jobId,
                detail.correlationId(),
                domainHistory.size(),
                detail.jobType());
    }

    private void publishArtifact(String jobId, String correlationId, ArtifactEnvelope artifact) {
        OprmArtifactEnvelopeDto envelopeDto = new OprmArtifactEnvelopeDto(
                artifact.artifactType(),
                artifact.artifactVersion(),
                artifact.artifactId(),
                artifact.moduleName(),
                artifact.producer(),
                artifact.createdAt().toString(),
                artifact.correlationId(),
                artifact.traceId(),
                artifact.sourceRefs(),
                artifact.inputRefs(),
                objectMapper.convertValue(artifact.payload(), Map.class),
                OprmArtifactStatus.valueOf(artifact.status()),
                artifact.confidenceScore(),
                artifact.metadata()
        );

        OprmArtifactPublishRequest publishRequest = new OprmArtifactPublishRequest(
                jobId,
                correlationId,
                envelopeDto,
                Map.of(
                        "sourceRefs", artifact.sourceRefs(),
                        "inputRefs", artifact.inputRefs(),
                        "producer", artifact.producer()
                ),
                jobId + ":" + artifact.artifactId()
        );

        backendArtifactPublishClient.publish(publishRequest);
    }

    private void updateStatus(String jobId,
                              OprmJobStatus status,
                              String phase,
                              String message,
                              String errorCode,
                              String errorMessage,
                              Map<String, Object> metrics) {
        try {
            backendStatusClient.updateStatus(jobId, new OprmJobStatusUpdateRequest(
                    workerId,
                    status,
                    Instant.now().toString(),
                    phase,
                    message,
                    errorCode,
                    errorMessage,
                    metrics
            ));
        } catch (Exception statusException) {
            log.warn("oprm-status-update-failed jobId={} status={}", jobId, status, statusException);
        }
    }
}
