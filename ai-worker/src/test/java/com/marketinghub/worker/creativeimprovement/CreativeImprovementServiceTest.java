package com.marketinghub.worker.creativeimprovement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketinghub.worker.creative.CreativeImageClient;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Responsabilidade: validar a materialização técnica de revisões visuais. */
class CreativeImprovementServiceTest {

    /** Exige requisitos, proibições e critérios no prompt enviado ao gerador. */
    @Test
    void buildsImagePromptWithEveryStructuredReviewConstraint() {
        CreativeImprovementBackendClient backend = mock(CreativeImprovementBackendClient.class);
        CreativeImageClient imageClient = mock(CreativeImageClient.class);
        CreativeImprovementService service = new CreativeImprovementService(backend, imageClient);
        when(backend.listPending(3)).thenReturn(List.of(Map.of(
                "creativeId", 88L,
                "revisedImagePrompt", "Crie anúncio premium para manicures.",
                "mandatoryVisualRequirements", List.of("Headline Agenda Cheia legível", "CTA Saiba mais legível"),
                "forbiddenVisualElements", List.of("Texto simulado", "Botão vazio"),
                "visualAcceptanceCriteria", List.of("Headline pode ser lida em tela mobile"))));
        when(imageClient.generateImage(any(), any(), any())).thenReturn("https://cdn.test/revision.png");

        CreativeImprovementService.Summary result = service.processPending(3);

        assertThat(result.success()).isEqualTo(1);
        verify(imageClient).generateImage(
                contains("1. Headline Agenda Cheia legível"),
                eq("Revisão visual solicitada pelo backend"),
                eq("creative-improvement-88"));
        verify(imageClient).generateImage(contains("ELEMENTOS PROIBIDOS"), any(), any());
        verify(imageClient).generateImage(contains("CRITÉRIOS DE ACEITAÇÃO"), any(), any());
        verify(backend).report(eq(88L), any());
    }

    /** Bloqueia consumo de imagem quando o parecer não possui contrato verificável. */
    @Test
    void rejectsIncompleteCorrectionBeforeCallingImageModel() {
        CreativeImprovementBackendClient backend = mock(CreativeImprovementBackendClient.class);
        CreativeImageClient imageClient = mock(CreativeImageClient.class);
        CreativeImprovementService service = new CreativeImprovementService(backend, imageClient);
        when(backend.listPending(1)).thenReturn(List.of(Map.of(
                "creativeId", 89L,
                "revisedImagePrompt", "Melhore o visual")));

        CreativeImprovementService.Summary result = service.processPending(1);

        assertThat(result.failed()).isEqualTo(1);
        verify(backend).report(eq(89L), any());
        verify(imageClient, org.mockito.Mockito.never()).generateImage(any(), any(), any());
    }
}
