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
import java.util.List;
import java.util.Objects;
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
            "Produzir 20 imagens para post de Instagram usando a original como base";
    private static final int PROMPT_MAX_LENGTH = 1000;
    private static final int MAX_IMAGE_GENERATION_ATTEMPTS = 3;

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
        List<LeadPortalImagePackageClient.ImagePackage> packages = packageClient.listRecentPackages();
        for (LeadPortalImagePackageClient.ImagePackage imagePackage : packages) {
            boolean startedProcessing = false;
            try {
                packageClient.markProcessing(imagePackage.id());
                startedProcessing = true;
                handlePackage(imagePackage);
            } catch (LeadPortalWorkerException ex) {
                if (!startedProcessing) {
                    HttpStatusCode status = ex.getStatus();
                    if (status != null && status.value() == 409) {
                        log.info(
                                "Skipping lead-portal image package {} because it was already claimed by another worker",
                                imagePackage.id());
                    } else {
                        log.warn(
                                "Backend refused to start processing for lead-portal image package {}: {}",
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
        return packages;
    }

    private void handlePackage(LeadPortalImagePackageClient.ImagePackage imagePackage) {
        byte[] originalBytes = storageClient.download(imagePackage.storedFileName());
        int imagesToGenerate = resolveImagesToGenerate(imagePackage);
        String prompt = buildPrompt(imagePackage);

        ImageOrientation baseOrientation = planService.detectOrientation(originalBytes);
        ImageGenerationPlan plan = planService.resolvePlan(imagePackage, baseOrientation);
        ImageOrientation effectiveOrientation = plan != null && plan.orientation() != null ? plan.orientation() : baseOrientation;
        String resolvedModel = plan != null && plan.apiModel() != null ? plan.apiModel() : imageClient.getModel();

        List<LeadPortalImagePackageClient.GeneratedImage> generated = new ArrayList<>();
        for (int index = 0; index < imagesToGenerate; index++) {
            CreativeImageOptimizer.OptimizedImage optimized =
                    generateImageWithRetry(originalBytes, prompt, plan, imagePackage.id(), index);
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

    private CreativeImageOptimizer.OptimizedImage generateImageWithRetry(
            byte[] originalBytes,
            String prompt,
            ImageGenerationPlan plan,
            long packageId,
            int imageIndex) {
        RuntimeException lastFailure = null;
        for (int attempt = 1; attempt <= MAX_IMAGE_GENERATION_ATTEMPTS; attempt++) {
            try {
                return imageClient.generateFromBase(originalBytes, prompt, plan);
            } catch (RuntimeException ex) {
                lastFailure = ex;
                if (!isTransientError(ex) || attempt == MAX_IMAGE_GENERATION_ATTEMPTS) {
                    throw ex;
                }
                Duration delay = retryDelay(attempt);
                log.warn(
                        "Transient error while generating lead-portal image {} for package {} (attempt {} of {}): {}. Retrying in {} seconds.",
                        imageIndex + 1,
                        packageId,
                        attempt,
                        MAX_IMAGE_GENERATION_ATTEMPTS,
                        ex.getMessage(),
                        delay.toSeconds());
                sleep(delay);
            }
        }
        throw lastFailure;
    }

    private Duration retryDelay(int attempt) {
        long seconds = Math.min(30, (1L << (attempt - 1)) * 2L);
        return Duration.ofSeconds(seconds);
    }

    private void sleep(Duration delay) {
        try {
            Thread.sleep(delay.toMillis());
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(
                    "Worker interrompido durante a espera para nova tentativa de geração de imagem", interrupted);
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
            if (current instanceof LeadPortalOpenAiImageClient.OpenAiImageException openAiEx) {
                int status = openAiEx.getStatusCode();
                if (status == 429 || status == 408 || (status >= 500 && status < 600)) {
                    return true;
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
