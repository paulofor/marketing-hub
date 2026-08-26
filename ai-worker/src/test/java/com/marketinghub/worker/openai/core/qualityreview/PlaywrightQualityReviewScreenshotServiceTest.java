package com.marketinghub.worker.openai.core.qualityreview;

import static org.assertj.core.api.Assertions.assertThat;

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
}
