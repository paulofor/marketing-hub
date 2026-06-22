package com.marketinghub.nichocnaev2.pipeline.candidategenerator;

import static org.assertj.core.api.Assertions.assertThat;

import com.marketinghub.nichocnaev2.pipeline.StageContext;
import com.marketinghub.nichocnaev2.pipeline.StageResult;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Valida o gerador inicial de candidatos MEI/autônomo da v2 sem contaminar dor ou promessa. */
class CandidateGeneratorProcessorTest {
    /** Deve gerar candidatos MEI/autônomo e URLs-semente para a etapa de segurança sem escolher vencedor. */
    @Test
    void generatesMeiCandidatesAndCandidateUrlsForSafetyFilter() {
        CandidateGeneratorProcessor processor = new CandidateGeneratorProcessor();

        StageResult result = processor.process(new StageContext(
                "job-1",
                "stage-1",
                Map.of("cnaeCode", "7319002", "cnaeDescription", "Promoção de vendas")));

        assertThat(result.status()).isEqualTo("BOOTSTRAPPED");
        assertThat(result.output().get("nextStageCode")).isEqualTo("source-safety-filter");
        assertThat((List<?>) result.output().get("candidateUrls")).hasSizeGreaterThanOrEqualTo(3);
        assertThat((List<?>) result.output().get("candidates")).hasSizeGreaterThanOrEqualTo(10);
        assertThat(result.output().get("audienceFocus")).isEqualTo("MEI_AUTONOMO_DONO_OPERADOR");
        assertThat(String.valueOf(result.output().get("candidates"))).contains("painHypotheses=[]", "priorConfidence=LOW");
        assertThat(String.valueOf(result.output().get("candidates")))
                .contains("Promoção de vendas", "MEI_AUTONOMO_DONO_OPERADOR", "EXECUTA_PESSOALMENTE_O_TRABALHO")
                .doesNotContain("CNAE 7319002");
    }

    /** Deve especializar o CNAE de vestuário em recortes de MEI dono-operador, não varejo genérico. */
    @Test
    void specializesFashionRetailAsMeiOwnerOperator() {
        CandidateGeneratorProcessor processor = new CandidateGeneratorProcessor();

        StageResult result = processor.process(new StageContext(
                "job-1",
                "stage-1",
                Map.of("cnaeCode", "4781400", "cnaeDescription", "Comércio varejista de artigos do vestuário e acessórios")));

        String candidates = String.valueOf(result.output().get("candidates"));
        assertThat(candidates)
                .contains("MEI_MODA_WHATSAPP_INSTAGRAM", "MEI_BRECHO_REVENDA_MODA", "MEI_SACOLEIRA_REVENDEDORA")
                .contains("MEI_AUTONOMO_DONO_OPERADOR")
                .doesNotContain("RETAIL_OPERATOR", "STORE_ASSISTANT");
    }
}
