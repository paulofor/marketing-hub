package com.marketinghub.leadportal.service;

import com.marketinghub.leadportal.config.LegacyAssetsProperties;
import com.marketinghub.leadportal.config.StorageProperties;
import com.marketinghub.leadportal.model.Flow;
import com.marketinghub.leadportal.model.FlowQuestion;
import com.marketinghub.leadportal.model.SimpleFormStyle;
import com.marketinghub.leadportal.model.SimpleFormStyleDefinition;
import com.marketinghub.leadportal.service.ImageOptimizer.OptimizedImage;
import com.marketinghub.leadportal.service.LegacyAssetClient.DownloadedAsset;
import com.marketinghub.leadportal.storage.FileStorageService;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class FlowAssetService {

    private static final Pattern PROOF_IMAGE_KEY = Pattern.compile("^exemplo_real_card_\\d+_imagem_url$");

    private final LegacyAssetClient legacyAssetClient;
    private final FileStorageService fileStorageService;
    private final ImageOptimizer imageOptimizer;
    private final LegacyAssetsProperties legacyAssetsProperties;
    private final StorageProperties storageProperties;
    private final URI legacyBaseUri;

    private static final Logger log = LoggerFactory.getLogger(FlowAssetService.class);

    public FlowAssetService(
            LegacyAssetClient legacyAssetClient,
            FileStorageService fileStorageService,
            ImageOptimizer imageOptimizer,
            LegacyAssetsProperties legacyAssetsProperties,
            StorageProperties storageProperties) {
        this.legacyAssetClient = legacyAssetClient;
        this.fileStorageService = fileStorageService;
        this.imageOptimizer = imageOptimizer;
        this.legacyAssetsProperties = legacyAssetsProperties;
        this.storageProperties = storageProperties;
        this.legacyBaseUri = buildLegacyBaseUri(legacyAssetsProperties.normalizedBaseUrl());
    }

    public Flow optimizeAssets(Flow flow) {
        if (flow == null) {
            return null;
        }

        SimpleFormStyle optimizedStyle = optimizeStyle(flow.simpleFormStyle(), flow.slug());
        List<FlowQuestion> optimizedQuestions = optimizeProofAssets(flow.questions(), flow.slug());

        return new Flow(
                flow.slug(),
                flow.name(),
                flow.description(),
                flow.customFormHtml(),
                flow.model(),
                flow.prompt(),
                flow.imagePromptModel(),
                flow.imagePromptTemplate(),
                flow.imageBatchSize(),
                optimizedQuestions,
                optimizedStyle,
                flow.facebookPixelId(),
                flow.facebookPixelCode(),
                flow.facebookPixelCreatedAt());
    }

    private SimpleFormStyle optimizeStyle(SimpleFormStyle style, String slug) {
        if (style == null || style.definition() == null) {
            return style;
        }

        SimpleFormStyleDefinition definition = style.definition();
        String heroImageUrl = migrateAsset(definition.heroImageUrl(), slug, "hero");
        String backgroundPatternUrl = migrateAsset(definition.backgroundPatternUrl(), slug, "background-pattern");

        if (Objects.equals(heroImageUrl, definition.heroImageUrl())
                && Objects.equals(backgroundPatternUrl, definition.backgroundPatternUrl())) {
            return style;
        }

        SimpleFormStyleDefinition updatedDefinition = new SimpleFormStyleDefinition(
                definition.backgroundColor(),
                definition.backgroundGradient(),
                backgroundPatternUrl,
                definition.cardBackground(),
                definition.cardBorderColor(),
                definition.cardShadow(),
                definition.headingColor(),
                definition.textColor(),
                definition.mutedTextColor(),
                definition.primaryColor(),
                definition.accentColor(),
                definition.buttonBackground(),
                definition.buttonTextColor(),
                definition.buttonShadow(),
                definition.buttonBorderRadius(),
                definition.highlightBackground(),
                definition.inputBackground(),
                definition.inputBorderColor(),
                definition.heroLayout(),
                heroImageUrl,
                definition.heroImageBlendColor());

        return new SimpleFormStyle(style.slug(), style.name(), updatedDefinition);
    }

    private List<FlowQuestion> optimizeProofAssets(List<FlowQuestion> questions, String slug) {
        if (questions == null || questions.isEmpty()) {
            return questions;
        }

        List<FlowQuestion> optimized = new ArrayList<>(questions.size());
        boolean changed = false;

        for (FlowQuestion question : questions) {
            if (question != null && isProofImage(question.dataKey())) {
                String optimizedUrl = migrateAsset(question.title(), slug, question.dataKey());
                if (!Objects.equals(optimizedUrl, question.title())) {
                    changed = true;
                    optimized.add(new FlowQuestion(
                            optimizedUrl,
                            question.dataKey(),
                            question.type(),
                            question.required(),
                            question.description(),
                            question.placeholder(),
                            question.options()));
                    continue;
                }
            }
            optimized.add(question);
        }

        return changed ? List.copyOf(optimized) : questions;
    }

    private boolean isProofImage(String dataKey) {
        return dataKey != null && PROOF_IMAGE_KEY.matcher(dataKey).matches();
    }

    private String migrateAsset(String rawUrl, String slug, String label) {
        if (rawUrl == null || rawUrl.isBlank()) {
            return rawUrl;
        }

        return resolveLegacyUrl(rawUrl)
                .flatMap(uri -> downloadAndUpload(uri, slug, label, rawUrl))
                .orElse(rawUrl);
    }

    private Optional<String> downloadAndUpload(URI uri, String slug, String label, String fallback) {
        return legacyAssetClient
                .fetch(uri.toString())
                .flatMap(asset -> optimizeAndStore(asset, slug, label))
                .or(() -> Optional.ofNullable(fallback));
    }

    private Optional<String> optimizeAndStore(DownloadedAsset asset, String slug, String label) {
        try {
            log.info("Migrating legacy asset '{}' for flow '{}'", asset.fileName(), slug);
            OptimizedImage optimized = imageOptimizer.optimize(asset.content());
            String storedFile = fileStorageService.store(
                    optimized.content(), buildIdentifier(slug, label), asset.fileName(), optimized.contentType());
            return fileStorageService.resolvePublicUrl(storedFile).or(() -> Optional.of(storedFile));
        } catch (Exception ex) {
            log.warn("Failed to optimize legacy asset for flow '{}'", slug, ex);
            return Optional.empty();
        }
    }

    private Optional<URI> resolveLegacyUrl(String rawUrl) {
        if (rawUrl == null) {
            return Optional.empty();
        }
        String trimmed = rawUrl.trim();
        if (trimmed.isEmpty()) {
            return Optional.empty();
        }
        if (isAlreadyServedByPublicCdn(trimmed)) {
            return Optional.empty();
        }

        String uploadsPrefix = legacyAssetsProperties.normalizedUploadPathPrefix();
        String uploadsPrefixWithoutTrailing = uploadsPrefix.endsWith("/")
                ? uploadsPrefix.substring(0, uploadsPrefix.length() - 1)
                : uploadsPrefix;

        try {
            URI uri = URI.create(trimmed);
            if (uri.isAbsolute()) {
                if (uri.getPath() != null && uri.getPath().startsWith(uploadsPrefixWithoutTrailing)) {
                    return Optional.of(legacyAssetsProperties.resolveUploadUri(uri.getPath()));
                }
                if (isLegacyHost(uri) && uri.getPath() != null && uri.getPath().contains(uploadsPrefixWithoutTrailing)) {
                    return Optional.of(uri);
                }
                return Optional.empty();
            }
        } catch (IllegalArgumentException ignored) {
            // Will handle as relative path below.
        }

        if (trimmed.startsWith(uploadsPrefix) || trimmed.startsWith(uploadsPrefixWithoutTrailing)) {
            return Optional.of(legacyAssetsProperties.resolveUploadUri(trimmed));
        }
        if (trimmed.startsWith("/uploads/")) {
            return Optional.of(legacyAssetsProperties.resolveUploadUri(trimmed));
        }
        if (trimmed.startsWith("uploads/")) {
            return Optional.of(legacyAssetsProperties.resolveUploadUri("/" + trimmed));
        }
        return Optional.empty();
    }

    private boolean isAlreadyServedByPublicCdn(String url) {
        String publicBaseUrl = storageProperties.getPublicBaseUrl();
        if (publicBaseUrl == null || publicBaseUrl.isBlank()) {
            return false;
        }
        String normalizedPublicBase = publicBaseUrl.endsWith("/")
                ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1)
                : publicBaseUrl;
        return url.startsWith(normalizedPublicBase);
    }

    private boolean isLegacyHost(URI uri) {
        if (legacyBaseUri == null || legacyBaseUri.getHost() == null) {
            return false;
        }
        return legacyBaseUri.getHost().equalsIgnoreCase(uri.getHost());
    }

    private URI buildLegacyBaseUri(String baseUrl) {
        try {
            if (baseUrl == null || baseUrl.isBlank()) {
                return null;
            }
            return URI.create(baseUrl);
        } catch (IllegalArgumentException ex) {
            log.warn("Invalid legacy assets base URL: {}", baseUrl, ex);
            return null;
        }
    }

    private String buildIdentifier(String slug, String label) {
        String safeSlug = sanitize(slug, "flow");
        String safeLabel = sanitize(label, "asset");
        return safeSlug + "-" + safeLabel;
    }

    private String sanitize(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        String sanitized = value.replaceAll("[^a-zA-Z0-9_-]", "-");
        return sanitized.isBlank() ? fallback : sanitized;
    }
}
