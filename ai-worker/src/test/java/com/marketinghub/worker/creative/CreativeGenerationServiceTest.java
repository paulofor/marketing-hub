package com.marketinghub.worker.creative;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.creative.CreativeStatus;
import com.marketinghub.creative.dto.CreateCreativeRequest;
import com.marketinghub.experiment.CreativeGenerationMode;
import com.marketinghub.experiment.CreativeGenerationStatus;
import com.marketinghub.experiment.dto.ExperimentDto;
import com.marketinghub.worker.creative.CreativeGenerationBackendClient.CreativeTaskExecutionAudit;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Valida a reativação do processamento de criativos pendentes pelo AI Worker.
 */
class CreativeGenerationServiceTest {

    /**
     * Garante que Íris consome a fila, reutiliza a prova aprovada e conclui a pendência.
     */
    @Test
    void shouldProcessDefaultPendingCreativeGeneration() {
        CreativeGenerationBackendClient backendClient = mock(CreativeGenerationBackendClient.class);
        CreativeChatGptClient textClient = mock(CreativeChatGptClient.class);
        CreativeImageClient imageClient = mock(CreativeImageClient.class);
        CreativeGenerationService service =
                new CreativeGenerationService(backendClient, textClient, new ObjectMapper());
        ExperimentDto experiment = pendingExperiment();
        CreateCreativeRequest generated = new CreateCreativeRequest();
        generated.setHeadline("Headline");
        generated.setPrimaryText("Texto principal");
        generated.setCta("Gerar minha amostra personalizada");
        generated.setStatus(CreativeStatus.DRAFT);

        when(backendClient.listPending(5)).thenReturn(List.of(experiment));
        when(textClient.generateCreatives(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(1)))
                .thenReturn(generation(List.of(generated)));
        CreativeGenerationService.ProcessingSummary summary = service.processPending(5);

        assertThat(summary.total()).isEqualTo(1);
        assertThat(summary.succeeded()).isEqualTo(1);
        assertThat(summary.failed()).isZero();
        verify(backendClient).markStarted(49L);
        ArgumentCaptor<CreateCreativeRequest> captor = ArgumentCaptor.forClass(CreateCreativeRequest.class);
        verify(backendClient).createCreative(org.mockito.ArgumentMatchers.eq(49L), captor.capture());
        assertThat(captor.getValue().getImageUrl()).isEqualTo("/assets/product-deliverable.png");
        assertThat(captor.getValue().getCta()).isEqualTo("LEARN_MORE");
        verify(imageClient, never()).generateImage(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyString());
        ArgumentCaptor<CreativeTaskExecutionAudit> auditCaptor =
                ArgumentCaptor.forClass(CreativeTaskExecutionAudit.class);
        verify(backendClient).markCompleted(org.mockito.ArgumentMatchers.eq(49L), auditCaptor.capture());
        assertThat(auditCaptor.getValue().executionMode()).isEqualTo("MODEL");
        assertThat(auditCaptor.getValue().reasoningEffort()).isEqualTo("medium");
        assertThat(auditCaptor.getValue().promptSent())
                .isEqualTo("Núcleo de Íris.\n\nGere a copy.");
        assertThat(auditCaptor.getValue().agentPromptPart()).isEqualTo("Núcleo de Íris.");
        assertThat(auditCaptor.getValue().activityPromptPart()).isEqualTo("Gere a copy.");
    }

