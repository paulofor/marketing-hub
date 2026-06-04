package com.marketinghub.nichocnae.signalextractor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.marketinghub.oprmcoletormei.marketimport.config.OprmMarketImportCollectorProperties;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

/** Responsabilidade: validar o contrato montado pelo cliente backend da etapa cinco. */
class SignalExtractorBackendClientTest {

    /** Deve montar payload de conclusão sem JSON textual ou metadado técnico fora do contrato. */
    @Test
    void shouldBuildCompletionRequestWithContractFieldsOnly() {
        SignalExtractorBackendClient client = new SignalExtractorBackendClient(
                new OprmMarketImportCollectorProperties("http://backend", "/tmp"), mock(RestClient.class));
        SignalExtractorPending pending = pending();
        List<ExtractedSignal> signals = List.of(new ExtractedSignal("ROUTINE_TASK", "Confirmar agenda", "agenda", 80));

        SignalExtractorCompletionRequest request = client.toCompletionRequest(pending, signals);

        assertThat(request.researchCycleId()).isEqualTo(1001L);
        assertThat(request.sourceCandidateId()).isEqualTo(301L);
        assertThat(request.sourceDomain()).isEqualTo("exemplo.com");
        assertThat(request.createdBy()).isEqualTo("oprmSignalExtractor");
        assertThat(request.signals()).hasSize(1);
        assertThat(request.signals().getFirst().signalText()).doesNotContain("{").doesNotContain("}");
    }

    /** Cria uma pendência mínima para validar o contrato do cliente backend. */
    private SignalExtractorPending pending() {
        return new SignalExtractorPending(
                9001L,
                1001L,
                301L,
                "https://exemplo.com/fonte",
                "exemplo.com",
                "Como organizar agenda",
                "PUBLIC_CONTENT",
                "Resumo público da fonte",
                "Trecho curto permitido para extração de sinais.",
                Instant.parse("2026-06-04T00:00:00Z"),
                "COMPLETED",
                200,
                "SHORT_EXCERPT_ONLY",
                "PUBLIC_SNIPPET",
                "PENDING",
                Instant.parse("2026-06-04T00:00:00Z"));
    }
}
