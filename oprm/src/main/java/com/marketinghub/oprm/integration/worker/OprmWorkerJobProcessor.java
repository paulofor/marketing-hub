package com.marketinghub.oprm.integration.worker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.oprm.application.OprmArtifactPipelineService;
import com.marketinghub.oprm.domain.ArtifactEnvelope;
import com.marketinghub.oprm.integration.client.BackendArtifactPublishClient;
import com.marketinghub.oprm.integration.client.BackendJobClient;
import com.marketinghub.oprm.integration.client.BackendStatusClient;
import com.marketinghub.oprm.integration.contract.OprmArtifactEnvelopeDto;
import com.marketinghub.oprm.integration.contract.OprmArtifactPublishRequest;
import com.marketinghub.oprm.integration.contract.OprmArtifactStatus;
import com.marketinghub.oprm.integration.contract.OprmContractVersion;
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
    private final OprmArtifactPipelineService artifactPipelineService;
    private final ObjectMapper objectMapper;
    private final String workerId;
    private final String workerVersion;
    private final int claimLeaseSeconds;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public OprmWorkerJobProcessor(BackendJobClient backendJobClient,
                                  BackendStatusClient backendStatusClient,
                                  BackendArtifactPublishClient backendArtifactPublishClient,
                                  OprmArtifactPipelineService artifactPipelineService,
                                  ObjectMapper objectMapper,
                                  @Value("${oprm.worker.id:oprm-worker-local}") String workerId,
                                  @Value("${oprm.worker.version:0.1.0}") String workerVersion,
                                  @Value("${oprm.worker.claim-lease-seconds:120}") int claimLeaseSeconds) {
        this.backendJobClient = backendJobClient;
        this.backendStatusClient = backendStatusClient;
        this.backendArtifactPublishClient = backendArtifactPublishClient;
        this.artifactPipelineService = artifactPipelineService;
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

            if (detail.jobType() != OprmJobType.OCCUPATION_MAPPING) {
                throw new IllegalStateException("unsupported OPRM job type for Sprint 2: " + detail.jobType());
            }

            List<ArtifactEnvelope> artifacts = artifactPipelineService.runPipeline(
                    detail.occupationSeedRef(),
                    "default",
                    "pt-BR",
                    detail.correlationId()
            );

            artifacts.forEach(artifact -> publishArtifact(claimedJob.jobId(), detail.correlationId(), artifact));

            updateStatus(claimedJob.jobId(), OprmJobStatus.SUCCEEDED, "phase-run",
                    "OPRM job completed", null, null, Map.of("artifactsPublished", artifacts.size()));
            log.info("oprm-job-processed jobId={} correlationId={} artifactsPublished={}",
                    claimedJob.jobId(),
                    detail.correlationId(),
                    artifacts.size());
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
