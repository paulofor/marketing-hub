package com.marketinghub.socialmediaworker.youtube;

import com.marketinghub.socialmediaworker.config.SocialMediaProperties;
import com.marketinghub.socialmediaworker.dto.YoutubePublicationInput;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

/**
 * Encapsula chamadas oficiais para a YouTube Data API.
 */
@Component
public class YoutubeApiClient {
    private static final String UPLOAD_URL = "https://www.googleapis.com/upload/youtube/v3/videos";

    private final SocialMediaProperties properties;
    private final RestClient restClient;

    /**
     * Recebe configuracoes e cliente HTTP para falar com o YouTube.
     */
    public YoutubeApiClient(SocialMediaProperties properties, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.restClient = restClientBuilder.build();
    }

    /**
     * Publica um video no canal autenticado ou devolve um identificador simulado em dry-run.
     */
    @SuppressWarnings("unchecked")
    public YoutubeUploadResponse uploadVideo(YoutubePublicationInput input) {
        if (properties.youtube().dryRun()) {
            return new YoutubeUploadResponse("dry-run-" + input.publicationId(), "https://youtube.com/watch?v=dry-run-" + input.publicationId());
        }
        if (!StringUtils.hasText(properties.youtube().accessToken())) {
            throw new IllegalArgumentException("Canal YouTube sem OAuth token configurado para publicacao real.");
        }
        if (!StringUtils.hasText(input.videoSourceUrl())) {
            throw new IllegalArgumentException("Publicacao YouTube exige videoSourceUrl.");
        }

        byte[] videoBytes = restClient.get().uri(URI.create(input.videoSourceUrl())).retrieve().body(byte[].class);
        if (videoBytes == null || videoBytes.length == 0) {
            throw new IllegalArgumentException("Arquivo de video vazio ou indisponivel para upload no YouTube.");
        }
        Map<String, Object> metadata = Map.of(
                "snippet",
                Map.of(
                        "title",
                        input.title(),
                        "description",
                        input.description() == null ? "" : input.description(),
                        "tags",
                        input.tags() == null ? List.of() : input.tags(),
                        "categoryId",
                        "22"),
                "status",
                Map.of("privacyStatus", resolvePrivacy(input)));

        ResponseEntity<Void> uploadSession = restClient
                .post()
                .uri(UPLOAD_URL + "?part=snippet,status&uploadType=resumable")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.youtube().accessToken())
                .header("X-Upload-Content-Length", String.valueOf(videoBytes.length))
                .header("X-Upload-Content-Type", "video/mp4")
                .contentType(MediaType.APPLICATION_JSON)
                .body(metadata)
                .retrieve()
                .toBodilessEntity();

        URI uploadUri = uploadSession.getHeaders().getLocation();
        if (uploadUri == null) {
            throw new IllegalStateException("YouTube nao retornou URL de upload resumable.");
        }

        Map<String, Object> response = restClient
                .put()
                .uri(uploadUri)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.youtube().accessToken())
                .contentType(MediaType.valueOf("video/mp4"))
                .body(videoBytes)
                .retrieve()
                .body(Map.class);

        String videoId = response == null ? null : (String) response.get("id");
        return new YoutubeUploadResponse(videoId, videoId == null ? null : "https://youtube.com/watch?v=" + videoId);
    }

    /**
     * Resolve a privacidade final do video respeitando o contrato recebido.
     */
    private String resolvePrivacy(YoutubePublicationInput input) {
        if (StringUtils.hasText(input.privacyStatus())) {
            return input.privacyStatus();
        }
        return StringUtils.hasText(properties.youtube().defaultPrivacyStatus()) ? properties.youtube().defaultPrivacyStatus() : "private";
    }

    /**
     * Representa o retorno minimo de upload do YouTube.
     */
    public record YoutubeUploadResponse(String videoId, String videoUrl) {}
}
