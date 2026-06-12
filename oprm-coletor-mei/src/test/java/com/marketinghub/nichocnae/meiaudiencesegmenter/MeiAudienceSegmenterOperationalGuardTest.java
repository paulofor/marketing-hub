package com.marketinghub.nichocnae.meiaudiencesegmenter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketinghub.nichocnae.pipeline.ArtifactStore;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Responsabilidade: validar a barreira operacional da etapa MEI antes de buscar ou processar pendências. */
@ExtendWith(MockitoExtension.class)
class MeiAudienceSegmenterOperationalGuardTest {
    @Mock MeiAudienceSegmenterBackendClient backendClient;
    @Mock MeiAudienceSegmenterProcessor processor;
    @Mock ArtifactStore artifactStore;

    /** Confirma que a ausência da chave dedicada e do fallback bloqueia a etapa antes de buscar pendências. */
    @Test
    void shouldBlockPendingLookupWhenDedicatedAndFallbackOpenAiKeysAreMissing() {
        MeiAudienceSegmenterOperationalGuard guard = new MeiAudienceSegmenterOperationalGuard(propertiesWithApiKey(""));
        MeiAudienceSegmenterService service = new MeiAudienceSegmenterService(backendClient, processor, artifactStore, guard);

        assertThatThrownBy(service::listPendingCycles)
                .isInstanceOf(MeiAudienceSegmenterOperationalException.class)
                .hasMessageContaining("OPRM_MEI_AUDIENCE_SEGMENTER_OPENAI_API_KEY")
                .hasMessageContaining("OPENAI_API_KEY")
                .hasMessageContaining("module=oprm-coletor-mei")
                .hasMessageContaining("operation=mei-audience-segmenter")
                .hasMessageContaining("researchCycleId=n/a");
        verify(backendClient, never()).listPendingCycles();
    }

    /** Confirma que o valor já resolvido pelo fallback OPENAI_API_KEY libera a busca de pendências. */
    @Test
    void shouldAllowPendingLookupWhenOpenAiFallbackApiKeyIsConfigured() {
        MeiAudienceSegmenterOperationalGuard guard = new MeiAudienceSegmenterOperationalGuard(propertiesWithApiKey("fallback-openai-key"));
        MeiAudienceSegmenterService service = new MeiAudienceSegmenterService(backendClient, processor, artifactStore, guard);
        when(backendClient.listPendingCycles()).thenReturn(List.of(pending()));

        List<MeiAudienceSegmenterPending> pendingCycles = service.listPendingCycles();

        assertThat(pendingCycles).hasSize(1);
        verify(backendClient).listPendingCycles();
    }

    /** Confirma que a falha operacional com ciclo conhecido é reportada ao backend sem mascarar a causa-raiz. */
    @Test
    void shouldNotifyBackendWithOperationalErrorWhenKeyIsMissingAfterPendingWasLoaded() {
        MeiAudienceSegmenterOperationalGuard guard = mock(MeiAudienceSegmenterOperationalGuard.class);
        MeiAudienceSegmenterService service = new MeiAudienceSegmenterService(backendClient, processor, artifactStore, guard);
        MeiAudienceSegmenterPending pending = pending();
        MeiAudienceSegmenterOperationalException error = new MeiAudienceSegmenterOperationalException(
                "Falha operacional na etapa mei-audience-segmenter: variável OPRM_MEI_AUDIENCE_SEGMENTER_OPENAI_API_KEY ausente e fallback OPENAI_API_KEY indisponível. module=oprm-coletor-mei, operation=mei-audience-segmenter, researchCycleId=1001.");
        when(backendClient.listPendingCycles()).thenReturn(List.of(pending));
        doAnswer(invocation -> {
                    Long researchCycleId = invocation.getArgument(0);
                    if (Long.valueOf(1001L).equals(researchCycleId)) {
                        throw error;
                    }
                    return null;
                })
                .when(guard)
                .assertReadyForExecution(any());

        assertThatThrownBy(() -> service.processPending("test"))
                .isSameAs(error)
                .hasMessageContaining("researchCycleId=1001");
        verify(backendClient).failStageExecution(pending, error);
    }

    /** Cria propriedades simulando o valor final já resolvido pelo application.yml. */
    private MeiAudienceSegmenterOpenAiProperties propertiesWithApiKey(String apiKey) {
        return new MeiAudienceSegmenterOpenAiProperties(
                "https://api.openai.com/v1",
                apiKey,
                "",
                "gpt-test",
                "OPRM_MEI_AUDIENCE_SEGMENTER_OPENAI_API_KEY",
                "OPENAI_API_KEY");
    }

    /** Cria uma pendência mínima para testar a barreira operacional. */
    private MeiAudienceSegmenterPending pending() {
        return new MeiAudienceSegmenterPending(
                1001L,
                2002L,
                3003L,
                "9602501",
                "Cabeleireiros, manicure e pedicure",
                "Serviços de beleza",
                "Beleza MEI",
                "Rotina operacional",
                "Captação por indicação",
                "Instagram e WhatsApp",
                "Agenda cheia e retrabalho",
                "Medo de perder clientes",
                "Ter agenda previsível",
                "Ficar sem faturamento",
                "Linguagem direta",
                "Dores comprovadas",
                "Resultados desejados",
                "Evidências recentes",
                "example.com",
                80,
                75,
                70,
                10,
                Instant.parse("2026-06-12T00:00:00Z"),
                List.of(),
                List.of());
    }
}
