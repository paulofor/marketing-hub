package com.marketinghub.worker.leadportal.image;

import com.marketinghub.worker.creative.CreativeImageOptimizer;
import com.marketinghub.worker.imagegeneration.ImageGenerationPlan;
import com.marketinghub.worker.imagegeneration.ImageGenerationPlanService;
import com.marketinghub.worker.imagegeneration.ImageOrientation;
import com.marketinghub.worker.leadportal.image.LeadPortalImagePackageClient.LeadPortalWorkerException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import software.amazon.awssdk.core.exception.SdkException;

@Service
public class LeadPortalImageProcessingService {

    private static final Logger log = LoggerFactory.getLogger(LeadPortalImageProcessingService.class);
    private static final String DEFAULT_TREATMENT =
            "Produzir imagens para post de Instagram usando a original como base";
    private static final int PROMPT_MAX_LENGTH = 3000;
    private static final int MAX_IMAGE_ATTEMPTS = 3;
    private static final Duration RETRY_DELAY_BASE = Duration.ofMillis(500);

    private final LeadPortalImagePackageClient packageClient;
    private final LeadPortalStorageClient storageClient;
    private final LeadPortalOpenAiImageClient imageClient;
    private final ImageGenerationPlanService planService;

    public LeadPortalImageProcessingService(
            LeadPortalImagePackageClient packageClient,
            LeadPortalStorageClient storageClient,
            LeadPortalOpenAiImageClient imageClient,
            ImageGenerationPlanService planService) {
        this.packageClient = packageClient;
        this.storageClient = storageClient;
        this.imageClient = imageClient;
        this.planService = planService;
    }

    public List<LeadPortalImagePackageClient.ImagePackage> process() {
        if (!imageClient.isEnabled()) {
            log.warn("OpenAI API key not configured; skipping lead-portal image processing");
            return List.of();
        }
        List<LeadPortalImagePackageClient.ImagePackage> packages = fetchRecentPackages();
        if (packages == null) {
            return List.of();
        }
        List<LeadPortalImagePackageClient.ImagePackage> promptOnly = new ArrayList<>();
        for (LeadPortalImagePackageClient.ImagePackage imagePackage : packages) {
            boolean startedProcessing = false;
            try {
                packageClient.markProcessing(imagePackage.id());
                startedProcessing = true;
                if (hasBaseImage(imagePackage)) {
                    handlePackage(imagePackage);
                } else {
                    promptOnly.add(imagePackage);
                }
            } catch (LeadPortalWorkerException ex) {
                if (!startedProcessing) {
                    HttpStatusCode status = ex.getStatus();
                    if (status != null && status.value() == 409) {
                        log.info("Skipping lead-portal image package {} because it was already claimed by another worker",
                                imagePackage.id());
                    } else {
                        log.warn("Backend refused to start processing for lead-portal image package {}: {}",
                                imagePackage.id(),
                                ex.getMessage());
                    }
                    continue;
                }
                log.error("Failed to process lead-portal image package {}", imagePackage.id(), ex);
                if (!handleTransientFailure(imagePackage.id(), ex)) {
                    packageClient.markFailed(imagePackage.id(), resolveFailureReason(ex));
                }
            } catch (Exception ex) {
                log.error("Failed to process lead-portal image package {}", imagePackage.id(), ex);
                if (startedProcessing) {
                    if (!handleTransientFailure(imagePackage.id(), ex)) {
                        packageClient.markFailed(imagePackage.id(), resolveFailureReason(ex));
                    }
                }
            }
        }
        if (!promptOnly.isEmpty()) {
            try {
                handlePromptOnlyPackagesBatch(promptOnly);
            } catch (Exception ex) {
                handleBatchFailure(promptOnly, ex);
            }
        }
        return packages;
    }

