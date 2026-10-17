package com.marketinghub.worker.pipeline.gerasalespagev1;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.worker.openai.core.exception.StageWorkerException;
import java.net.URI;
import java.util.Map;
import org.jsoup.Jsoup;

/** Responsabilidade: preservar a demonstração exata aprovada no HTML low-ticket revisado e publicado. */
final class GeraSalesPageReviewedVideo {
    /** Impede instâncias de um renderizador puro sem rede ou geração de mídia. */
    private GeraSalesPageReviewedVideo() {}

    /** Valida somente vínculo explícito e bloqueia entrada inválida antes de consumir modelo. */
    static JsonNode validate(Map<String, Object> data, ObjectMapper json) {
        JsonNode video = json.valueToTree(data).path("experiment").path("approvedLandingVideo");
        if (video.isMissingNode() || video.isNull()) return video;
        boolean valid = video.isObject() && video.path("assetId").isIntegralNumber()
                && video.path("assetId").asLong() > 0 && "LANDING_HERO".equals(video.path("role").asText())
                && "APPROVED".equals(video.path("reviewStatus").asText())
                && video.path("sha256").asText().matches("[a-fA-F0-9]{64}")
                && video.path("captionsBurnedIn").isBoolean() && video.path("captionsBurnedIn").asBoolean()
                && video.path("captions").isTextual() && !video.path("captions").asText().isBlank()
                && secure(video.path("assetUrl").asText());
        if (!valid) throw new StageWorkerException("A demonstração selecionada não possui mídia, legenda e aprovação válidas.");
        return video;
    }

    /** Acrescenta ou substitui somente o bloco próprio sem alterar preço, checkout ou voz. */
    static String render(String html, Map<String, Object> data, ObjectMapper json) {
        var video = validate(data, json);
        if (video.isMissingNode() || video.isNull()) return html;
        var document = Jsoup.parse(html);
        document.outputSettings().prettyPrint(false);
        document.select("[data-mh-reviewed-video=v1]").remove();
        document.select("video").stream().filter(player -> video.path("assetUrl").asText().equals(player.attr("src"))
                || player.select("source").stream().anyMatch(source -> video.path("assetUrl").asText().equals(source.attr("src"))))
                .forEach(org.jsoup.nodes.Element::remove);
        var section = new org.jsoup.nodes.Element("section")
                .attr("data-mh-reviewed-video", "v1").attr("data-track-section", "reviewed-demonstration")
                .attr("aria-label", "Demonstração do produto")
                .attr("style", "max-width:960px;margin:24px auto;padding:16px;box-sizing:border-box;overflow-wrap:anywhere");
        section.appendElement("h2").text("Veja um exemplo do que você recebe");
        var player = section.appendElement("video").attr("controls", "").attr("playsinline", "")
                .attr("preload", "metadata").attr("data-video-asset-id", video.path("assetId").asText())
                .attr("data-media-sha256", video.path("sha256").asText())
                .attr("aria-label", "Assistir à demonstração do produto")
                .attr("style", "display:block;width:100%;max-height:70vh;background:#f6f2f4;object-fit:contain");
        player.appendElement("source").attr("src", video.path("assetUrl").asText()).attr("type", "video/mp4");
        player.appendText("Seu navegador não conseguiu reproduzir a demonstração. A oferta continua disponível nesta página.");
        section.appendElement("p").text("Exemplo demonstrativo. Assista se quiser e confira as condições da oferta abaixo.");
        var transcript = section.appendElement("details");
        transcript.appendElement("summary").text("Ler a demonstração");
        transcript.appendElement("p").text(video.path("captions").asText());
        var main = document.selectFirst("main");
        var firstSection = main == null ? null : main.selectFirst("section, header");
        if (firstSection != null) firstSection.after(section);
        else if (main != null) main.appendChild(section);
        else document.body().appendChild(section);
        return document.outerHtml();
    }

    /** Confere URL HTTPS absoluta sem credenciais, sem executar tráfego externo. */
    private static boolean secure(String value) {
        try {
            var uri = URI.create(value);
            return "https".equals(uri.getScheme()) && uri.getHost() != null && uri.getUserInfo() == null;
        } catch (IllegalArgumentException ex) {
            org.slf4j.LoggerFactory.getLogger(GeraSalesPageReviewedVideo.class).warn("URL inválida de demonstração selecionada", ex);
            return false;
        }
    }
}
