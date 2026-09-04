package com.marketinghub.videomanagement.service.provider;

import com.fasterxml.jackson.databind.JsonNode;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.netty.http.client.HttpClient;

/** Responsabilidade: comprovar que referências Product UGC são imagens públicas e imutáveis antes do consumo. */
@Component
public class RunwayReferenceImageInspector {
    private static final Logger log = LoggerFactory.getLogger(RunwayReferenceImageInspector.class);
    private static final int MAX_IMAGE_BYTES = 8 * 1024 * 1024;
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(15);
    private final WebClient webClient;
    private final boolean allowLocalHttp;

    /** Configura o cliente seguro usado pelo executor para validar referências públicas. */
    @Autowired
    public RunwayReferenceImageInspector(WebClient.Builder builder) {
        this(builder, false);
    }

    /** Permite testes locais explícitos sem enfraquecer a validação HTTPS de produção. */
    RunwayReferenceImageInspector(WebClient.Builder builder, boolean allowLocalHttp) {
        this.allowLocalHttp = allowLocalHttp;
        this.webClient =
                builder
                        .clientConnector(
                                new ReactorClientHttpConnector(
                                        HttpClient.create()
                                                .followRedirect(false)
                                                .responseTimeout(REQUEST_TIMEOUT)))
                        .exchangeStrategies(
                                ExchangeStrategies.builder()
                                        .codecs(
                                                configurer ->
                                                        configurer
                                                                .defaultCodecs()
                                                                .maxInMemorySize(MAX_IMAGE_BYTES))
                                        .build())
                        .build();
    }

    /** Baixa e inspeciona as duas referências exigidas pela receita Product UGC. */
    public List<Evidence> inspectProductUgc(String characterUri, String productUri) {
        return List.of(
                inspect("CHARACTER_IMAGE", characterUri),
                inspect("PRODUCT_IMAGE", productUri));
    }

    /** Reprova alteração de bytes, formato ou dimensões entre o preflight e a chamada paga. */
    public void requireMatches(JsonNode expected, List<Evidence> actual) {
        if (!expected.isArray() || expected.size() != 2 || actual.size() != 2) {
            throw new VideoProviderException(
                    "PROVIDER_REFERENCE_DRIFT",
                    "As duas referências Product UGC não possuem evidência congelada.");
        }
        Map<String, JsonNode> expectedByRole = new LinkedHashMap<>();
        expected.forEach(value -> expectedByRole.put(value.path("role").asText(), value));
        for (Evidence evidence : actual) {
            JsonNode frozen = expectedByRole.get(evidence.role());
            boolean matches =
                    frozen != null
                            && evidence.sourceHost().equals(frozen.path("sourceHost").asText())
                            && evidence.contentType().equals(frozen.path("contentType").asText())
                            && evidence.contentLength() == frozen.path("contentLength").asLong(-1)
                            && evidence.width() == frozen.path("width").asInt(-1)
                            && evidence.height() == frozen.path("height").asInt(-1)
                            && evidence.sha256().equals(frozen.path("sha256").asText());
            if (!matches) {
                throw new VideoProviderException(
                        "PROVIDER_REFERENCE_DRIFT",
                        "A referência " + evidence.role()
                                + " mudou depois do preflight; nenhuma chamada paga foi iniciada.");
            }
        }
    }

    /** Converte evidência técnica em mapa sanitizado, sem persistir query string da URL. */
    public Map<String, Object> audit(Evidence evidence) {
        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        result.put("role", evidence.role());
        result.put("sourceHost", evidence.sourceHost());
        result.put("contentType", evidence.contentType());
        result.put("contentLength", evidence.contentLength());
        result.put("width", evidence.width());
        result.put("height", evidence.height());
        result.put("sha256", evidence.sha256());
        return result;
    }

