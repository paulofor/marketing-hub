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
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Migra e otimiza os ativos visuais persistidos nos fluxos públicos do Lead Portal. */
@Service
public class FlowAssetService {

    private static final Pattern PROOF_IMAGE_KEY = Pattern.compile("^exemplo_real_card_\\d+_imagem_url$");
    private static final Pattern IMAGE_TAG = Pattern.compile("<img\\b[^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern IMAGE_SOURCE = Pattern.compile(
            "\\bsrc\\s*=\\s*([\"'])(https?://[^\"']+)\\1", Pattern.CASE_INSENSITIVE);
    private static final String WEB_OPTIMIZED_MARKER = "data-mh-web-optimized";

    private final LegacyAssetClient legacyAssetClient;
    private final FileStorageService fileStorageService;
    private final ImageOptimizer imageOptimizer;
    private final LegacyAssetsProperties legacyAssetsProperties;
    private final StorageProperties storageProperties;
    private final URI legacyBaseUri;
    private final URI publicBaseUri;

    private static final Logger log = LoggerFactory.getLogger(FlowAssetService.class);

    /** Inicializa o tratamento de ativos com download, otimização e armazenamento versionado. */
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
        this.publicBaseUri = buildBaseUri(storageProperties.getPublicBaseUrl());
    }

    /** Otimiza estilos, provas e imagens incorporadas no HTML standalone do fluxo. */
    public Flow optimizeAssets(Flow flow) {
        if (flow == null) {
            return null;
        }

        SimpleFormStyle optimizedStyle = optimizeStyle(flow.simpleFormStyle(), flow.slug());
        List<FlowQuestion> optimizedQuestions = optimizeProofAssets(flow.questions(), flow.slug());
        String optimizedCustomFormHtml = optimizeStandaloneHtml(flow.customFormHtml(), flow.slug());

        return new Flow(
                flow.slug(),
                flow.name(),
                flow.description(),
                optimizedCustomFormHtml,
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

    /** Substitui o recurso baixado pelo navegador por um derivado leve sem alterar o src canônico. */
    private String optimizeStandaloneHtml(String html, String slug) {
        if (html == null || html.isBlank() || !html.toLowerCase().contains("<img")) {
            return html;
        }
        Matcher matcher = IMAGE_TAG.matcher(html);
        StringBuffer result = new StringBuffer(html.length());
        Map<String, String> optimizedBySource = new HashMap<>();
        int optimizedImages = 0;
        while (matcher.find()) {
            String imageTag = matcher.group();
            String replacement = imageTag;
            if (!imageTag.toLowerCase().contains(WEB_OPTIMIZED_MARKER)) {
                Matcher sourceMatcher = IMAGE_SOURCE.matcher(imageTag);
                if (sourceMatcher.find()) {
                    String source = sourceMatcher.group(2).replace("&amp;", "&");
                    String optimizedUrl = optimizedBySource.get(source);
                    if (optimizedUrl == null && !optimizedBySource.containsKey(source)) {
                        optimizedUrl = optimizeWebImage(source, slug).orElse("");
                        optimizedBySource.put(source, optimizedUrl);
                    }
                    if (optimizedUrl != null && !optimizedUrl.isBlank()) {
                        replacement = addWebDeliveryAttributes(imageTag, optimizedUrl, optimizedImages == 0);
                        optimizedImages++;
                    }
                }
            }
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    /** Baixa somente imagens de hosts confiáveis e publica o derivado JPEG da landing. */
    private Optional<String> optimizeWebImage(String rawUrl, String slug) {
        Optional<URI> trustedUri = resolveTrustedWebImageUri(rawUrl);
        if (trustedUri.isEmpty()) {
            return Optional.empty();
        }
        return legacyAssetClient
                .fetch(trustedUri.get().toString())
                .flatMap(asset -> optimizeAndStoreForWeb(asset, slug, rawUrl));
    }

    /** Converte e armazena um derivado específico para carregamento web da landing. */
    private Optional<String> optimizeAndStoreForWeb(DownloadedAsset asset, String slug, String sourceUrl) {
        try {
            OptimizedImage optimized = imageOptimizer.optimizeForLanding(asset.content());
            String storedFile = fileStorageService.store(
                    optimized.content(),
                    buildIdentifier(slug, "landing-web-" + stableSourceHash(sourceUrl)),
                    toJpegFileName(asset.fileName()),
                    optimized.contentType());
            return fileStorageService.resolvePublicUrl(storedFile).or(() -> Optional.of(storedFile));
        } catch (Exception ex) {
            log.warn("Falha ao otimizar imagem da landing. flowSlug={}, sourceUrl={}", slug, sourceUrl, ex);
            return Optional.empty();
        }
    }

    /** Mantém o src aprovado e orienta o navegador a selecionar o derivado otimizado. */
    private String addWebDeliveryAttributes(String imageTag, String optimizedUrl, boolean priority) {
        String enhanced = addAttributeIfMissing(imageTag, "srcset", escapeHtmlAttribute(optimizedUrl) + " 1x");
        enhanced = addAttributeIfMissing(enhanced, WEB_OPTIMIZED_MARKER, "true");
        enhanced = addAttributeIfMissing(enhanced, "loading", priority ? "eager" : "lazy");
        enhanced = addAttributeIfMissing(enhanced, "decoding", "async");
        return addAttributeIfMissing(enhanced, "fetchpriority", priority ? "high" : "low");
    }

    /** Acrescenta um atributo antes do fechamento da tag quando ele ainda não está declarado. */
    private String addAttributeIfMissing(String tag, String name, String value) {
        Pattern attribute = Pattern.compile("\\b" + Pattern.quote(name) + "\\s*=", Pattern.CASE_INSENSITIVE);
        if (attribute.matcher(tag).find()) {
            return tag;
        }
        int insertionPoint = tag.endsWith("/>") ? tag.length() - 2 : tag.length() - 1;
        return tag.substring(0, insertionPoint)
                + " "
                + name
                + "=\""
                + value
                + "\""
                + tag.substring(insertionPoint);
    }

    /** Aceita somente URLs HTTP dos hosts configurados para legado ou CDN pública. */
    private Optional<URI> resolveTrustedWebImageUri(String rawUrl) {
        try {
            URI uri = URI.create(rawUrl);
            if (!uri.isAbsolute() || !("http".equalsIgnoreCase(uri.getScheme())
                    || "https".equalsIgnoreCase(uri.getScheme()))) {
                return Optional.empty();
            }
            if (sameHost(uri, publicBaseUri) || sameHost(uri, legacyBaseUri)) {
                return Optional.of(uri);
            }
        } catch (IllegalArgumentException ex) {
            log.warn("URL de imagem da landing ignorada por formato inválido. rawUrl={}", rawUrl, ex);
        }
        return Optional.empty();
    }

    /** Compara hosts absolutos sem confiar apenas em prefixos textuais de URL. */
    private boolean sameHost(URI candidate, URI configuredBase) {
        return candidate != null
                && configuredBase != null
                && candidate.getHost() != null
                && configuredBase.getHost() != null
                && candidate.getHost().equalsIgnoreCase(configuredBase.getHost());
    }

    /** Gera um identificador estável e curto para reutilizar a mesma origem no HTML. */
    private String stableSourceHash(String sourceUrl) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(sourceUrl.getBytes(StandardCharsets.UTF_8));
            StringBuilder hash = new StringBuilder();
            for (int index = 0; index < 6; index++) {
                hash.append(String.format("%02x", digest[index]));
            }
            return hash.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 indisponível para identificar ativo da landing", ex);
        }
    }

    /** Normaliza o nome armazenado para refletir o conteúdo JPEG gerado. */
    private String toJpegFileName(String fileName) {
        String normalized = fileName == null || fileName.isBlank() ? "landing-image" : fileName;
        return normalized.replaceFirst("(?i)\\.[a-z0-9]+$", "") + ".jpg";
    }

    /** Escapa caracteres que poderiam encerrar o valor do atributo HTML. */
    private String escapeHtmlAttribute(String value) {
        return value.replace("&", "&amp;").replace("\"", "&quot;");
    }

    /** Migra os ativos legados declarados no estilo visual do formulário. */
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

    /** Migra as imagens de prova declaradas como perguntas técnicas do fluxo. */
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

    /** Identifica a chave canônica usada por imagens de prova social. */
    private boolean isProofImage(String dataKey) {
        return dataKey != null && PROOF_IMAGE_KEY.matcher(dataKey).matches();
    }

    /** Migra um ativo legado quando a origem pertence ao host configurado. */
    private String migrateAsset(String rawUrl, String slug, String label) {
        if (rawUrl == null || rawUrl.isBlank()) {
            return rawUrl;
        }

        return resolveLegacyUrl(rawUrl)
                .flatMap(uri -> downloadAndUpload(uri, slug, label, rawUrl))
                .orElse(rawUrl);
    }

    /** Baixa, otimiza e armazena o ativo, preservando a URL anterior em caso de falha. */
    private Optional<String> downloadAndUpload(URI uri, String slug, String label, String fallback) {
        return legacyAssetClient
                .fetch(uri.toString())
                .flatMap(asset -> optimizeAndStore(asset, slug, label))
                .or(() -> Optional.ofNullable(fallback));
    }

    /** Otimiza e armazena um ativo de formulário ou prova com resolução ampla. */
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

    /** Resolve somente caminhos e URLs pertencentes ao repositório legado configurado. */
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
            // O caminho relativo é tratado pelas regras abaixo.
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

    /** Confirma se uma URL já pertence à base pública atual do armazenamento. */
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

    /** Confirma se a URI recebida pertence ao host legado permitido. */
    private boolean isLegacyHost(URI uri) {
        if (legacyBaseUri == null || legacyBaseUri.getHost() == null) {
            return false;
        }
        return legacyBaseUri.getHost().equalsIgnoreCase(uri.getHost());
    }

    /** Converte a configuração da base legada para URI. */
    private URI buildLegacyBaseUri(String baseUrl) {
        return buildBaseUri(baseUrl);
    }

    /** Converte uma URL base configurada em URI, preservando falha explícita em log. */
    private URI buildBaseUri(String baseUrl) {
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

    /** Monta o prefixo estável usado pelo armazenamento de ativos. */
    private String buildIdentifier(String slug, String label) {
        String safeSlug = sanitize(slug, "flow");
        String safeLabel = sanitize(label, "asset");
        return safeSlug + "-" + safeLabel;
    }

    /** Remove caracteres incompatíveis com a chave de armazenamento. */
    private String sanitize(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        String sanitized = value.replaceAll("[^a-zA-Z0-9_-]", "-");
        return sanitized.isBlank() ? fallback : sanitized;
    }
}
