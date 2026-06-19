package com.marketinghub.nichocnaev2.pipeline.candidategenerator;

import static org.assertj.core.api.Assertions.assertThat;

import com.marketinghub.nichocnaev2.pipeline.StageContext;
import com.marketinghub.nichocnaev2.pipeline.StageResult;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Valida o gerador inicial de candidatos neutros da v2 sem contaminar dor, canal ou promessa. */
class CandidateGeneratorProcessorTest {
    /** Deve gerar candidatos neutros e URLs-semente para a etapa de segurança sem escolher vencedor. */
    @Test
    void generatesNeutralCandidatesAndCandidateUrlsForSafetyFilter() {
        CandidateGeneratorProcessor processor = new CandidateGeneratorProcessor();

        StageResult result = processor.process(new StageContext("job-1", "stage-1", Map.of("cnaeCode", "4781400")));

        assertThat(result.status()).isEqualTo("BOOTSTRAPPED");
        assertThat(result.output().get("nextStageCode")).isEqualTo("source-safety-filter");
        assertThat((List<?>) result.output().get("candidateUrls")).hasSizeGreaterThanOrEqualTo(3);
        assertThat((List<?>) result.output().get("candidates")).hasSizeBetween(4, 6);
        assertThat(String.valueOf(result.output().get("candidates"))).contains("painHypotheses=[]", "priorConfidence=LOW");
    }
}
