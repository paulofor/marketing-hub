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
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Valida a reativação do processamento de criativos pendentes pelo AI Worker.
 */
class CreativeGenerationServiceTest {

    /**
     * Garante que o serviço consome a fila, gera imagem, cria o criativo e conclui a pendência.
     */
    @Test
    void shouldProcessDefaultPendingCreativeGeneration() {
        CreativeGenerationBackendClient backendClient = mock(CreativeGenerationBackendClient.class);
        CreativeChatGptClient textClient = mock(CreativeChatGptClient.class);
        CreativeImageClient imageClient = mock(CreativeImageClient.class);
        CreativeGenerationService service =
                new CreativeGenerationService(backendClient, textClient, imageClient, new ObjectMapper());
        ExperimentDto experiment = pendingExperiment();
        CreateCreativeRequest generated = new CreateCreativeRequest();
        generated.setHeadline("Headline");
        generated.setPrimaryText("Texto principal");
        generated.setCta("Gerar minha amostra personalizada");
        generated.setStatus(CreativeStatus.DRAFT);

        when(backendClient.listPending(5)).thenReturn(List.of(experiment));
        when(textClient.generateCreatives(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(1)))
                .thenReturn(new CreativeChatGptClient.Generation(List.of(generated), null, null));
        when(imageClient.generateImage(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.anyString()))
                .thenReturn("/assets/creative.jpg");

        CreativeGenerationService.ProcessingSummary summary = service.processPending(5);

        assertThat(summary.total()).isEqualTo(1);
        assertThat(summary.succeeded()).isEqualTo(1);
        assertThat(summary.failed()).isZero();
        verify(backendClient).markStarted(49L);
        ArgumentCaptor<CreateCreativeRequest> captor = ArgumentCaptor.forClass(CreateCreativeRequest.class);
        verify(backendClient).createCreative(org.mockito.ArgumentMatchers.eq(49L), captor.capture());
        assertThat(captor.getValue().getImageUrl()).isEqualTo("/assets/creative.jpg");
        assertThat(captor.getValue().getCta()).isEqualTo("LEARN_MORE");
        verify(backendClient).markCompleted(49L);
    }

    /** Garante que copy acima dos limites Meta seja bloqueada sem truncamento silencioso. */
    @Test
    void shouldRejectGeneratedCreativeTextAboveMetaLimits() {
        CreativeGenerationBackendClient backendClient = mock(CreativeGenerationBackendClient.class);
        CreativeChatGptClient textClient = mock(CreativeChatGptClient.class);
        CreativeImageClient imageClient = mock(CreativeImageClient.class);
        CreativeGenerationService service =
                new CreativeGenerationService(backendClient, textClient, imageClient, new ObjectMapper());
        ExperimentDto experiment = pendingExperiment();
        experiment.setCreativeImagePrompt(null);
        CreateCreativeRequest generated = new CreateCreativeRequest();
        generated.setHeadline(repeat("Headline longa", 30));
        generated.setPrimaryText(repeat("Texto principal persuasivo", 30));
        generated.setDescription(repeat("Descricao complementar", 30));
        generated.setCta("Gerar minha amostra personalizada agora");

        when(backendClient.listPending(5)).thenReturn(List.of(experiment));
        when(textClient.generateCreatives(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(1)))
                .thenReturn(new CreativeChatGptClient.Generation(List.of(generated), null, null));
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
                org.mockito.ArgumentMatchers.contains("primaryText excede 125 caracteres"));
    }

    /** Garante que o worker não cria criativo quando a imagem não foi gerada. */
    @Test
    void shouldFailPendingGenerationWhenImageUrlIsEmpty() {
        CreativeGenerationBackendClient backendClient = mock(CreativeGenerationBackendClient.class);
        CreativeChatGptClient textClient = mock(CreativeChatGptClient.class);
        CreativeImageClient imageClient = mock(CreativeImageClient.class);
        CreativeGenerationService service =
                new CreativeGenerationService(backendClient, textClient, imageClient, new ObjectMapper());
        ExperimentDto experiment = pendingExperiment();
        CreateCreativeRequest generated = new CreateCreativeRequest();
        generated.setHeadline("Headline");
        generated.setPrimaryText("Texto principal");
        generated.setStatus(CreativeStatus.DRAFT);

        when(backendClient.listPending(5)).thenReturn(List.of(experiment));
        when(textClient.generateCreatives(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(1)))
                .thenReturn(new CreativeChatGptClient.Generation(List.of(generated), null, null));
        when(imageClient.generateImage(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(" ");

        CreativeGenerationService.ProcessingSummary summary = service.processPending(5);

        assertThat(summary.succeeded()).isZero();
        assertThat(summary.failed()).isEqualTo(1);
        verify(backendClient, never()).createCreative(
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.any());
        verify(backendClient).markFailed(
                org.mockito.ArgumentMatchers.eq(49L),
                org.mockito.ArgumentMatchers.contains("Imagem do criativo não foi gerada"));
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
        return dto;
    }

    /** Repete um texto de base para montar entradas maiores que o contrato persistivel. */
    private String repeat(String text, int times) {
        return String.join(" ", java.util.Collections.nCopies(times, text));
    }
}
