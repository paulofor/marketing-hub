package com.marketinghub.worker.leadportal.image;

import com.marketinghub.worker.creative.CreativeImageOptimizer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class LeadPortalImageProcessingService {

    private static final Logger log = LoggerFactory.getLogger(LeadPortalImageProcessingService.class);
    private static final String DEFAULT_TREATMENT =
            "Produzir 20 imagens para post de Instagram usando a original como base";

    private final LeadPortalImagePackageClient packageClient;
    private final LeadPortalStorageClient storageClient;
    private final LeadPortalOpenAiImageClient imageClient;

    public LeadPortalImageProcessingService(
            LeadPortalImagePackageClient packageClient,
            LeadPortalStorageClient storageClient,
            LeadPortalOpenAiImageClient imageClient) {
        this.packageClient = packageClient;
        this.storageClient = storageClient;
        this.imageClient = imageClient;
    }

    public List<LeadPortalImagePackageClient.ImagePackage> process() {
        if (!imageClient.isEnabled()) {
            log.warn("OpenAI API key not configured; skipping lead-portal image processing");
            return List.of();
        }
        List<LeadPortalImagePackageClient.ImagePackage> packages = packageClient.listRecentPackages();
        for (LeadPortalImagePackageClient.ImagePackage imagePackage : packages) {
            try {
                packageClient.markProcessing(imagePackage.id());
                handlePackage(imagePackage);
            } catch (Exception ex) {
                log.error("Failed to process lead-portal image package {}", imagePackage.id(), ex);
                packageClient.markFailed(imagePackage.id(), ex.getMessage());
            }
        }
        return packages;
    }

    private void handlePackage(LeadPortalImagePackageClient.ImagePackage imagePackage) {
        byte[] originalBytes = storageClient.download(imagePackage.storedFileName());
        int imagesToGenerate = resolveImagesToGenerate(imagePackage);
        String prompt = buildPrompt(imagePackage);

        List<LeadPortalImagePackageClient.GeneratedImage> generated = new ArrayList<>();
        for (int index = 0; index < imagesToGenerate; index++) {
            CreativeImageOptimizer.OptimizedImage optimized = imageClient.generateFromBase(originalBytes, prompt);
            String filename = buildFilename(imagePackage.submissionId(), index, optimized.extension());
            LeadPortalStorageClient.StoredImage stored = storageClient.upload(
                    optimized.content(),
                    filename,
                    MediaType.parseMediaType("image/" + optimized.extension()));
            generated.add(new LeadPortalImagePackageClient.GeneratedImage(
                    stored.objectKey(), stored.publicUrl(), imageClient.getModel(), prompt, "openai"));
        }

        packageClient.submitResults(imagePackage.id(), generated, imageClient.getModel(), prompt);
    }

    private String buildPrompt(LeadPortalImagePackageClient.ImagePackage imagePackage) {
        String basePrompt = StringUtils.hasText(imagePackage.prompt()) ? imagePackage.prompt() : "";
        String treatment = StringUtils.hasText(imagePackage.treatment()) ? imagePackage.treatment() : DEFAULT_TREATMENT;
        StringBuilder builder = new StringBuilder();
        if (StringUtils.hasText(basePrompt)) {
            builder.append(basePrompt.trim());
            if (builder.charAt(builder.length() - 1) != '.') {
                builder.append('.');
            }
            builder.append(' ');
        }
        builder.append(treatment);
        builder.append(". Preserve logo, paleta de cores e proporção da imagem base.");
        return builder.toString();
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
}