    private List<LeadPortalImagePackageClient.ImagePackage> fetchRecentPackages() {
        try {
            return packageClient.listRecentPackages();
        } catch (Exception ex) {
            String reason = resolveFailureReason(ex);
            if (isTransientError(ex)) {
                log.warn(
                        "Failed to list lead-portal image packages due to transient error: {}. Will retry later.",
                        reason);
                log.debug("Transient failure while listing lead-portal image packages", ex);
            } else {
                log.error("Failed to list lead-portal image packages: {}", reason, ex);
            }
            return null;
        }
    }

    private void handlePackage(LeadPortalImagePackageClient.ImagePackage imagePackage) {
        byte[] originalBytes = null;
        boolean hasBaseImage = StringUtils.hasText(imagePackage.storedFileName());
        if (hasBaseImage) {
            originalBytes = storageClient.download(imagePackage.storedFileName());
        } else {
            log.info("Lead-portal package {} has no base image; generating from prompt only", imagePackage.id());
        }
        int imagesToGenerate = resolveImagesToGenerate(imagePackage);
        String prompt = buildPrompt(imagePackage);

        ImageOrientation baseOrientation = planService.detectOrientation(originalBytes);
        ImageGenerationPlan plan = planService.resolvePlan(imagePackage, baseOrientation);
        ImageOrientation effectiveOrientation = plan != null && plan.orientation() != null ? plan.orientation() : baseOrientation;
        String resolvedModel = plan != null && plan.apiModel() != null ? plan.apiModel() : imageClient.getModel();

        List<LeadPortalImagePackageClient.GeneratedImage> generated = new ArrayList<>();
        for (int index = 0; index < imagesToGenerate; index++) {
            CreativeImageOptimizer.OptimizedImage optimized =
                    generateWithRetry(originalBytes, prompt, plan, imagePackage.id(), index, hasBaseImage);
            String filename = buildFilename(imagePackage.submissionId(), index, optimized.extension());
            LeadPortalStorageClient.StoredImage stored = storageClient.upload(
                    optimized.content(),
                    filename,
                    MediaType.parseMediaType("image/" + optimized.extension()));
            generated.add(new LeadPortalImagePackageClient.GeneratedImage(
                    stored.objectKey(),
                    stored.publicUrl(),
                    resolvedModel,
                    prompt,
                    "openai",
                    plan != null ? plan.width() : null,
                    plan != null ? plan.height() : null,
                    effectiveOrientation != null ? effectiveOrientation.name() : null));
        }

        packageClient.submitResults(imagePackage.id(), generated, resolvedModel, prompt);
    }

