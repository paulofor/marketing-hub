package com.marketinghub.worker.openai.core.qualityreview;

import java.util.List;

/** Responsabilidade: produzir imagens renderizadas da landing para avaliação visual do Quality Review. */
public interface QualityReviewScreenshotService {

    /** Renderiza o HTML da landing e devolve evidências públicas e hashes dos screenshots gerados. */
    List<QualityReviewScreenshotEvidence> renderScreenshots(QualityReviewInput input);
}
