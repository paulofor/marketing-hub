package com.marketinghub.worker.creativereview;

import com.marketinghub.worker.frameworkimage.FrameworkImageStorageClient;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.ScreenshotType;
import com.microsoft.playwright.options.WaitUntilState;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** Responsabilidade: produzir evidências visuais auditáveis do anúncio e da página de destino. */
@Component
public class CreativeReviewVisualEvidenceService {
    private static final Logger log = LoggerFactory.getLogger(CreativeReviewVisualEvidenceService.class);
    private final FrameworkImageStorageClient storageClient;
    private final String chromiumExecutablePath;
    private final long timeoutMillis;

    /** Inicializa a captura visual com o storage público e o Chromium configurado no worker. */
    public CreativeReviewVisualEvidenceService(
            FrameworkImageStorageClient storageClient,
            @Value("${playwright.chromium-executable-path:${CHROMIUM_BIN:}}") String chromiumExecutablePath,
            @Value("${creative-review.worker.screenshot-timeout-ms:120000}") long timeoutMillis) {
        this.storageClient = storageClient;
        this.chromiumExecutablePath = chromiumExecutablePath;
        this.timeoutMillis = timeoutMillis;
    }

    /** Captura a landing em mobile e desktop e quadros representativos quando o anúncio for vídeo. */
    public Evidence capture(Map<String, Object> creative) {
        String destinationUrl = text(creative.get("destinationUrl"));
        if (!isHttpUrl(destinationUrl)) {
            throw new IllegalArgumentException("Anúncio sem URL de destino pública e válida");
        }
        Long creativeId = Long.valueOf(creative.get("creativeId").toString());
        try (Playwright playwright = Playwright.create(); Browser browser = launch(playwright)) {
            List<String> landing = List.of(
                    capturePage(browser, destinationUrl, creativeId, "landing-mobile", 390, 844, true),
                    capturePage(browser, destinationUrl, creativeId, "landing-desktop", 1440, 1000, true));
            List<String> videoFrames = "VIDEO".equalsIgnoreCase(text(creative.get("format")))
                    ? captureVideoFrames(browser, text(creative.get("mediaUrl")), creativeId)
                    : List.of();
            return new Evidence(landing, videoFrames);
        } catch (RuntimeException ex) {
            log.error("Falha ao capturar evidências do anúncio. creativeId={} destinationUrl={}", creativeId, destinationUrl, ex);
            throw ex;
        }
    }

    /** Inicializa o navegador headless respeitando o binário disponível no ambiente. */
    private Browser launch(Playwright playwright) {
        BrowserType.LaunchOptions options = new BrowserType.LaunchOptions()
                .setHeadless(true).setArgs(List.of("--no-sandbox", "--disable-dev-shm-usage", "--disable-gpu"));
        if (StringUtils.hasText(chromiumExecutablePath) && Files.exists(Path.of(chromiumExecutablePath))) {
            options.setExecutablePath(Path.of(chromiumExecutablePath));
        }
        return playwright.chromium().launch(options);
    }

    /** Renderiza uma URL real na viewport solicitada e publica o screenshot integral. */
    private String capturePage(Browser browser, String url, Long creativeId, String label,
                               int width, int height, boolean fullPage) {
        Page page = browser.newPage(new Browser.NewPageOptions().setViewportSize(width, height));
        try {
            page.navigate(url, new Page.NavigateOptions()
                    .setWaitUntil(WaitUntilState.DOMCONTENTLOADED).setTimeout(timeoutMillis));
            return upload(page.screenshot(new Page.ScreenshotOptions().setFullPage(fullPage)
                    .setType(ScreenshotType.JPEG).setQuality(82).setTimeout(timeoutMillis)), creativeId, label);
        } finally {
            page.close();
        }
    }

    /** Extrai três momentos do vídeo para avaliar composição, legibilidade e continuidade estética. */
    private List<String> captureVideoFrames(Browser browser, String mediaUrl, Long creativeId) {
        if (!isHttpUrl(mediaUrl)) throw new IllegalArgumentException("Vídeo sem URL pública e válida");
        Page page = browser.newPage(new Browser.NewPageOptions().setViewportSize(1080, 1080));
        try {
            page.setContent("<html><body style='margin:0;background:#000;display:grid;place-items:center;height:100vh'>"
                    + "<video id='ad' crossorigin='anonymous' muted playsinline style='max-width:100%;max-height:100%' src='"
                    + escapeHtml(mediaUrl) + "'></video></body></html>");
            page.waitForFunction("document.querySelector('#ad').readyState >= 2", null,
                    new Page.WaitForFunctionOptions().setTimeout(timeoutMillis));
            Number duration = (Number) page.evalOnSelector("#ad", "video => video.duration");
            List<String> frames = new ArrayList<>();
            double[] positions = {0.1, 0.5, 0.9};
            for (int index = 0; index < positions.length; index++) {
                double second = Math.max(0, duration.doubleValue() * positions[index]);
                page.evalOnSelector("#ad", "(video, second) => { video.currentTime = second; }", second);
                page.waitForTimeout(500);
                frames.add(upload(page.screenshot(new Page.ScreenshotOptions().setType(ScreenshotType.JPEG)
                        .setQuality(86).setTimeout(timeoutMillis)), creativeId, "video-frame-" + (index + 1)));
            }
            return List.copyOf(frames);
        } finally {
            page.close();
        }
    }

    /** Publica uma evidência visual com nome correlacionado ao criativo. */
    private String upload(byte[] bytes, Long creativeId, String label) {
        return storageClient.upload(bytes, "creative-" + creativeId + "-" + label + ".jpg").publicUrl();
    }

    /** Valida que a evidência aponta para um recurso HTTP acessível pelo navegador. */
    private boolean isHttpUrl(String value) {
        return value.matches("(?i)^https?://.+");
    }

    /** Escapa a URL antes de inseri-la no HTML técnico usado para capturar quadros. */
    private String escapeHtml(String value) {
        return value.replace("&", "&amp;").replace("'", "&#39;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /** Normaliza valores opcionais recebidos do contrato do backend. */
    private String text(Object value) {
        return value == null ? "" : value.toString().trim();
    }

    /** Responsabilidade: transportar screenshots da landing e quadros do vídeo enviados ao modelo. */
    public record Evidence(List<String> landingScreenshots, List<String> videoFrames) {}
}
