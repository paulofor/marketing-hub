package com.marketinghub.pipelines.geracaoanuncios.v1.texto;

import com.marketinghub.worker.util.UrlUtils;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/** Responsabilidade: consumir os contratos backend da etapa Texto do GeracaoAnuncios v1. */
@Component
public class GeraAnuncioTextoBackendClient {
    public static final String PENDING_ENDPOINT = "/internal/aiworker/geracaoanuncios/v1/texto/stage-executions/pending";

    private static final Logger log = LoggerFactory.getLogger(GeraAnuncioTextoBackendClient.class);

    private final WebClient webClient;
    private final String backendBaseUrl;
    private final String apiPrefix;

    /** Inicializa o cliente HTTP com URL base e prefixo oficial da API do backend. */
    public GeraAnuncioTextoBackendClient(
            WebClient.Builder builder,
            @Value("${backend.base-url:http://191.252.181.168:8000}") String backendBaseUrl,
            @Value("${backend.api-prefix:/api}") String apiPrefix) {
        this.webClient = builder.build();
        this.backendBaseUrl = backendBaseUrl;
        this.apiPrefix = apiPrefix;
    }

    /** Busca execuções pendentes pelo endpoint pending canônico da etapa Texto no backend. */
    public List<GeraAnuncioTextoInput> fetchPending() {
        String uri = UrlUtils.joinPath(backendBaseUrl, apiPrefix, PENDING_ENDPOINT);
        log.info("Buscando pending GeracaoAnuncios v1 Texto. endpoint={}", uri);
        List<GeraAnuncioTextoInput> response = webClient.post()
                .uri(uri)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<GeraAnuncioTextoInput>>() {})
                .doOnNext(payload -> log.info(
                        "Resposta pending GeracaoAnuncios v1 Texto recebida. endpoint={} quantidade={}",
                        uri,
                        payload.size()))
                .block();
        return response != null ? response : List.of();
    }
}
