package com.marketinghub.worker.frameworkimage;

public record FrameworkImageJobCompletionPayload(
        String stage,
        String model,
        String prompt,
        String batchId,
        Long assetId,
        String sourceUrl,
        String webUrl) {
}