    /** Garante que copy acima dos limites Meta seja bloqueada sem truncamento silencioso. */
    @Test
    void shouldRejectGeneratedCreativeTextAboveMetaLimits() {
        CreativeGenerationBackendClient backendClient = mock(CreativeGenerationBackendClient.class);
        CreativeChatGptClient textClient = mock(CreativeChatGptClient.class);
        CreativeImageClient imageClient = mock(CreativeImageClient.class);
        CreativeGenerationService service =
                new CreativeGenerationService(backendClient, textClient, new ObjectMapper());
        ExperimentDto experiment = pendingExperiment();
        experiment.setCreativeImagePrompt(null);
        CreateCreativeRequest generated = new CreateCreativeRequest();
        generated.setHeadline(repeat("Headline longa", 30));
        generated.setPrimaryText(repeat("Texto principal persuasivo", 30));
        generated.setDescription(repeat("Descricao complementar", 30));
        generated.setCta("Gerar minha amostra personalizada agora");

        when(backendClient.listPending(5)).thenReturn(List.of(experiment));
        when(textClient.generateCreatives(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(1)))
                .thenReturn(generation(List.of(generated)));
        when(textClient.generateCreatives(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.eq(1),
                org.mockito.ArgumentMatchers.contains("primaryText excede 125 caracteres")))
                .thenReturn(generation(List.of(generated)));
        CreativeGenerationService.ProcessingSummary summary = service.processPending(5);

        assertThat(summary.failed()).isEqualTo(1);
        verify(imageClient, never()).generateImage(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyString());
        verify(backendClient, never()).createCreative(
                org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.any());
        verify(backendClient).markFailed(
                org.mockito.ArgumentMatchers.eq(49L),
                org.mockito.ArgumentMatchers.contains("primaryText excede 125 caracteres"),
                org.mockito.ArgumentMatchers.any());
        verify(backendClient).markFailed(
                org.mockito.ArgumentMatchers.eq(49L),
                org.mockito.ArgumentMatchers.contains("(atual:"),
                org.mockito.ArgumentMatchers.any());
    }

    /** Garante uma reescrita semântica antes de desistir de uma copy inválida. */
    @Test
    void shouldRetryCopyGenerationAfterMetaContractViolation() {
        CreativeGenerationBackendClient backendClient = mock(CreativeGenerationBackendClient.class);
        CreativeChatGptClient textClient = mock(CreativeChatGptClient.class);
        CreativeImageClient imageClient = mock(CreativeImageClient.class);
        CreativeGenerationService service =
                new CreativeGenerationService(backendClient, textClient, new ObjectMapper());
        ExperimentDto experiment = pendingExperiment();
        CreateCreativeRequest invalid = new CreateCreativeRequest();
        invalid.setPrimaryText(repeat("Texto principal persuasivo", 30));
        CreateCreativeRequest valid = new CreateCreativeRequest();
        valid.setHeadline("Seu perfil à altura");
        valid.setPrimaryText("Veja uma prévia real do seu perfil e conheça o kit Agenda Cheia por R$ 67.");
        valid.setDescription("Veja antes de comprar");
        valid.setCta("LEARN_MORE");

        when(backendClient.listPending(5)).thenReturn(List.of(experiment));
        when(textClient.generateCreatives(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(1)))
                .thenReturn(generation(List.of(invalid)));
        when(textClient.generateCreatives(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.eq(1),
                org.mockito.ArgumentMatchers.contains("primaryText excede 125 caracteres")))
                .thenReturn(generation(List.of(valid)));
        CreativeGenerationService.ProcessingSummary summary = service.processPending(5);

        assertThat(summary.succeeded()).isEqualTo(1);
        verify(textClient).generateCreatives(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(1));
        verify(textClient).generateCreatives(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.eq(1),
                org.mockito.ArgumentMatchers.contains("primaryText excede 125 caracteres"));
        verify(backendClient).createCreative(
                org.mockito.ArgumentMatchers.eq(49L), org.mockito.ArgumentMatchers.eq(valid));
    }

