package com.marketinghub.nichocnaev2.pipeline.candidategenerator;

import com.marketinghub.nichocnaev2.pipeline.StageContext;
import com.marketinghub.nichocnaev2.pipeline.StageProcessor;
import com.marketinghub.nichocnaev2.pipeline.StageResult;
import java.util.List;
import java.util.Map;

/** Executa a etapa plugável que cria candidatos neutros para pesquisa NichoCNAE versão 2. */
public final class CandidateGeneratorProcessor implements StageProcessor {
    /** Produz uma saída mínima de bootstrap sem escolher vencedor nem declarar dor validada. */
    @Override
    public StageResult process(StageContext context) {
        return new StageResult("BOOTSTRAPPED", Map.of("stage", "candidate-generator"), List.of());
    }
}
