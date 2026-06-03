package com.marketinghub.mois.bibliotecapaginavenda.worker.v1.pipeline.htmlcapture;

import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.config.WorkerProperties;
import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.pipeline.StageArtifact;
import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.pipeline.StageContext;
import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.pipeline.StageProcessor;
import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.pipeline.StageResult;
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

    /** Busca a URL na internet, valida conteúdo útil e devolve HTML bruto com hash, tamanho e metadados HTTP. */
    @Override
    public StageResult<HtmlCaptureOutput> process(StageContext<HtmlCaptureInput> context) throws Exception {
        HtmlCaptureInput input = context.input();
        Connection.Response response = Jsoup.connect(input.urlCanonical())
                .timeout(properties.requestTimeoutMs())
                .userAgent("MarketingHub-MOIS-HtmlCapture/1.0")
                .ignoreContentType(true)
                .followRedirects(true)
                .maxBodySize(0)
                .execute();
        String rawHtml = response.body() == null ? "" : response.body();
        if (response.statusCode() < 200 || response.statusCode() >= 400 || rawHtml.isBlank()) {
            throw new IllegalStateException("HTTP " + response.statusCode() + " sem HTML bruto útil");
        }
        byte[] bytes = rawHtml.getBytes(StandardCharsets.UTF_8);
        String sha256 = sha256(bytes);
        String finalUrl = response.url() == null ? input.urlCanonical() : response.url().toString();
        String contentType = response.contentType() == null ? "text/html" : response.contentType();
        String storageKey = context.artifactStore().putText(context.execution().idJob(), ARTIFACT_TYPE_RAW_HTML, contentType, rawHtml);
        HtmlCaptureOutput output = new HtmlCaptureOutput(rawHtml, finalUrl, response.statusCode(), contentType, sha256, bytes.length, Instant.now());
        StageArtifact artifact = new StageArtifact(
                ARTIFACT_TYPE_RAW_HTML,
                "sales-page-" + input.pageId() + ".html",
                contentType,
                storageKey,
                sha256,
                bytes.length,
                Map.of("pageId", input.pageId(), "finalUrl", finalUrl, "httpStatus", response.statusCode()));
        log.info("MOIS pipeline htmlcapture capturou HTML bruto. snapshotId={}, pageId={}, httpStatus={}, bytes={}, sha256={}",
                context.execution().idJob(), input.pageId(), response.statusCode(), bytes.length, sha256);
        return new StageResult<>(output, List.of(artifact), Map.of(
                "httpStatus", response.statusCode(),
                "contentLength", bytes.length,
                "rawHtmlSha256", sha256));
    }

    /** Calcula o hash SHA-256 do HTML bruto capturado. */
    private String sha256(byte[] bytes) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(digest.digest(bytes));
    }
}
