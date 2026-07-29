package com.marketinghub.socialmediaworker.youtube;

import com.marketinghub.socialmediaworker.config.SocialMediaProperties;
import com.marketinghub.socialmediaworker.dto.YoutubePublicationAction;
import com.marketinghub.socialmediaworker.dto.YoutubePublicationInput;
import com.marketinghub.socialmediaworker.dto.YoutubePublicationOutput;
import com.marketinghub.socialmediaworker.pipeline.StageResult;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Integra o worker com os contratos internos do backend para YouTube.
 */
@Component
public class YoutubeBackendClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(YoutubeBackendClient.class);
    private static final String PENDING_PATH = "/api/social-distribution/publications/pending";
    private static final String PUBLISHING_PATH = "/api/social-distribution/publications/%d/publishing";
    private static final String PUBLISHED_PATH = "/api/social-distribution/publications/%d/published";
    private static final String FAILED_PATH = "/api/social-distribution/publications/%d/failed";

    private final SocialMediaProperties properties;
    private final RestClient restClient;

    /**
     * Recebe configuracoes e cliente HTTP para consumir o backend.
     */
    public YoutubeBackendClient(SocialMediaProperties properties, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.restClient = restClientBuilder.build();
    }

    /**
     * Busca execucoes pendentes no endpoint canonico da etapa YouTube.
     */
    public List<YoutubePublicationInput> fetchPending(int limit) {
        String url = properties.backend().baseUrl() + PENDING_PATH;
        try {
            LOGGER.info("Buscando pendencias YouTube no backend: url==>{}", url);
            List<SocialVideoPublicationPending> pending = restClient
                    .get()
                    .uri(url)
                    .headers(this::applyAuth)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            return pending == null
                    ? List.of()
                    : pending.stream()
                            .filter(item -> "YOUTUBE".equals(item.platform()))
                            .limit(limit)
                            .map(this::toYoutubeInput)
                            .toList();
        } catch (RestClientException ex) {
            LOGGER.error("Falha ao buscar pendencias YouTube no backend: url<=={}, erro={}", url, ex.getMessage(), ex);
            return List.of();
        }
    }

    /**
     * Marca no backend que a publicacao foi assumida pelo worker.
     */
    public void markPublishing(Long publicationId) {
        String url = properties.backend().baseUrl() + PUBLISHING_PATH.formatted(publicationId);
        try {
            LOGGER.info("Marcando publicacao social como PUBLISHING: url==>{}", url);
            restClient.post().uri(url).headers(this::applyAuth).retrieve().toBodilessEntity();
        } catch (RestClientException ex) {
            LOGGER.error("Falha ao marcar publicacao social como PUBLISHING: url<=={}, erro={}", url, ex.getMessage(), ex);
        }
    }

    /**
     * Envia o resultado de uma execucao YouTube para persistencia no backend.
     */
    public void reportResult(Long executionId, StageResult<YoutubePublicationOutput> result) {
        if (result.success()) {
            reportPublished(executionId, result.output());
        } else {
            reportFailed(executionId, result.errorCategory(), result.errorMessage());
        }
    }

    /**
     * Envia sucesso de publicacao ao backend de distribuicao organica.
     */
    private void reportPublished(Long publicationId, YoutubePublicationOutput output) {
        String url = properties.backend().baseUrl() + PUBLISHED_PATH.formatted(publicationId);
        try {
            LOGGER.info("Reportando publicacao YouTube concluida: url==>{}, publicationId={}", url, publicationId);
            restClient
                    .post()
                    .uri(url)
                    .headers(this::applyAuth)
                    .body(Map.of("publishedUrl", output.externalUrl(), "externalPostId", output.externalVideoId(), "publishedAt", Instant.now().toString()))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException ex) {
            LOGGER.error("Falha ao reportar publicacao YouTube concluida: url<=={}, erro={}", url, ex.getMessage(), ex);
        }
    }

    /**
     * Envia falha de publicacao ao backend de distribuicao organica.
     */
    private void reportFailed(Long publicationId, String errorCategory, String errorMessage) {
        String url = properties.backend().baseUrl() + FAILED_PATH.formatted(publicationId);
        try {
            LOGGER.info("Reportando falha YouTube: url==>{}, publicationId={}, errorCategory={}", url, publicationId, errorCategory);
            restClient
                    .post()
                    .uri(url)
                    .headers(this::applyAuth)
                    .body(Map.of("errorCategory", errorCategory == null ? "WORKER_ERROR" : errorCategory, "errorMessage", errorMessage == null ? "" : errorMessage))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException ex) {
            LOGGER.error("Falha ao reportar erro YouTube ao backend: url<=={}, erro={}", url, ex.getMessage(), ex);
        }
    }

    /**
     * Converte o contrato existente da distribuicao organica na entrada do processor YouTube.
     */
    private YoutubePublicationInput toYoutubeInput(SocialVideoPublicationPending pending) {
        return new YoutubePublicationInput(
                pending.id(),
                pending.productId(),
                null,
                pending.socialAccountExternalAccountId(),
                YoutubePublicationAction.PUBLISH_VIDEO,
                pending.videoUrl(),
                pending.title(),
                pending.caption(),
                splitTags(pending.hashtags()),
                "private",
                null,
                "Aquecimento organico do produto " + pending.productName());
    }

    /**
     * Converte hashtags de texto livre em lista simples de tags.
     */
    private List<String> splitTags(String hashtags) {
        if (!StringUtils.hasText(hashtags)) {
            return List.of();
        }
        return Arrays.stream(hashtags.split("\\s+"))
                .map(tag -> tag.replace("#", "").trim())
                .filter(StringUtils::hasText)
                .toList();
    }

    /**
     * Aplica autenticacao interna quando configurada.
     */
    private void applyAuth(HttpHeaders headers) {
        if (StringUtils.hasText(properties.backend().authToken())) {
            headers.setBearerAuth(properties.backend().authToken());
        }
        headers.setAccept(Arrays.asList(org.springframework.http.MediaType.APPLICATION_JSON));
    }

    /**
     * Representa somente os campos usados da resposta existente de distribuicao organica.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SocialVideoPublicationPending(
            Long id,
            Long productId,
            String productName,
            String platform,
            String title,
            String caption,
            String hashtags,
            String videoUrl,
            String socialAccountExternalAccountId) {}
}
