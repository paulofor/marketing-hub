package com.marketinghub.worker.openai.core.qualityreview;

import com.marketinghub.worker.openai.core.model.OpenAiResult;
import com.marketinghub.worker.openai.core.model.StageExecution;
import com.marketinghub.worker.openai.core.port.StageResponseHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Responsabilidade: registrar o resultado operacional das execuções da revisão visual. */
public class QualityReviewResponseHandler implements StageResponseHandler<QualityReviewInput, QualityReviewOutput> {

    private static final Logger log = LoggerFactory.getLogger(QualityReviewResponseHandler.class);

    /** Registra os dados resumidos de sucesso após a resposta validada da revisão visual. */
    @Override
    public void handleSuccess(StageExecution<QualityReviewInput> execution, OpenAiResult<QualityReviewOutput> result) {
        log.info(
                "Quality review execution completed. idJob={}, experimentId={}, imageCount={}, inputTokens={}, outputTokens={}, costUsd={}",
                execution.idJob(),
                execution.aggregateId(),
                execution.input().imageUrls().size(),
                result.inputTokens(),
                result.outputTokens(),
                result.costUsd());
    }

    /** Registra a falha completa da revisão visual preservando stack trace para diagnóstico. */
    @Override
    public void handleFailure(StageExecution<QualityReviewInput> execution, Throwable error) {
        log.error("Quality review execution failed. idJob={}, experimentId={}", execution.idJob(), execution.aggregateId(), error);
    }
}
