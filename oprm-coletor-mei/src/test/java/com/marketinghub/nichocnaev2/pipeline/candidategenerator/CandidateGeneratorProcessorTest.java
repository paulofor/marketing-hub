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
        assertThat(result.output().get("audienceFocus")).isEqualTo("INSTAGRAM_BROAD_MEI_AUTONOMO");
        assertThat(result.output().get("distributionChannel")).isEqualTo("INSTAGRAM");
        assertThat(result.output().get("hypothesisPipelineInputRole")).isEqualTo("AUDIENCE_ROUTINE_LANGUAGE_INPUT");
        assertThat(result.output().get("commercialBoundary")).isEqualTo("NAO_GERAR_DOR_RESULTADO_OFERTA");
        assertThat(String.valueOf(result.output().get("candidates")))
                .contains("priorConfidence=LOW", "AUDIENCE_ROUTINE_LANGUAGE_INPUT")
                .doesNotContain("painHypotheses");
        assertThat(String.valueOf(result.output().get("candidates")))
                .contains("Promoção de vendas", "INSTAGRAM_BROAD_MEI_AUTONOMO", "CRIATIVO_FILTRA_PUBLICO_AMPLO")
                .doesNotContain("CNAE 7319002");
    }

    /** Deve manter padrão amplo para Instagram em todos os CNAEs, sem especialização estreita por atividade. */
    @Test
    void keepsBroadInstagramPatternForAnyCnae() {
        CandidateGeneratorProcessor processor = new CandidateGeneratorProcessor();

        StageResult result = processor.process(new StageContext(
                "job-1",
                "stage-1",
                Map.of("cnaeCode", "4781400", "cnaeDescription", "Comércio varejista de artigos do vestuário e acessórios")));

        String candidates = String.valueOf(result.output().get("candidates"));
        assertThat(candidates)
                .contains("RENDA_COM_TRABALHO_PROPRIO", "CLIENTES_PELO_WHATSAPP", "AGENDA_VAZIA_OU_OSCILANTE")
                .contains("INSTAGRAM_BROAD_MEI_AUTONOMO", "BROAD_AUDIENCE_CREATIVE_FILTER")
                .contains("Comércio varejista de artigos do vestuário e acessórios")
                .doesNotContain("MEI_MODA_WHATSAPP_INSTAGRAM", "RETAIL_OPERATOR", "STORE_ASSISTANT");
    }
}