    private void handlePromptOnlyPackagesBatch(List<LeadPortalImagePackageClient.ImagePackage> packages) {
        Map<Long, PackageBatchContext> packageContexts = new LinkedHashMap<>();
        Map<String, BatchJobContext> jobContexts = new HashMap<>();
        List<LeadPortalOpenAiImageClient.BatchPromptRequest> batchRequests = new ArrayList<>();

        for (LeadPortalImagePackageClient.ImagePackage imagePackage : packages) {
            int imagesToGenerate = Math.max(1, resolveImagesToGenerate(imagePackage));
            ImageOrientation baseOrientation = planService.detectOrientation(null);
            ImageGenerationPlan plan = planService.resolvePlan(imagePackage, baseOrientation);
            ImageOrientation effectiveOrientation = plan != null && plan.orientation() != null ? plan.orientation() : baseOrientation;
            String prompt = buildPrompt(imagePackage);
            String resolvedModel = plan != null && plan.apiModel() != null ? plan.apiModel() : imageClient.getModel();

            PackageBatchContext context = new PackageBatchContext(
                    imagePackage, prompt, plan, effectiveOrientation, resolvedModel, imagesToGenerate);
            packageContexts.put(imagePackage.id(), context);

            for (int index = 0; index < imagesToGenerate; index++) {
                String customId = buildBatchCustomId(imagePackage.id(), index);
                batchRequests.add(new LeadPortalOpenAiImageClient.BatchPromptRequest(customId, prompt, plan));
                jobContexts.put(customId, new BatchJobContext(context, index));
            }
        }

        if (batchRequests.isEmpty()) {
            for (LeadPortalImagePackageClient.ImagePackage imagePackage : packages) {
                packageClient.markFailed(imagePackage.id(), "Nenhuma solicitação válida para batch");
            }
            return;
        }

        Map<String, LeadPortalOpenAiImageClient.BatchGenerationResult> results =
                imageClient.generatePromptBatch(batchRequests);

        Set<String> processed = new HashSet<>();
        for (Map.Entry<String, LeadPortalOpenAiImageClient.BatchGenerationResult> entry : results.entrySet()) {
            BatchJobContext jobContext = jobContexts.get(entry.getKey());
            if (jobContext == null) {
                continue;
            }
            processed.add(entry.getKey());
            PackageBatchContext context = jobContext.packageContext();
            LeadPortalOpenAiImageClient.BatchGenerationResult generationResult = entry.getValue();
            if (generationResult == null || !generationResult.isSuccessful()) {
                context.fail(determineBatchFailureReason(generationResult));
                continue;
            }
            try {
                CreativeImageOptimizer.OptimizedImage optimized = generationResult.image();
                String filename = buildFilename(context.imagePackage().submissionId(), jobContext.imageIndex(), optimized.extension());
                LeadPortalStorageClient.StoredImage stored = storageClient.upload(
                        optimized.content(),
                        filename,
                        MediaType.parseMediaType("image/" + optimized.extension()));
                context.addGenerated(new LeadPortalImagePackageClient.GeneratedImage(
                        stored.objectKey(),
                        stored.publicUrl(),
                        context.resolvedModel(),
                        context.prompt(),
                        "openai",
                        context.width(),
                        context.height(),
                        context.orientationName()));
            } catch (Exception ex) {
                context.fail(resolveFailureReason(ex));
            }
        }

        for (Map.Entry<String, BatchJobContext> entry : jobContexts.entrySet()) {
            if (!processed.contains(entry.getKey())) {
                entry.getValue().packageContext().fail(
                        "OpenAI batch não retornou o item " + entry.getKey());
            }
        }

        for (PackageBatchContext context : packageContexts.values()) {
            boolean completed = !context.failed() && context.generated().size() >= context.expectedImages();
            if (completed) {
                packageClient.submitResults(
                        context.imagePackage().id(), context.generated(), context.resolvedModel(), context.prompt());
                continue;
            }

            String reason = context.failureReason();
            if (!StringUtils.hasText(reason)) {
                reason = "OpenAI batch retornou apenas %d de %d imagens".formatted(
                        context.generated().size(), context.expectedImages());
            }
            requestPromptBatchRetry(context.imagePackage().id(), reason);
        }
    }

    private void handleBatchFailure(List<LeadPortalImagePackageClient.ImagePackage> packages, Throwable throwable) {
        String reason = resolveFailureReason(throwable);
        log.warn(
                "Lead-portal prompt-only batch falhou com motivo '{}'; reagendando {} pacote(s) para retry",
                reason,
                packages != null ? packages.size() : 0,
                throwable);
        if (packages == null || packages.isEmpty()) {
            return;
        }
        for (LeadPortalImagePackageClient.ImagePackage imagePackage : packages) {
            requestPromptBatchRetry(imagePackage.id(), reason);
        }
    }

    private void requestPromptBatchRetry(long packageId, String reason) {
        try {
            packageClient.markRetry(packageId, reason);
            log.info(
                    "Scheduled retry for lead-portal prompt-only package {} after batch issue: {}",
                    packageId,
                    reason);
        } catch (LeadPortalWorkerException retryEx) {
            log.warn(
                    "Backend rejected retry for lead-portal prompt-only package {}: {}. Marking as failed.",
                    packageId,
                    retryEx.getMessage());
            packageClient.markFailed(packageId, reason);
        } catch (Exception retryEx) {
            log.warn(
                    "Failed to request retry for lead-portal prompt-only package {}", packageId, retryEx);
            packageClient.markFailed(packageId, reason);
        }
    }

