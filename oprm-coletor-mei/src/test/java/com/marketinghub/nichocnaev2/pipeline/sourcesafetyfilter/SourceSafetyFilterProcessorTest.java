package com.marketinghub.nichocnaev2.pipeline.sourcesafetyfilter;

import static org.assertj.core.api.Assertions.assertThat;

import com.marketinghub.nichocnaev2.pipeline.StageContext;
import com.marketinghub.nichocnaev2.pipeline.StageResult;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Valida o filtro determinístico de segurança da etapa 2 do pipeline NichoCNAE v2. */
class SourceSafetyFilterProcessorTest {
    /** Deve bloquear domínio proibido, remover tracking e deduplicar URL canônica antes de qualquer fetch. */
    @Test
    void filtersBlockedDomainsTrackingParametersAndDuplicateCanonicalUrls() {
        SourceSafetyFilterProcessor processor = new SourceSafetyFilterProcessor();
        StageContext context = new StageContext(
                "job-1",
                "stage-2",
                Map.of("candidateUrls", List.of(
                        "https://www.gov.br/mei/rotina?utm_source=ads&id=10#x",
                        "https://www.gov.br/mei/rotina?id=10",
                        "https://adult.example/oferta")));

        StageResult result = processor.process(context);

        assertThat(result.status()).isEqualTo("ALLOW");
        assertThat(result.output().get("allowedUrlCount")).isEqualTo(1);
        assertThat(result.output().get("rejectedUrlCount")).isEqualTo(2);
        assertThat(result.output().toString()).contains("https://www.gov.br/mei/rotina?id=10");
        assertThat(result.output().toString()).contains("HARD_BLOCKED_DOMAIN", "DUPLICATE");
    }

    /** Deve registrar hard reject global quando todas as URLs candidatas forem inseguras. */
    @Test
    void returnsHardRejectWhenEveryCandidateUrlIsUnsafe() {
        SourceSafetyFilterProcessor processor = new SourceSafetyFilterProcessor();
        StageContext context = new StageContext(
                "job-1",
                "stage-2",
                Map.of("candidateUrls", List.of("ftp://example.com/file", "https://casino.example/aposta")));

        StageResult result = processor.process(context);

        assertThat(result.status()).isEqualTo("HARD_REJECT");
        assertThat(result.output().get("allowedUrlCount")).isEqualTo(0);
        assertThat(result.output().get("rejectedUrlCount")).isEqualTo(2);
    }
}
