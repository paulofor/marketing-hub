package com.marketinghub.mois.bibliotecapaginavenda.worker.v1.pipeline.pageanalysis;

import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.config.WorkerProperties;
import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.pipeline.StageArtifact;
import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.pipeline.StageContext;
import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.pipeline.StageProcessor;
import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.pipeline.StageResult;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;

/** Executa a análise comercial de páginas capturadas usando a tecnologia de IA isolada nesta etapa. */
@Component
@RequiredArgsConstructor
@Slf4j
public class PageAnalysisProcessor implements StageProcessor<PageAnalysisInput, PageAnalysisOutput> {

    private static final String ARTIFACT_TYPE_EXTRACTED_TEXT = "EXTRACTED_TEXT";
    private static final String ARTIFACT_TYPE_OPENAI_REQUEST = "OPENAI_REQUEST";
    private static final String ARTIFACT_TYPE_OPENAI_RESPONSE = "OPENAI_RESPONSE";

    private final WorkerProperties properties;
    private final OpenAiSalesPageAnalyzer openAiSalesPageAnalyzer;

    /** Extrai texto do HTML capturado, executa a análise comercial e devolve resultado auditável. */
    @Override
    public StageResult<PageAnalysisOutput> process(StageContext<PageAnalysisInput> context) throws Exception {
        PageAnalysisInput input = context.input();
        String text = extractAnalysisText(context);
        SalesPageAnalysisResult analysis = openAiSalesPageAnalyzer.analyze(
                context.execution().idJob(), input.pageId(), input.urlCanonical(), text);
        List<StageArtifact> artifacts = List.of(
                textArtifact(context, input, text),
                payloadArtifact(context, input, ARTIFACT_TYPE_OPENAI_REQUEST, analysis.requestPayloadJson()),
                payloadArtifact(context, input, ARTIFACT_TYPE_OPENAI_RESPONSE, analysis.responsePayloadJson())
        );
        return new StageResult<>(new PageAnalysisOutput(analysis), artifacts, Map.of(
                "scoreTotal", analysis.scoreTotal(),
                "inputTokens", safeMetric(analysis.inputTokens()),
                "outputTokens", safeMetric(analysis.outputTokens())
        ));
    }

    /** Extrai texto usando primeiro o HTML capturado e só usa fallback vivo quando o contrato antigo não trouxer HTML. */
    private String extractAnalysisText(StageContext<PageAnalysisInput> context) throws java.io.IOException {
        PageAnalysisInput input = context.input();
        if (input.rawHtml() != null && !input.rawHtml().isBlank()) {
            var doc = Jsoup.parse(input.rawHtml(), input.urlCanonical());
            String bodyText = doc.body() != null ? doc.body().text() : doc.text();
            return bodyText + "\n\n" + summarizeImagesForAnalysis(input.rawHtml(), input.urlCanonical());
        }
        log.warn("MOIS pageanalysis recebeu job sem rawHtml capturado; usando fallback ao vivo. jobId={}, pageId={}, urlCanonical={}",
                context.execution().idJob(), input.pageId(), input.urlCanonical());
        var doc = Jsoup.connect(input.urlCanonical()).timeout(properties.requestTimeoutMs()).get();
        String bodyText = doc.body() != null ? doc.body().text() : "";
        return bodyText + "\n\n" + summarizeImagesForAnalysis(doc.html(), input.urlCanonical());
    }

    /** Resume evidências visuais do HTML para evitar que a análise trate página carregada de imagens como página sem imagem. */
    String summarizeImagesForAnalysis(String rawHtml, String baseUrl) {
        var doc = Jsoup.parse(rawHtml == null ? "" : rawHtml, baseUrl);
        var images = doc.select("img");
        long proofLikeImages = images.stream()
                .filter(img -> {
                    String evidence = (img.attr("alt") + " " + img.attr("src") + " " + img.attr("class") + " " + (img.parent() == null ? "" : img.parent().text())).toLowerCase();
                    return evidence.contains("depo") || evidence.contains("antes") || evidence.contains("after")
                            || evidence.contains("before") || evidence.contains("resultado") || evidence.contains("print")
                            || evidence.contains("prova") || evidence.contains("whatsapp") || evidence.contains("instagram");
                })
                .count();
        String samples = images.stream()
                .limit(12)
                .map(img -> {
                    String alt = img.attr("alt").isBlank() ? "sem alt" : img.attr("alt");
                    String src = img.attr("src").isBlank() ? img.attr("data-src") : img.attr("src");
                    String parentText = img.parent() == null ? "" : img.parent().text();
                    return "- alt='" + truncateForSummary(alt, 80) + "', src='" + truncateForSummary(src, 100)
                            + "', contexto='" + truncateForSummary(parentText, 140) + "'";
                })
                .reduce("", (left, right) -> left + "\n" + right);
        return "Resumo visual extraído do HTML: total_img=" + images.size()
                + "; imagens_com_sinais_de_prova=" + proofLikeImages
                + "; amostras=" + samples;
    }

    /** Limita textos usados no resumo visual para manter o prompt objetivo e barato. */
    private String truncateForSummary(String value, int maxLength) {
        String normalized = value == null ? "" : value.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength) + "...";
    }

    /** Registra artefato lógico para o texto extraído do HTML capturado. */
    private StageArtifact textArtifact(StageContext<PageAnalysisInput> context, PageAnalysisInput input, String text) {
        String storageKey = context.artifactStore().putText(context.execution().idJob(), ARTIFACT_TYPE_EXTRACTED_TEXT, "text/plain", text);
        return new StageArtifact(ARTIFACT_TYPE_EXTRACTED_TEXT, "sales-page-" + input.pageId() + "-text.txt", "text/plain",
                storageKey, null, text == null ? 0 : text.length(), Map.of("pageId", input.pageId(), "urlCanonical", input.urlCanonical()));
    }

    /** Registra artefato lógico para payloads de integração da etapa. */
    private StageArtifact payloadArtifact(StageContext<PageAnalysisInput> context, PageAnalysisInput input, String type, String payload) {
        String safePayload = payload == null ? "" : payload;
        String storageKey = context.artifactStore().putText(context.execution().idJob(), type, "application/json", safePayload);
        return new StageArtifact(type, "sales-page-" + input.pageId() + "-" + type.toLowerCase() + ".json", "application/json",
                storageKey, null, safePayload.length(), Map.of("pageId", input.pageId(), "urlCanonical", input.urlCanonical()));
    }

    /** Normaliza métrica nula para manter o mapa de métricas sem valores inválidos. */
    private int safeMetric(Integer value) {
        return value == null ? 0 : value;
    }
}
