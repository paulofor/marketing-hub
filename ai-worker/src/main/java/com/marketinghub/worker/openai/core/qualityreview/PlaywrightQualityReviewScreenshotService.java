package com.marketinghub.worker.openai.core.qualityreview;

import com.marketinghub.worker.frameworkimage.FrameworkImageStorageClient;
import com.marketinghub.worker.openai.core.exception.StageWorkerException;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.ScreenshotType;
import com.microsoft.playwright.options.WaitUntilState;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

/** Responsabilidade: renderizar HTML em Chromium headless, salvar screenshots públicos e expô-los para a OpenAI. */
public class PlaywrightQualityReviewScreenshotService implements QualityReviewScreenshotService {

    private static final Logger log = LoggerFactory.getLogger(PlaywrightQualityReviewScreenshotService.class);
    private static final List<ViewportSpec> VIEWPORTS = List.of(
            new ViewportSpec("mobile", 390, 1200, true),
            new ViewportSpec("desktop", 1440, 1200, false)
    );

    private final FrameworkImageStorageClient storageClient;
    private final QualityReviewWorkerProperties properties;

    /** Inicializa o serviço com storage público e propriedades operacionais do Quality Review. */
    public PlaywrightQualityReviewScreenshotService(
            FrameworkImageStorageClient storageClient,
            QualityReviewWorkerProperties properties
    ) {
        this.storageClient = storageClient;
        this.properties = properties;
    }

    /** Renderiza o HTML final priorizando mobile e publica os screenshots disponíveis no storage. */
    @Override
    public List<QualityReviewScreenshotEvidence> renderScreenshots(QualityReviewInput input) {
        if (input == null || !StringUtils.hasText(input.landingHtml())) {
            throw new StageWorkerException("Quality review cannot render screenshot without landing HTML");
        }
        try (Playwright playwright = Playwright.create(); Browser browser = launchBrowser(playwright)) {
            List<QualityReviewScreenshotEvidence> screenshotUrls = new ArrayList<>();
            for (ViewportSpec viewport : VIEWPORTS) {
                renderAndUploadPrioritized(browser, input, viewport, screenshotUrls);
            }
            return List.copyOf(screenshotUrls);
        } catch (RuntimeException error) {
            log.error(
                    "Falha ao renderizar screenshots do Quality Review [jobId={}, experimentId={}, htmlLength={}]",
                    input != null ? input.idJob() : null,
                    input != null ? input.experimentId() : null,
                    input != null && input.landingHtml() != null ? input.landingHtml().length() : 0,
                    error);
            throw error;
        }
    }

    /** Informa a ordem operacional de captura para testes e auditoria da prioridade mobile. */
    static List<String> capturePriorityViewportNames() {
        return VIEWPORTS.stream().map(ViewportSpec::name).toList();
    }

    /** Executa a captura da viewport e mantém o fluxo com mobile quando uma viewport secundária falha. */
    private void renderAndUploadPrioritized(
            Browser browser,
            QualityReviewInput input,
            ViewportSpec viewport,
            List<QualityReviewScreenshotEvidence> screenshotUrls
    ) {
        try {
            screenshotUrls.add(renderAndUpload(browser, input, viewport));
        } catch (RuntimeException error) {
            if (viewport.required() || screenshotUrls.isEmpty()) {
                throw error;
            }
            log.warn(
                    "Viewport secundária do Quality Review falhou; prosseguindo com screenshots prioritários [jobId={}, experimentId={}, viewport={}, screenshotsDisponiveis={}]",
                    input.idJob(),
                    input.experimentId(),
                    viewport.name(),
                    screenshotUrls.size(),
                    error);
        }
    }

    /** Inicializa Chromium headless usando o binário configurado quando disponível. */
    private Browser launchBrowser(Playwright playwright) {
        BrowserType.LaunchOptions options = new BrowserType.LaunchOptions()
                .setHeadless(true)
                .setArgs(List.of("--no-sandbox", "--disable-dev-shm-usage", "--disable-gpu"));
        String executablePath = properties.chromiumExecutablePath();
        if (StringUtils.hasText(executablePath) && Files.exists(Path.of(executablePath))) {
            options.setExecutablePath(Path.of(executablePath));
        }
        return playwright.chromium().launch(options);
    }

    /** Renderiza uma viewport específica, captura JPEG full-page sem recortar a landing e publica no storage. */
    private QualityReviewScreenshotEvidence renderAndUpload(Browser browser, QualityReviewInput input, ViewportSpec viewport) {
        Page page = browser.newPage(new Browser.NewPageOptions()
                .setViewportSize(viewport.width(), viewport.height())
                .setDeviceScaleFactor(1));
        try {
            page.setContent(input.landingHtml(), new Page.SetContentOptions()
                    .setWaitUntil(WaitUntilState.DOMCONTENTLOADED)
                    .setTimeout(properties.screenshotTimeout().toMillis()));
            waitForNetworkIdle(page, input, viewport);
            byte[] screenshot = page.screenshot(new Page.ScreenshotOptions()
                    .setFullPage(true)
                    .setTimeout(properties.screenshotTimeout().toMillis())
                    .setType(ScreenshotType.JPEG)
                    .setQuality(82));
            FrameworkImageStorageClient.UploadedFrameworkImage uploaded = storageClient.upload(
                    screenshot,
                    buildScreenshotFilename(input, viewport));
            log.info(
                    "Screenshot full-page do Quality Review publicado [jobId={}, experimentId={}, viewport={}, screenshotTimeoutMs={}, publicUrl={}, bytes={}]",
                    input.idJob(),
                    input.experimentId(),
                    viewport.name(),
                    properties.screenshotTimeout().toMillis(),
                    uploaded.publicUrl(),
                    screenshot.length);
            return new QualityReviewScreenshotEvidence(viewport.name(), uploaded.publicUrl(), sha256Hex(screenshot), screenshot.length);
        } finally {
            page.close();
        }
    }

    /** Aguarda estabilidade de rede sem bloquear a revisão quando scripts externos mantêm conexões abertas. */
    private void waitForNetworkIdle(Page page, QualityReviewInput input, ViewportSpec viewport) {
        try {
            page.waitForLoadState(LoadState.NETWORKIDLE, new Page.WaitForLoadStateOptions().setTimeout(5000));
        } catch (PlaywrightException error) {
            log.warn(
                    "Network idle não atingido antes do screenshot do Quality Review; prosseguindo com DOM renderizado [jobId={}, viewport={}]",
                    input.idJob(),
                    viewport.name(),
                    error);
        }
    }


    /** Calcula o hash SHA-256 em hexadecimal para identificar screenshots duplicados com URLs diferentes. */
    private String sha256Hex(byte[] content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content);
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte value : hash) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException error) {
            throw new StageWorkerException("SHA-256 algorithm unavailable for quality-review screenshot audit", error);
        }
    }

    /** Gera nome estável para o screenshot publicado no storage. */
    private String buildScreenshotFilename(QualityReviewInput input, ViewportSpec viewport) {
        String jobId = StringUtils.hasText(input.idJob()) ? input.idJob() : "quality-review";
        return jobId + "-quality-review-" + viewport.name() + ".jpg";
    }

    /** Responsabilidade: representar uma viewport de screenshot da landing e sua prioridade operacional. */
    private record ViewportSpec(String name, int width, int height, boolean required) {
        /** Preserva nome, dimensões e obrigatoriedade da viewport para renderização do browser. */
        private ViewportSpec {
        }
    }
}
