package com.marketinghub.worker.openai.core.qualityreview;

import java.util.List;

/** Responsabilidade: produzir imagens renderizadas da landing para avaliação visual do Quality Review. */
public interface QualityReviewScreenshotService {

    /** Renderiza o HTML da landing e devolve URLs públicas dos screenshots gerados. */
    List<String> renderScreenshots(QualityReviewInput input);
}