    private boolean hasBaseImage(LeadPortalImagePackageClient.ImagePackage imagePackage) {
        return imagePackage != null && StringUtils.hasText(imagePackage.storedFileName());
    }

    private String buildBatchCustomId(long packageId, int imageIndex) {
        return "package-" + packageId + "-image-" + imageIndex;
    }

    private String determineBatchFailureReason(LeadPortalOpenAiImageClient.BatchGenerationResult result) {
        if (result == null) {
            return "OpenAI batch retornou resposta vazia";
        }
        if (StringUtils.hasText(result.errorMessage())) {
            return result.errorMessage();
        }
        return "OpenAI batch retornou resposta vazia";
    }

    private CreativeImageOptimizer.OptimizedImage generateWithRetry(
            byte[] originalBytes,
            String prompt,
            ImageGenerationPlan plan,
            long packageId,
            int imageIndex,
            boolean hasBaseImage) {
        for (int attempt = 1; attempt <= MAX_IMAGE_ATTEMPTS; attempt++) {
            try {
                if (hasBaseImage) {
                    return imageClient.generateFromBase(originalBytes, prompt, plan);
                }
                return imageClient.generateFromPrompt(prompt, plan);
            } catch (RuntimeException ex) {
                if (!isTransientError(ex) || attempt == MAX_IMAGE_ATTEMPTS) {
                    throw ex;
                }
                Duration delay = RETRY_DELAY_BASE.multipliedBy(attempt);
                String statusDetail = statusSuffix(ex);
                log.warn(
                        "Lead-portal image generation attempt {} of {} failed for package {} (image {}): {}{}. Retrying in {} ms.",
                        attempt,
                        MAX_IMAGE_ATTEMPTS,
                        packageId,
                        imageIndex,
                        ex.getMessage(),
                        statusDetail,
                        delay.toMillis());
                sleepQuietly(delay);
            }
        }
        throw new IllegalStateException("Failed to generate image after retries");
    }

    private String statusSuffix(Throwable throwable) {
        LeadPortalOpenAiImageClient.ImageGenerationException imageEx = findImageGenerationException(throwable);
        if (imageEx != null && imageEx.getStatus() != null) {
            return " (status " + imageEx.getStatus().value() + ")";
        }
        return "";
    }

