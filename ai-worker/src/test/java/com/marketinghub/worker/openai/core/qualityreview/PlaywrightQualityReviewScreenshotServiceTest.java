package com.marketinghub.worker.openai.core.qualityreview;

import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Responsabilidade: validar a prioridade operacional dos screenshots visuais do Quality Review. */
class PlaywrightQualityReviewScreenshotServiceTest {

    /** Deve capturar mobile antes de desktop para preservar a evidência visual prioritária. */
    @Test
    void capturePriorityViewportNamesShouldStartWithMobile() {
        assertThat(PlaywrightQualityReviewScreenshotService.capturePriorityViewportNames())
                .containsExactly("mobile", "desktop");
    }

    /** Deve selecionar a seção com mais imagens para complementar a visão full-page. */
    @Test
    void selectProofSectionIndexShouldChooseDensestImageSection() {
        assertThat(PlaywrightQualityReviewScreenshotService.selectProofSectionIndex(List.of(1, 3, 2)))
                .isEqualTo(1);
    }

    /** Não deve criar recorte complementar quando nenhuma seção reúne múltiplas provas. */
    @Test
    void selectProofSectionIndexShouldIgnoreSectionsWithoutMultipleImages() {
        assertThat(PlaywrightQualityReviewScreenshotService.selectProofSectionIndex(List.of(0, 1, 1)))
                .isEqualTo(-1);
    }

    /** Deve tornar a prova eager e aguardar seus pixels mesmo quando o HTML pediu decode assíncrono. */
    @Test
    void imageReadinessScriptsShouldWaitForConcretePixels() {
        try (Playwright playwright = Playwright.create();
                Browser browser = playwright.chromium().launch(
                        new BrowserType.LaunchOptions()
                                .setHeadless(true)
                                .setExecutablePath(Path.of(resolveChromiumExecutable()))
                                .setArgs(List.of("--no-sandbox", "--disable-dev-shm-usage")))) {
            Page page = browser.newPage();
            page.setContent(
                    "<img id='proof' loading='lazy' decoding='async' "
                            + "src='data:image/svg+xml,%3Csvg xmlns=%22http://www.w3.org/2000/svg%22 width=%2210%22 height=%2210%22%3E%3Crect width=%2210%22 height=%2210%22 fill=%22green%22/%3E%3C/svg%3E'>");

            page.evaluate(PlaywrightQualityReviewScreenshotService.forceEagerImagesScript());
            var ready = page.waitForFunction(
                    PlaywrightQualityReviewScreenshotService.loadedImagesExpression(),
                    null,
                    new Page.WaitForFunctionOptions().setTimeout(5_000));
            ready.dispose();
            page.evaluate(PlaywrightQualityReviewScreenshotService.decodedImagesScript());

            assertThat(page.evaluate("() => document.querySelector('#proof').loading"))
                    .isEqualTo("eager");
            assertThat(page.evaluate("() => document.querySelector('#proof').naturalWidth"))
                    .isEqualTo(10);
        }
    }

    /** Usa o Chromium fornecido pela sandbox sem depender de download durante os testes. */
    private String resolveChromiumExecutable() {
        String configured = System.getenv("PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH");
        return configured == null || configured.isBlank() ? "/usr/bin/chromium" : configured;
    }
}