    /** Valida rede, mídia, dimensão, proporção e hash de uma referência individual. */
    private Evidence inspect(String role, String value) {
        URI uri = publicImageUri(value);
        String safeUri = safeUri(uri);
        try {
            ResponseEntity<byte[]> response =
                    webClient
                            .get()
                            .uri(uri)
                            .accept(MediaType.IMAGE_PNG, MediaType.IMAGE_JPEG)
                            .retrieve()
                            .toEntity(byte[].class)
                            .timeout(REQUEST_TIMEOUT)
                            .block();
            byte[] content = response == null ? null : response.getBody();
            MediaType contentType = response == null ? null : response.getHeaders().getContentType();
            if (content == null
                    || content.length == 0
                    || content.length > MAX_IMAGE_BYTES
                    || !supportedContentType(contentType)) {
                throw invalid(role, "a URL não retornou PNG/JPEG dentro do limite de 8 MB");
            }
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(content));
            if (image == null || image.getWidth() <= 0 || image.getHeight() <= 0) {
                throw invalid(role, "o conteúdo não pôde ser decodificado como imagem");
            }
            double ratio = image.getWidth() / (double) image.getHeight();
            if (ratio < 0.4 || ratio > 4.0) {
                throw invalid(role, "a proporção deve permanecer entre 0,4 e 4");
            }
            Evidence evidence =
                    new Evidence(
                            role,
                            uri.getHost().toLowerCase(java.util.Locale.ROOT),
                            contentType.toString(),
                            content.length,
                            image.getWidth(),
                            image.getHeight(),
                            sha256(content));
            log.info(
                    "Referência Product UGC validada; role={} url={} type={} bytes={} width={} height={} sha256={}",
                    role,
                    safeUri,
                    evidence.contentType(),
                    evidence.contentLength(),
                    evidence.width(),
                    evidence.height(),
                    evidence.sha256());
            return evidence;
        } catch (WebClientResponseException ex) {
            log.error(
                    "Falha HTTP ao validar referência Product UGC; role={} url={} status={}",
                    role,
                    safeUri,
                    ex.getStatusCode().value(),
                    ex);
            throw invalid(role, "a URL respondeu HTTP " + ex.getStatusCode().value(), ex);
        } catch (IOException ex) {
            log.error(
                    "Falha ao decodificar referência Product UGC; role={} url={}", role, safeUri, ex);
            throw invalid(role, "a imagem não pôde ser decodificada", ex);
        } catch (VideoProviderException ex) {
            log.warn(
                    "Referência Product UGC recusada; role={} url={} code={}",
                    role,
                    safeUri,
                    ex.getCode(),
                    ex);
            throw ex;
        } catch (RuntimeException ex) {
            log.error(
                    "Falha de integração ao validar referência Product UGC; role={} url={}",
                    role,
                    safeUri,
                    ex);
            throw invalid(role, "a referência está indisponível", ex);
        }
    }

    /** Exige URL HTTPS pública e bloqueia destinos locais antes do download. */
    private URI publicImageUri(String value) {
        try {
            URI uri = URI.create(value == null ? "" : value.trim());
            boolean allowedScheme =
                    "https".equalsIgnoreCase(uri.getScheme())
                            || (allowLocalHttp && "http".equalsIgnoreCase(uri.getScheme()));
            if (!allowedScheme
                    || !StringUtils.hasText(uri.getHost())
                    || uri.getUserInfo() != null
                    || (!allowLocalHttp && uri.getPort() != -1 && uri.getPort() != 443)) {
                throw invalid("REFERENCE", "a URL deve ser HTTPS pública, sem credenciais ou porta privada");
            }
            if (!allowLocalHttp) requirePublicHost(uri.getHost());
            return uri;
        } catch (IllegalArgumentException ex) {
            log.warn("URL de referência Product UGC inválida", ex);
            throw invalid("REFERENCE", "a URL é inválida", ex);
        }
    }

    /** Resolve o host e recusa endereços privados, locais, multicast ou não roteáveis. */
    private void requirePublicHost(String host) {
        try {
            for (InetAddress address : InetAddress.getAllByName(host)) {
                if (address.isAnyLocalAddress()
                        || address.isLoopbackAddress()
                        || address.isLinkLocalAddress()
                        || address.isSiteLocalAddress()
                        || address.isMulticastAddress()) {
                    throw invalid("REFERENCE", "o host resolve para uma rede não pública");
                }
            }
        } catch (UnknownHostException ex) {
            log.error("Host de referência Product UGC não pôde ser resolvido; host={}", host, ex);
            throw invalid("REFERENCE", "o host não pôde ser resolvido", ex);
        }
    }

    /** Confirma os formatos raster suportados e impede fallback HTML do frontend. */
    private boolean supportedContentType(MediaType contentType) {
        return contentType != null
                && (MediaType.IMAGE_PNG.isCompatibleWith(contentType)
                        || MediaType.IMAGE_JPEG.isCompatibleWith(contentType));
    }

    /** Calcula o identificador imutável dos bytes usados no preflight. */
    private String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException ex) {
            log.error("SHA-256 indisponível ao validar referência Product UGC", ex);
            throw invalid("REFERENCE", "SHA-256 indisponível", ex);
        }
    }

    /** Remove query e fragmento antes de registrar a origem em log. */
    private String safeUri(URI uri) {
        return uri.getScheme() + "://" + uri.getHost() + uri.getPath();
    }

    /** Cria uma falha funcional estável para o preflight e para o render. */
    private VideoProviderException invalid(String role, String detail) {
        return new VideoProviderException(
                "PROVIDER_REFERENCE_INVALID", "Referência " + role + " inválida: " + detail + ".");
    }

    /** Preserva a causa técnica completa ao criar a falha funcional. */
    private VideoProviderException invalid(String role, String detail, Throwable cause) {
        return new VideoProviderException(
                "PROVIDER_REFERENCE_INVALID", "Referência " + role + " inválida: " + detail + ".", cause);
    }

    /** Resume a mídia pública sem guardar o conteúdo ou parâmetros sensíveis da URL. */
    public record Evidence(
            String role,
            String sourceHost,
            String contentType,
            long contentLength,
            int width,
            int height,
            String sha256) {}
}
