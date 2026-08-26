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
            screenshotUrls.addAll(renderAndUpload(browser, input, viewport));
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

    /** Renderiza uma viewport e publica a página inteira junto da seção visual com mais provas. */
    private List<QualityReviewScreenshotEvidence> renderAndUpload(
            Browser browser,
            QualityReviewInput input,
            ViewportSpec viewport
    ) {
        Page page = browser.newPage(new Browser.NewPageOptions()
                .setViewportSize(viewport.width(), viewport.height())
                .setDeviceScaleFactor(1));
        try {
            page.setContent(input.landingHtml(), new Page.SetContentOptions()
                    .setWaitUntil(WaitUntilState.DOMCONTENTLOADED)
                    .setTimeout(properties.screenshotTimeout().toMillis()));
            waitForNetworkIdle(page, input, viewport);
            List<QualityReviewScreenshotEvidence> evidence = new ArrayList<>();
            byte[] fullPageScreenshot = page.screenshot(new Page.ScreenshotOptions()
                    .setFullPage(true)
                    .setTimeout(properties.screenshotTimeout().toMillis())
                    .setType(ScreenshotType.JPEG)
                    .setQuality(82));
            evidence.add(upload(input, viewport, "full-page", fullPageScreenshot));
            log.info(
                    "Screenshot full-page do Quality Review publicado [jobId={}, experimentId={}, viewport={}, screenshotTimeoutMs={}, publicUrl={}, bytes={}]",
                    input.idJob(),
                    input.experimentId(),
                    viewport.name(),
                    properties.screenshotTimeout().toMillis(),
                    evidence.getFirst().publicUrl(),
                    fullPageScreenshot.length);
            captureProofSection(page, input, viewport, evidence);
            return List.copyOf(evidence);
        } finally {
            page.close();
        }
    }

    /** Captura a seção com maior concentração de imagens para preservar a legibilidade das provas. */
    private void captureProofSection(
            Page page,
            QualityReviewInput input,
            ViewportSpec viewport,
            List<QualityReviewScreenshotEvidence> evidence
    ) {
        try {
            var sections = page.locator("main section");
            List<Integer> imageCounts = new ArrayList<>();
            for (int index = 0; index < sections.count(); index++) {
                imageCounts.add(sections.nth(index).locator("img").count());
            }
            int proofSectionIndex = selectProofSectionIndex(imageCounts);
            if (proofSectionIndex < 0) {
                return;
            }
            byte[] proofScreenshot = sections.nth(proofSectionIndex).screenshot(
                    new com.microsoft.playwright.Locator.ScreenshotOptions()
                            .setTimeout(properties.screenshotTimeout().toMillis())
                            .setType(ScreenshotType.JPEG)
                            .setQuality(88));
            QualityReviewScreenshotEvidence uploaded = upload(
                    input,
                    viewport,
                    "proof-section",
                    proofScreenshot);
            evidence.add(uploaded);
            log.info(
                    "Screenshot focado nas provas do Quality Review publicado [jobId={}, experimentId={}, viewport={}, sectionIndex={}, publicUrl={}, bytes={}]",
                    input.idJob(),
                    input.experimentId(),
                    viewport.name(),
                    proofSectionIndex,
                    uploaded.publicUrl(),
                    proofScreenshot.length);
        } catch (RuntimeException error) {
            log.warn(
                    "Captura complementar das provas falhou; preservando screenshot full-page [jobId={}, experimentId={}, viewport={}, evidenciasDisponiveis={}]",
                    input.idJob(),
                    input.experimentId(),
                    viewport.name(),
                    evidence.size(),
                    error);
        }
    }

    /** Seleciona a primeira seção com a maior quantidade de imagens, exigindo ao menos duas provas. */
    static int selectProofSectionIndex(List<Integer> imageCounts) {
        int selectedIndex = -1;
        int highestCount = 1;
        for (int index = 0; index < imageCounts.size(); index++) {
            int count = imageCounts.get(index) != null ? imageCounts.get(index) : 0;
            if (count > highestCount) {
                selectedIndex = index;
                highestCount = count;
            }
        }
        return selectedIndex;
    }

    /** Publica uma evidência visual com variante explícita e hash auditável. */
    private QualityReviewScreenshotEvidence upload(
            QualityReviewInput input,
            ViewportSpec viewport,
            String variant,
            byte[] screenshot
    ) {
        FrameworkImageStorageClient.UploadedFrameworkImage uploaded = storageClient.upload(
                screenshot,
                buildScreenshotFilename(input, viewport, variant));
        return new QualityReviewScreenshotEvidence(
                viewport.name() + "-" + variant,
                uploaded.publicUrl(),
                sha256Hex(screenshot),
                screenshot.length);
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

    /** Gera nome estável para cada variante de screenshot publicada no storage. */
    private String buildScreenshotFilename(QualityReviewInput input, ViewportSpec viewport, String variant) {
        String jobId = StringUtils.hasText(input.idJob()) ? input.idJob() : "quality-review";
        return jobId + "-quality-review-" + viewport.name() + "-" + variant + ".jpg";
    }

    /** Responsabilidade: representar uma viewport de screenshot da landing e sua prioridade operacional. */
    private record ViewportSpec(String name, int width, int height, boolean required) {
        /** Preserva nome, dimensões e obrigatoriedade da viewport para renderização do browser. */
        private ViewportSpec {
        }
    }
}
