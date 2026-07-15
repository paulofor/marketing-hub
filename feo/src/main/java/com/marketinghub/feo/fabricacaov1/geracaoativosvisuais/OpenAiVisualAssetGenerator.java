package com.marketinghub.feo.fabricacaov1.geracaoativosvisuais;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.feo.fabricacaov1.contract.VisualAsset;
import com.marketinghub.feo.fabricacaov1.contract.VisualAssetSpec;
import com.marketinghub.feo.infrastructure.config.FeoProperties;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Gera imagens ricas da FEO usando a API oficial de imagens da OpenAI.
 */
@Component
public class OpenAiVisualAssetGenerator implements VisualAssetGenerator {

    private static final Duration REQUEST_TIMEOUT = Duration.ofMinutes(5);

    private final WebClient openAiWebClient;
    private final FeoProperties properties;
    private final ObjectMapper objectMapper;

    /**
     * Recebe cliente HTTP, configuração e serializador para auditar request e response.
     */
    public OpenAiVisualAssetGenerator(
            @Qualifier("openAiWebClient") WebClient openAiWebClient,
            FeoProperties properties,
            ObjectMapper objectMapper) {
        this.openAiWebClient = openAiWebClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    /**
     * Chama `/v1/images/generations` e converte o primeiro `b64_json` em PNG final.
     */
    @Override
    public VisualAsset generate(VisualAssetSpec spec) {
        if (!properties.hasOpenAiApiKey()) {
            throw new IllegalStateException("OPENAI_API_KEY não configurada para gerar imagens FEO");
        }
        String openAiApiKey = properties.resolvedOpenAiApiKey();
        Map<String, Object> request = Map.of(
                "model", properties.imageModel(),
                "prompt", spec.prompt(),
                "size", spec.size(),
                "quality", properties.imageQuality(),
                "output_format", spec.outputFormat(),
                "n", 1);
        OpenAiImageResponse response = openAiWebClient.post()
                .uri("/v1/images/generations")
                .headers(headers -> headers.setBearerAuth(openAiApiKey))
                .bodyValue(request)
                .retrieve()
                .bodyToMono(OpenAiImageResponse.class)
                .block(REQUEST_TIMEOUT);
        if (response == null || response.data() == null || response.data().isEmpty()
                || response.data().getFirst().b64_json() == null) {
            throw new IllegalStateException("OpenAI não retornou imagem para " + spec.code());
        }
        byte[] image = Base64.getDecoder().decode(response.data().getFirst().b64_json());
        return new VisualAsset(
                spec.code(),
                spec.title(),
                spec.assetType(),
                fileName(spec),
                "image/" + spec.outputFormat(),
                image,
                spec.prompt(),
                properties.imageModel(),
                toJson(request),
                toJson(response),
                List.of("Imagem gerada pela OpenAI para enriquecer o produto final", "Aprovada para revisão editorial"));
    }

    /**
     * Define nome estável para a imagem no pacote final.
     */
    private String fileName(VisualAssetSpec spec) {
        return "imagens/" + spec.code().toLowerCase() + "-" + slug(spec.title()) + "." + spec.outputFormat();
    }

    /**
     * Normaliza título para caminho de arquivo.
     */
    private String slug(String value) {
        String safe = value == null ? "imagem" : value.toLowerCase();
        safe = java.text.Normalizer.normalize(safe, java.text.Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        safe = safe.replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        return safe.isBlank() ? "imagem" : safe;
    }

    /**
     * Serializa payload para auditoria do job.
     */
    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Falha ao serializar auditoria de imagem FEO", ex);
        }
    }

    /**
     * Representa a resposta necessária da API de imagens.
     */
    private record OpenAiImageResponse(List<OpenAiImageData> data) {
    }

    /**
     * Representa uma imagem base64 retornada pela OpenAI.
     */
    private record OpenAiImageData(String b64_json, String revised_prompt) {
    }
}