    private LeadPortalOpenAiImageClient.ImageGenerationException findImageGenerationException(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof LeadPortalOpenAiImageClient.ImageGenerationException imageEx) {
                return imageEx;
            }
            current = current.getCause();
        }
        return null;
    }

    private void sleepQuietly(Duration delay) {
        try {
            Thread.sleep(delay.toMillis());
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    private boolean handleTransientFailure(long packageId, Throwable throwable) {
        if (!isTransientError(throwable)) {
            return false;
        }
        String reason = resolveFailureReason(throwable);
        try {
            packageClient.markRetry(packageId, reason);
            log.warn(
                    "Scheduled retry for lead-portal image package {} due to transient error: {}",
                    packageId,
                    reason);
            return true;
        } catch (LeadPortalWorkerException retryEx) {
            log.warn(
                    "Backend rejected retry for lead-portal image package {}: {}. Falling back to failure.",
                    packageId,
                    retryEx.getMessage());
        } catch (Exception retryEx) {
            log.warn("Failed to request retry for lead-portal image package {}", packageId, retryEx);
        }
        return false;
    }

    private boolean isTransientError(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof LeadPortalOpenAiImageClient.ImageGenerationException imageEx) {
                HttpStatusCode status = imageEx.getStatus();
                if (status != null) {
                    int value = status.value();
                    if (value == 429 || value == 408 || (value >= 500 && value < 600)) {
                        return true;
                    }
                }
            }
            if (current instanceof LeadPortalWorkerException workerEx) {
                HttpStatusCode status = workerEx.getStatus();
                if (status != null) {
                    int value = status.value();
                    if (value == 429 || value == 408 || (value >= 500 && value < 600)) {
                        return true;
                    }
                }
            }
            if (current instanceof SdkException) {
                return true;
            }
            if (current instanceof WebClientRequestException) {
                return true;
            }
            if (current instanceof SocketTimeoutException
                    || current instanceof TimeoutException
                    || current instanceof ConnectException
                    || current instanceof UnknownHostException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private String buildPrompt(LeadPortalImagePackageClient.ImagePackage imagePackage) {
        String basePrompt = normalizeBasePrompt(imagePackage.prompt());
        String treatmentPrompt = normalizeTreatment(imagePackage.treatment());
        String suffix = treatmentPrompt + " Preserve logo, paleta de cores e proporção da imagem base.";
        if (!StringUtils.hasText(basePrompt)) {
            return ensureMaxLength(suffix, imagePackage.id());
        }
        return combinePrompts(basePrompt, suffix, imagePackage.id());
    }

    private String combinePrompts(String basePrompt, String suffix, long packageId) {
        String combined = basePrompt + " " + suffix;
        if (combined.length() <= PROMPT_MAX_LENGTH) {
            return combined;
        }
        int suffixLength = suffix.length();
        if (suffixLength >= PROMPT_MAX_LENGTH) {
            return ensureMaxLength(suffix, packageId);
        }
        int availableForBase = PROMPT_MAX_LENGTH - suffixLength - 1;
        if (availableForBase <= 0) {
            log.warn(
                    "Lead-portal prompt base for package {} removed because suffix already uses {} characters",
                    packageId,
                    suffixLength);
            return ensureMaxLength(suffix, packageId);
        }
        String truncatedBase = truncateWithEllipsis(basePrompt, availableForBase);
        if (!truncatedBase.equals(basePrompt)) {
            log.warn(
                    "Lead-portal prompt base for package {} truncated from {} to {} characters to satisfy {}-character limit",
                    packageId,
                    basePrompt.length(),
                    truncatedBase.length(),
                    PROMPT_MAX_LENGTH);
        }
        String adjusted = truncatedBase + " " + suffix;
        if (adjusted.length() <= PROMPT_MAX_LENGTH) {
            return adjusted;
        }
        return ensureMaxLength(adjusted, packageId);
    }

    private String ensureMaxLength(String text, long packageId) {
        if (text.length() <= PROMPT_MAX_LENGTH) {
            return text;
        }
        String truncated = truncateWithEllipsis(text, PROMPT_MAX_LENGTH);
        log.warn(
                "Lead-portal prompt segment for package {} trimmed from {} to {} characters to satisfy {}-character limit",
                packageId,
                text.length(),
                truncated.length(),
                PROMPT_MAX_LENGTH);
        return truncated;
    }

    private String normalizeBasePrompt(String prompt) {
        if (!StringUtils.hasText(prompt)) {
            return "";
        }
        String trimmed = prompt.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        char lastChar = trimmed.charAt(trimmed.length() - 1);
        if (lastChar == '.' || lastChar == '!' || lastChar == '?') {
            return trimmed;
        }
        return trimmed + ".";
    }

    private String normalizeTreatment(String treatment) {
        String resolved = StringUtils.hasText(treatment) ? treatment.trim() : DEFAULT_TREATMENT;
        if (resolved.isEmpty()) {
            resolved = DEFAULT_TREATMENT;
        }
        char lastChar = resolved.charAt(resolved.length() - 1);
        if (lastChar == '.' || lastChar == '!' || lastChar == '?') {
            return resolved;
        }
        return resolved + ".";
    }

    private String truncateWithEllipsis(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        }
        if (maxLength <= 3) {
            return text.substring(0, maxLength);
        }
        int cut = Math.min(text.length(), maxLength - 3);
        String truncated = text.substring(0, cut);
        int lastSpace = truncated.lastIndexOf(' ');
        if (lastSpace > cut / 2) {
            truncated = truncated.substring(0, lastSpace);
        }
        truncated = truncated.replaceAll("[\\p{Punct}\\s]+$", "");
        return truncated + "...";
    }

    private int resolveImagesToGenerate(LeadPortalImagePackageClient.ImagePackage imagePackage) {
        Integer planned = imagePackage.plannedOutputs();
        if (planned != null && planned > 0) {
            return planned;
        }
        Integer freeImages = imagePackage.freeImages();
        if (freeImages != null && freeImages > 0) {
            return freeImages;
        }
        return 20;
    }

    private String buildFilename(UUID submissionId, int index, String extension) {
        String prefix = submissionId != null ? submissionId.toString() : Instant.now().toString();
        return prefix + "-generated-" + index + "." + sanitizeExtension(extension);
    }

    private String sanitizeExtension(String extension) {
        if (!StringUtils.hasText(extension)) {
            return "jpg";
        }
        String cleaned = extension.trim().toLowerCase();
        if (cleaned.chars().anyMatch(ch -> !Character.isLetterOrDigit(ch))) {
            byte[] bytes = cleaned.getBytes(StandardCharsets.UTF_8);
            return Integer.toHexString(Objects.hash(bytes));
        }
        return cleaned;
    }

    private static final class BatchJobContext {
        private final PackageBatchContext packageContext;
        private final int imageIndex;

        BatchJobContext(PackageBatchContext packageContext, int imageIndex) {
            this.packageContext = packageContext;
            this.imageIndex = imageIndex;
        }

        PackageBatchContext packageContext() {
            return packageContext;
        }

        int imageIndex() {
            return imageIndex;
        }
    }

    private static final class PackageBatchContext {
        private final LeadPortalImagePackageClient.ImagePackage imagePackage;
        private final String prompt;
        private final ImageGenerationPlan plan;
        private final Integer width;
        private final Integer height;
        private final String orientationName;
        private final String resolvedModel;
        private final int expectedImages;
        private final List<LeadPortalImagePackageClient.GeneratedImage> generated = new ArrayList<>();
        private boolean failed;
        private String failureReason;

        PackageBatchContext(
                LeadPortalImagePackageClient.ImagePackage imagePackage,
                String prompt,
                ImageGenerationPlan plan,
                ImageOrientation orientation,
                String resolvedModel,
                int expectedImages) {
            this.imagePackage = imagePackage;
            this.prompt = prompt;
            this.plan = plan;
            this.width = plan != null ? plan.width() : null;
            this.height = plan != null ? plan.height() : null;
            this.orientationName = orientation != null ? orientation.name() : null;
            this.resolvedModel = resolvedModel;
            this.expectedImages = expectedImages;
        }

        void addGenerated(LeadPortalImagePackageClient.GeneratedImage image) {
            this.generated.add(image);
        }

        void fail(String reason) {
            this.failed = true;
            if (StringUtils.hasText(reason)) {
                this.failureReason = reason;
            }
        }

        boolean failed() {
            return failed;
        }

        String failureReason() {
            return failureReason;
        }

        LeadPortalImagePackageClient.ImagePackage imagePackage() {
            return imagePackage;
        }

        String prompt() {
            return prompt;
        }

        ImageGenerationPlan plan() {
            return plan;
        }

        Integer width() {
            return width;
        }

        Integer height() {
            return height;
        }

        String orientationName() {
            return orientationName;
        }

        String resolvedModel() {
            return resolvedModel;
        }

        int expectedImages() {
            return expectedImages;
        }

        List<LeadPortalImagePackageClient.GeneratedImage> generated() {
            return generated;
        }

        void clearFailure() {
            this.failed = false;
            this.failureReason = null;
        }
    }

    private String resolveFailureReason(Throwable throwable) {
        if (throwable == null) {
            return "Erro desconhecido no worker";
        }
        String message = throwable.getMessage();
        if (!StringUtils.hasText(message)) {
            return throwable.getClass().getSimpleName();
        }
        return message.trim();
    }
}
