package com.marketinghub.mds.service;

import com.marketinghub.mds.client.BackendMdsClient;
import com.marketinghub.mds.config.MdsProperties;
import com.marketinghub.mds.dto.*;
import com.marketinghub.mds.search.NonRecoverablePipelineException;
import com.marketinghub.mds.search.RecoverableSourceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class MdsLoopRunner {
    private static final Logger log = LoggerFactory.getLogger(MdsLoopRunner.class);

    private final BackendMdsClient backendMdsClient;
    private final MechanismDiscoveryPipelineService pipelineService;
    private final MdsProperties properties;

    public MdsLoopRunner(BackendMdsClient backendMdsClient,
                         MechanismDiscoveryPipelineService pipelineService,
                         MdsProperties properties) {
        this.backendMdsClient = backendMdsClient;
        this.pipelineService = pipelineService;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${mds.loop-interval-ms:15000}")
    public void poll() {
        if (!properties.isLoopEnabled()) {
            return;
        }

        List<BackendMdsRequestDto> pending = backendMdsClient.getPendingRequests();
        pending.stream().limit(properties.getPollLimit()).forEach(this::processOne);
    }

    private void processOne(BackendMdsRequestDto request) {
        try {
            backendMdsClient.claim(request.id(), new BackendClaimRequestDto(properties.getBackend().getWorkerId()));
            backendMdsClient.heartbeat(request.id(), new BackendHeartbeatRequestDto(
                    "pipeline",
                    "processing started",
                    Map.of("requestId", request.id())
            ));

            pipelineService.execute(request);
            backendMdsClient.complete(request.id(), new BackendCompleteRequestDto("mds pipeline finished"));
            log.info("mds-request-complete requestId={} correlationId={}", request.id(), request.correlationId());
        } catch (Exception ex) {
            String failureType = resolveFailureType(ex);
            log.error("mds-request-failed requestId={} failureType={} error={}", request.id(), failureType, ex.getMessage(), ex);
            backendMdsClient.fail(request.id(), new BackendFailRequestDto(
                    ex.getMessage(),
                    failureType,
                    failureType.equals("recoverable_external") ? "processing failed with recoverable source error" : "processing failed"
            ));
        }
    }

    private String resolveFailureType(Exception ex) {
        if (ex instanceof RecoverableSourceException) {
            return "recoverable_external";
        }
        if (ex instanceof NonRecoverablePipelineException) {
            return "non_recoverable_pipeline";
        }
        return "pipeline";
    }
}