    /** Garante que a validação conta emojis como caracteres Unicode completos, e não como dois chars UTF-16. */
    @Test
    void shouldAcceptPrimaryTextAtUnicodeCodePointLimit() {
        CreativeGenerationBackendClient backendClient = mock(CreativeGenerationBackendClient.class);
        CreativeChatGptClient textClient = mock(CreativeChatGptClient.class);
        CreativeImageClient imageClient = mock(CreativeImageClient.class);
        CreativeGenerationService service =
                new CreativeGenerationService(backendClient, textClient, new ObjectMapper());
        ExperimentDto experiment = pendingExperiment();
        CreateCreativeRequest generated = new CreateCreativeRequest();
        generated.setHeadline("Agenda cheia");
        generated.setPrimaryText("🚀".repeat(125));
        generated.setDescription("Venda com clareza");
        generated.setCta("LEARN_MORE");

        when(backendClient.listPending(5)).thenReturn(List.of(experiment));
        when(textClient.generateCreatives(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(1)))
                .thenReturn(generation(List.of(generated)));
        CreativeGenerationService.ProcessingSummary summary = service.processPending(5);

        assertThat(summary.succeeded()).isEqualTo(1);
        verify(backendClient).createCreative(49L, generated);
    }

    /** Garante que Íris não cria criativo sem prova de Dédalo aprovada por Têmis. */
    @Test
    void shouldFailPendingGenerationWhenImageUrlIsEmpty() {
        CreativeGenerationBackendClient backendClient = mock(CreativeGenerationBackendClient.class);
        CreativeChatGptClient textClient = mock(CreativeChatGptClient.class);
        CreativeImageClient imageClient = mock(CreativeImageClient.class);
        CreativeGenerationService service =
                new CreativeGenerationService(backendClient, textClient, new ObjectMapper());
        ExperimentDto experiment = pendingExperiment();
        experiment.setCommercialPlanVisualAssets(null);
        CreateCreativeRequest generated = new CreateCreativeRequest();
        generated.setHeadline("Headline");
        generated.setPrimaryText("Texto principal");
        generated.setStatus(CreativeStatus.DRAFT);

        when(backendClient.listPending(5)).thenReturn(List.of(experiment));
        when(textClient.generateCreatives(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(1)))
                .thenReturn(generation(List.of(generated)));
        CreativeGenerationService.ProcessingSummary summary = service.processPending(5);

        assertThat(summary.succeeded()).isZero();
        assertThat(summary.failed()).isEqualTo(1);
        verify(backendClient, never()).createCreative(
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.any());
        verify(backendClient).markFailed(
                org.mockito.ArgumentMatchers.eq(49L),
                org.mockito.ArgumentMatchers.contains("prova visual de Dédalo aprovada por Têmis"),
                org.mockito.ArgumentMatchers.any());
    }

    /** Cria um experimento pendente mínimo para o cenário de teste. */
    private ExperimentDto pendingExperiment() {
        ExperimentDto dto = new ExperimentDto();
        dto.setId(49L);
        dto.setName("Experimento 49");
        dto.setHypothesis("Hipótese");
        dto.setCreativeGenerationMode(CreativeGenerationMode.DEFAULT);
        dto.setCreativeGenerationStatus(CreativeGenerationStatus.REQUESTED);
        dto.setCreativesToGenerate(1);
        dto.setCreativeImagePrompt("Prompt visual");
        dto.setCommercialPlanVisualAssets(
                "{\"assets\":[{\"url\":\"/assets/product-deliverable.png\","
                        + "\"label\":\"Kit real do produto\",\"purpose\":\"DELIVERY\"}]}");
        return dto;
    }

    /** Cria retorno de modelo com auditoria completa para os cenários do worker. */
    private CreativeChatGptClient.Generation generation(List<CreateCreativeRequest> creatives) {
        return new CreativeChatGptClient.Generation(
                creatives,
                null,
                null,
                new CreativeChatGptClient.ExecutionAudit(
                        "gpt-test",
                        "medium",
                        "Núcleo de Íris.\n\nGere a copy.",
                        "Núcleo de Íris.",
                        "Gere a copy."));
    }

    /** Repete um texto de base para montar entradas maiores que o contrato persistivel. */
    private String repeat(String text, int times) {
        return String.join(" ", java.util.Collections.nCopies(times, text));
    }
}
