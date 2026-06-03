package com.marketinghub.mois.bibliotecapaginavenda.worker.v1.pipeline.htmlcapture;

import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.config.WorkerProperties;
import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.pipeline.StageArtifact;
import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.pipeline.StageContext;
import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.pipeline.StageProcessor;
import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.pipeline.StageResult;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;

/** Captura HTML bruto de uma URL da biblioteca e produz artefato rastreável para análise posterior. */
@Component
@RequiredArgsConstructor
@Slf4j
public class HtmlCaptureProcessor implements StageProcessor<HtmlCaptureInput, HtmlCaptureOutput> {

    private static final String ARTIFACT_TYPE_RAW_HTML = "RAW_HTML";

    private final WorkerProperties properties;

    /** Busca a URL, tenta fallback pela raiz redirecionada e devolve HTML bruto com hash, tamanho e metadados HTTP. */
    @Override
    public StageResult<HtmlCaptureOutput> process(StageContext<HtmlCaptureInput> context) throws Exception {
        HtmlCaptureInput input = context.input();
        Connection.Response primary = fetch(input.urlCanonical());
        String redirectDestinationUrl = finalUrl(primary, input.urlCanonical());
        String redirectRootUrl = rootUrl(redirectDestinationUrl);
        Connection.Response effective = primary;
        if (!isCapturable(primary) && shouldTryRedirectRoot(input.urlCanonical(), redirectDestinationUrl, redirectRootUrl)) {
            log.info("MOIS pipeline htmlcapture tentando raiz do redirecionamento. snapshotId={}, pageId={}, redirectDestinationUrl={}, redirectRootUrl={}",
                    context.execution().idJob(), input.pageId(), redirectDestinationUrl, redirectRootUrl);
            effective = fetch(redirectRootUrl);
        }
        String rawHtml = effective.body() == null ? "" : effective.body();
        if (!isCapturable(effective)) {
            throw new HtmlCaptureFailureException("HTTP " + effective.statusCode() + " sem HTML bruto útil", redirectDestinationUrl, redirectRootUrl, effective.statusCode());
        }
        byte[] bytes = rawHtml.getBytes(StandardCharsets.UTF_8);
        String sha256 = sha256(bytes);
        String finalUrl = finalUrl(effective, input.urlCanonical());
        String contentType = effective.contentType() == null ? "text/html" : effective.contentType();
        String storageKey = context.artifactStore().putText(context.execution().idJob(), ARTIFACT_TYPE_RAW_HTML, contentType, rawHtml);
        HtmlCaptureOutput output = new HtmlCaptureOutput(rawHtml, finalUrl, redirectDestinationUrl, redirectRootUrl, effective.statusCode(), contentType, sha256, bytes.length, Instant.now());
        String safeRedirectDestinationUrl = redirectDestinationUrl == null ? "" : redirectDestinationUrl;
        String safeRedirectRootUrl = redirectRootUrl == null ? "" : redirectRootUrl;
        StageArtifact artifact = new StageArtifact(
                ARTIFACT_TYPE_RAW_HTML,
                "sales-page-" + input.pageId() + ".html",
                contentType,
                storageKey,
                sha256,
                bytes.length,
                Map.of("pageId", input.pageId(), "finalUrl", finalUrl, "redirectDestinationUrl", safeRedirectDestinationUrl, "redirectRootUrl", safeRedirectRootUrl, "httpStatus", effective.statusCode()));
        log.info("MOIS pipeline htmlcapture capturou HTML bruto. snapshotId={}, pageId={}, httpStatus={}, finalUrl={}, redirectDestinationUrl={}, redirectRootUrl={}, bytes={}, sha256={}",
                context.execution().idJob(), input.pageId(), effective.statusCode(), finalUrl, redirectDestinationUrl, redirectRootUrl, bytes.length, sha256);
        return new StageResult<>(output, List.of(artifact), Map.of(
                "httpStatus", effective.statusCode(),
                "contentLength", bytes.length,
                "rawHtmlSha256", sha256));
    }

    /** Executa GET com redirecionamentos habilitados para a URL informada. */
    private Connection.Response fetch(String url) throws Exception {
        return Jsoup.connect(url)
                .timeout(properties.requestTimeoutMs())
                .userAgent("MarketingHub-MOIS-HtmlCapture/1.0")
                .ignoreContentType(true)
                .followRedirects(true)
                .maxBodySize(0)
                .execute();
    }

    /** Indica se a resposta HTTP possui corpo HTML aproveitável. */
    private boolean isCapturable(Connection.Response response) {
        String rawHtml = response.body() == null ? "" : response.body();
        return response.statusCode() >= 200 && response.statusCode() < 400 && !rawHtml.isBlank();
    }

    /** Decide se a raiz do domínio redirecionado deve ser tentada como fallback. */
    private boolean shouldTryRedirectRoot(String originalUrl, String redirectDestinationUrl, String redirectRootUrl) {
        return redirectDestinationUrl != null
                && redirectRootUrl != null
                && !redirectDestinationUrl.equals(originalUrl)
                && !redirectRootUrl.equals(redirectDestinationUrl)
                && !redirectRootUrl.equals(originalUrl);
    }

    /** Resolve a URL final reportada pela resposta HTTP. */
    private String finalUrl(Connection.Response response, String fallbackUrl) {
        return response.url() == null ? fallbackUrl : response.url().toString();
    }

    /** Extrai a raiz scheme://host[:port] da URL final de redirecionamento. */
    private String rootUrl(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        URI uri = URI.create(url);
        if (uri.getScheme() == null || uri.getHost() == null) {
            return null;
        }
        String port = uri.getPort() < 0 ? "" : ":" + uri.getPort();
        return uri.getScheme() + "://" + uri.getHost() + port;
    }

    /** Calcula o hash SHA-256 do HTML bruto capturado. */
    private String sha256(byte[] bytes) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(digest.digest(bytes));
    }
}
