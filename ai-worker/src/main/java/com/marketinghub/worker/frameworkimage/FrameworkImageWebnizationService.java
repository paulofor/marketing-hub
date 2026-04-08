package com.marketinghub.worker.frameworkimage;

import com.marketinghub.worker.creative.CreativeImageOptimizer;
import java.time.Duration;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class FrameworkImageWebnizationService {
    private static final Logger log = LoggerFactory.getLogger(FrameworkImageWebnizationService.class);
    private static final Duration DEFAULT_UPLOAD_BACKOFF = Duration.ofMillis(300);

    private final FrameworkImageBackendClient backendClient;
    private final FrameworkImageStorageClient storageClient;
    private final CreativeImageOptimizer imageOptimizer;
    private final WebClient webClient;
    private final int uploadAttempts;
    private final Duration uploadBackoff;

    public FrameworkImageWebnizationService(FrameworkImageBackendClient backendClient,
                                            FrameworkImageStorageClient storageClient,
                                            CreativeImageOptimizer imageOptimizer,
                                            WebClient.Builder webClientBuilder,
                                            @Value("${framework-image.webnization.upload.max-attempts:3}") int uploadAttempts,
                                            @Value("${framework-image.webnization.upload.backoff:PT0.3S}") Duration uploadBackoff) {
        this.backendClient = backendClient;
        this.storageClient = storageClient;
        this.imageOptimizer = imageOptimizer;
        this.webClient = webClientBuilder.build();
        this.uploadAttempts = Math.max(1, uploadAttempts);
        this.uploadBackoff = normalizeDuration(uploadBackoff, DEFAULT_UPLOAD_BACKOFF);
    }

    public void processPending() {
        List<FrameworkImageWebnizationPendingAssetDto> pendingAssets = backendClient.listPendingWebnization(20);
        if (pendingAssets.isEmpty()) {
            log.debug("Framework image webnization found no pending assets");
            return;
        }

        for (FrameworkImageWebnizationPendingAssetDto asset : pendingAssets) {
            try {
                byte[] sourceBytes = webClient.get()
                        .uri(asset.sourceUrl())
                        .retrieve()
                        .bodyToMono(byte[].class)
                        .block();
                if (sourceBytes == null || sourceBytes.length == 0) {
                    log.warn("Framework image webnization skipped asset {} due to empty source payload", asset.assetId());
                    continue;
                }

                CreativeImageOptimizer.OptimizedImage optimized = imageOptimizer.optimize(sourceBytes);
                FrameworkImageStorageClient.UploadedFrameworkImage uploaded = uploadWithRetry(
                        optimized.content(),
                        asset.assetId() + "-web-ready.jpg");
                backendClient.markWebReady(asset.assetId(), uploaded.publicUrl());
                log.info("Framework image web-ready published jobId={} experimentId={} assetId={} webUrl={} objectKey={}",
                        asset.jobId(), asset.experimentId(), asset.assetId(), uploaded.publicUrl(), uploaded.objectKey());
            } catch (Exception ex) {
                log.error("Framework image webnization failed jobId={} experimentId={} assetId={} error={}",
                        asset.jobId(), asset.experimentId(), asset.assetId(), ex.getMessage(), ex);
            }
        }
    }

    private FrameworkImageStorageClient.UploadedFrameworkImage uploadWithRetry(byte[] content, String preferredName) {
        RuntimeException lastError = null;
        for (int attempt = 1; attempt <= uploadAttempts; attempt++) {
            try {
                return storageClient.upload(content, preferredName);
            } catch (RuntimeException ex) {
                lastError = ex;
                if (attempt >= uploadAttempts) {
                    break;
                }
                long backoffMillis = uploadBackoff.toMillis() * attempt;
                sleep(backoffMillis);
            }
        }
        throw lastError != null ? lastError : new IllegalStateException("Framework image web-ready upload failed");
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(Math.max(50L, millis));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting to retry framework image web-ready upload", e);
        }
    }

    private Duration normalizeDuration(Duration candidate, Duration fallback) {
        if (candidate == null || candidate.isNegative() || candidate.isZero()) {
            return fallback;
        }
        return candidate;
    }
}
