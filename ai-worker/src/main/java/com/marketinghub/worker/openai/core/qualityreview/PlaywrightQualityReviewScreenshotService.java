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
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

/** Responsabilidade: renderizar HTML em Chromium headless, salvar screenshots públicos e expô-los para a OpenAI. */
public class PlaywrightQualityReviewScreenshotService implements QualityReviewScreenshotService {

    private static final Logger log = LoggerFactory.getLogger(PlaywrightQualityReviewScreenshotService.class);
    private static final List<ViewportSpec> VIEWPORTS = List.of(
            new ViewportSpec("desktop", 1440, 1200),
            new ViewportSpec("mobile", 390, 1200)
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

    /** Renderiza o HTML final em viewports desktop e mobile e publica os screenshots no storage. */
    @Override
    public List<String> renderScreenshots(QualityReviewInput input) {
        if (input == null || !StringUtils.hasText(input.landingHtml())) {
            throw new StageWorkerException("Quality review cannot render screenshot without landing HTML");
        }
        try (Playwright playwright = Playwright.create(); Browser browser = launchBrowser(playwright)) {
            List<String> screenshotUrls = new ArrayList<>();
            for (ViewportSpec viewport : VIEWPORTS) {
                screenshotUrls.add(renderAndUpload(browser, input, viewport));
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

    /** Renderiza uma viewport específica, captura JPEG full-page e publica no storage. */
    private String renderAndUpload(Browser browser, QualityReviewInput input, ViewportSpec viewport) {
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
                    .setType(ScreenshotType.JPEG)
                    .setQuality(82));
            FrameworkImageStorageClient.UploadedFrameworkImage uploaded = storageClient.upload(
                    screenshot,
                    buildScreenshotFilename(input, viewport));
            log.info(
                    "Screenshot do Quality Review publicado [jobId={}, experimentId={}, viewport={}, publicUrl={}, bytes={}]",
                    input.idJob(),
                    input.experimentId(),
                    viewport.name(),
                    uploaded.publicUrl(),
                    screenshot.length);
            return uploaded.publicUrl();
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

    /** Gera nome estável para o screenshot publicado no storage. */
    private String buildScreenshotFilename(QualityReviewInput input, ViewportSpec viewport) {
        String jobId = StringUtils.hasText(input.idJob()) ? input.idJob() : "quality-review";
        return jobId + "-quality-review-" + viewport.name() + ".jpg";
    }

    /** Responsabilidade: representar uma viewport de screenshot da landing. */
    private record ViewportSpec(String name, int width, int height) {
        /** Preserva nome e dimensões da viewport para renderização do browser. */
        private ViewportSpec {
        }
    }
}
