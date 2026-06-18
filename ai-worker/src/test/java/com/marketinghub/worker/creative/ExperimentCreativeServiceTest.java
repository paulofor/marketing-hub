package com.marketinghub.worker.creative;

import com.marketinghub.creative.Creative;
import com.marketinghub.creative.dto.CreateCreativeRequest;
import com.marketinghub.experiment.CreativeGenerationMode;
import com.marketinghub.experiment.CreativeGenerationStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.creative.service.CreativeService;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.hypothesis.Hypothesis;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Validates the scheduled creative generation workflow and its persistence safeguards.
 */
@ExtendWith(MockitoExtension.class)
class ExperimentCreativeServiceTest {
    @Mock
    ExperimentRepository experimentRepository;
    @Mock
    CreativeChatGptClient chatGptClient;
    @Mock
    CreativeImageClient imageClient;
    @Mock
    CreativeService creativeService;
    ExperimentCreativeService service;

    Experiment experiment;

    /**
     * Creates the shared experiment fixture and service under test.
     */
    @BeforeEach
    void setup() {
        experiment = new Experiment();
        experiment.setId(1L);
        experiment.setName("Experimento A");
        experiment.setHypothesis("Validar proposta principal para o público-alvo");
        experiment.setCreativesToGenerate(1);
        Hypothesis h = new Hypothesis();
        h.setTitle("title");
        h.setPersona("profissionais autônomos");
        h.setProblem("não conseguem manter um fluxo constante de clientes");
        h.setPromise("aumentar a base de clientes com campanhas digitais");
        experiment.setHypothesisRef(h);
        service = new ExperimentCreativeService(experimentRepository, chatGptClient, imageClient, creativeService, new ObjectMapper());
    }

    /**
     * Ensures successful default generation saves the creative and clears the pending flag.
     */
    @Test
    void generateCreatesCreativesAndResetsFlag() {
        when(experimentRepository.findAllToGenerateCreatives()).thenReturn(List.of(experiment));
        CreateCreativeRequest req = new CreateCreativeRequest();
        req.setHeadline("h1");
        req.setPrimaryText("p1");
        when(chatGptClient.generateCreatives(experiment, 1)).thenReturn(new CreativeChatGptClient.Generation(List.of(req), null, null));
        when(imageClient.generateImage(anyString(), isNull(), anyString())).thenReturn("img");
        Creative saved = new Creative();
        when(creativeService.create(1L, req)).thenReturn(saved);

        Map<Long, List<Creative>> result = service.generate();

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(imageClient).generateImage(promptCaptor.capture(), isNull(), contains("mode=DEFAULT"));
        String usedPrompt = promptCaptor.getValue();
        assertThat(usedPrompt).contains("Facebook e Instagram");
        assertThat(usedPrompt).contains("headline \"h1\"");
        assertThat(usedPrompt).contains("Experimento: Experimento A");
        assertThat(usedPrompt).contains("hipótese \"title\"");
        assertThat(req.getImageUrl()).isEqualTo("img");
        verify(creativeService).create(1L, req);
        verify(experimentRepository, times(2)).save(experiment);
        assertThat(experiment.getCreativesToGenerate()).isZero();
        assertThat(experiment.getCreativeGenerationStatus()).isEqualTo(CreativeGenerationStatus.COMPLETED);
        assertThat(experiment.getCreativeGenerationError()).isNull();
        assertThat(result.get(1L)).containsExactly(saved);
    }

    /**
     * Ensures long text fields are truncated before the creative is persisted.
     */
    @Test
    void truncateLongTextsBeforeSaving() {
        when(experimentRepository.findAllToGenerateCreatives()).thenReturn(List.of(experiment));
        String longText = "a".repeat(300);
        CreateCreativeRequest req = new CreateCreativeRequest();
        req.setHeadline(longText);
        req.setPrimaryText(longText);
        when(chatGptClient.generateCreatives(experiment, 1)).thenReturn(new CreativeChatGptClient.Generation(List.of(req), null, null));
        when(imageClient.generateImage(anyString(), isNull(), anyString())).thenReturn("img");
        Creative saved = new Creative();
        when(creativeService.create(eq(1L), any(CreateCreativeRequest.class))).thenReturn(saved);

        service.generate();

        ArgumentCaptor<CreateCreativeRequest> captor = ArgumentCaptor.forClass(CreateCreativeRequest.class);
        verify(creativeService).create(eq(1L), captor.capture());
        CreateCreativeRequest captured = captor.getValue();
        assertThat(captured.getHeadline().length()).isEqualTo(40);
        assertThat(captured.getPrimaryText().length()).isEqualTo(125);
    }

    /**
     * Ensures generated text keeps at most thirty hashtags.
     */
    @Test
    void limitHashtagsToThirty() {
        when(experimentRepository.findAllToGenerateCreatives()).thenReturn(List.of(experiment));
        String hashtags = IntStream.rangeClosed(1, 35)
                .mapToObj(i -> "#t" + i)
                .collect(Collectors.joining(" "));
        CreateCreativeRequest req = new CreateCreativeRequest();
        req.setHeadline("headline");
        req.setPrimaryText(hashtags);
        when(chatGptClient.generateCreatives(experiment, 1)).thenReturn(new CreativeChatGptClient.Generation(List.of(req), null, null));
        when(imageClient.generateImage(anyString(), isNull(), anyString())).thenReturn("img");
        when(creativeService.create(eq(1L), any(CreateCreativeRequest.class))).thenReturn(new Creative());

        service.generate();

        ArgumentCaptor<CreateCreativeRequest> captor = ArgumentCaptor.forClass(CreateCreativeRequest.class);
        verify(creativeService).create(eq(1L), captor.capture());
        String savedText = captor.getValue().getPrimaryText();
        long count = Arrays.stream(savedText.split("\\s+")).filter(p -> p.startsWith("#")).count();
        assertThat(count).isEqualTo(30);
    }

    /**
     * Ensures pipeline mode uses existing ad copy and image briefing instead of text generation.
     */
    @Test
    void pipelineModeGeneratesCreativesFromExistingContent() {
        experiment.setCreativeGenerationMode(CreativeGenerationMode.PIPELINE_ADS);
        experiment.setCreativesToGenerate(1);
        experiment.setFollowUpActionUrl("https://destino.com");
        experiment.setAdCopy("""
                {"adCopy":{"primaryTextVariants":[{"label":"dor","primaryText":"Texto","headline":"Headline","description":"Descrição","ctaText":"Saiba mais"}]}}
                """);
        experiment.setAdImageBriefing("""
                {"adImageBriefing":{"briefings":[{"mustMatchAdVariant":"dor","visualBriefing":"Use contraste simples","hierarchy":"1) promessa 2) CTA","safeMargins":"10%","assetType":"estatico"}]}}
                """);
        when(experimentRepository.findAllToGenerateCreatives()).thenReturn(List.of(experiment));
        when(imageClient.generateImage(anyString(), anyString(), anyString())).thenReturn("img");
        Creative savedCreative = new Creative();
        when(creativeService.create(eq(1L), any(CreateCreativeRequest.class))).thenReturn(savedCreative);

        Map<Long, List<Creative>> result = service.generate();

        verify(chatGptClient, never()).generateCreatives(any(), anyInt());
        verify(creativeService, times(1)).create(eq(1L), any(CreateCreativeRequest.class));
        assertThat(result.get(1L)).containsExactly(savedCreative);
        assertThat(experiment.getCreativesToGenerate()).isZero();
        assertThat(experiment.getCreativeGenerationMode()).isEqualTo(CreativeGenerationMode.DEFAULT);
    }


    /**
     * Ensures pipeline mode accepts artifacts wrapped inside model response text fields.
     */
    @Test
    void pipelineModeGeneratesCreativesFromEmbeddedJsonArtifacts() {
        experiment.setCreativeGenerationMode(CreativeGenerationMode.PIPELINE_ADS);
        experiment.setCreativesToGenerate(1);
        experiment.setAdCopy("""
                Resposta do modelo:
                ```json
                {"adCopy":{"primaryTextVariants":[{"label":"dor","primaryText":"Texto","headline":"Headline","description":"Descrição","ctaText":"Saiba mais"}]}}
                ```
                """);
        experiment.setAdImageBriefing("""
                Texto antes {"adImageBriefing":{"briefings":[{"mustMatchAdVariant":"dor","visualBriefing":"Use contraste simples","hierarchy":"1) promessa 2) CTA","safeMargins":"10%","assetType":"story"}]}} texto depois
                """);
        when(experimentRepository.findAllToGenerateCreatives()).thenReturn(List.of(experiment));
        when(imageClient.generateImage(anyString(), anyString(), anyString())).thenReturn("img");
        Creative savedCreative = new Creative();
        when(creativeService.create(eq(1L), any(CreateCreativeRequest.class))).thenReturn(savedCreative);

        Map<Long, List<Creative>> result = service.generate();

        ArgumentCaptor<CreateCreativeRequest> requestCaptor = ArgumentCaptor.forClass(CreateCreativeRequest.class);
        verify(creativeService).create(eq(1L), requestCaptor.capture());
        assertThat(requestCaptor.getValue().getFormat()).isEqualTo("STORY");
        assertThat(result.get(1L)).containsExactly(savedCreative);
        assertThat(experiment.getCreativesToGenerate()).isZero();
        assertThat(experiment.getCreativeGenerationMode()).isEqualTo(CreativeGenerationMode.DEFAULT);
    }

    /**
     * Ensures pipeline image prompts keep the mandatory hypothesis filter title visible.
     */
    @Test
    void pipelineModeHighlightsHypothesisImageFilterTitleInPrompt() {
        experiment.setCreativeGenerationMode(CreativeGenerationMode.PIPELINE_ADS);
        experiment.setCreativesToGenerate(1);
        experiment.getHypothesisRef().setImageFilterTitle("Mães empreendedoras");
        experiment.setAdCopy("""
                {"adCopy":{"primaryTextVariants":[{"label":"dor","primaryText":"Texto","headline":"Headline","description":"Descrição","ctaText":"Saiba mais"}]}}
                """);
        experiment.setAdImageBriefing("""
                {"adImageBriefing":{"briefings":[{"mustMatchAdVariant":"dor","visualBriefing":"Use contraste simples","hierarchy":"1) promessa 2) CTA","safeMargins":"10%","assetType":"estatico"}]}}
                """);
        when(experimentRepository.findAllToGenerateCreatives()).thenReturn(List.of(experiment));
        when(imageClient.generateImage(anyString(), anyString(), anyString())).thenReturn("img");
        when(creativeService.create(eq(1L), any(CreateCreativeRequest.class))).thenReturn(new Creative());

        service.generate();

        ArgumentCaptor<String> finalPromptCaptor = ArgumentCaptor.forClass(String.class);
        verify(imageClient).generateImage(finalPromptCaptor.capture(), anyString(), contains("mode=PIPELINE_ADS"));
        String prompt = finalPromptCaptor.getValue();
        assertThat(prompt).contains("Obrigatório: usar o título de filtro \"Mães empreendedoras\" em destaque dentro da imagem");
    }

    /**
     * Ensures default generation clears the request when image generation returns no URL.
     */
    @Test
    void defaultModeSkipsCreativeWhenImageUrlIsMissing() {
        when(experimentRepository.findAllToGenerateCreatives()).thenReturn(List.of(experiment));
        CreateCreativeRequest req = new CreateCreativeRequest();
        req.setHeadline("headline");
        req.setPrimaryText("texto");
        when(chatGptClient.generateCreatives(experiment, 1))
                .thenReturn(new CreativeChatGptClient.Generation(List.of(req), null, null));
        when(imageClient.generateImage(anyString(), isNull(), anyString())).thenReturn(null);

        Map<Long, List<Creative>> result = service.generate();

        verify(creativeService, never()).create(anyLong(), any(CreateCreativeRequest.class));
        assertThat(req.getImageUrl()).isNull();
        assertThat(result).doesNotContainKey(1L);
        assertThat(experiment.getCreativesToGenerate()).isZero();
        verify(experimentRepository, times(2)).save(experiment);
        assertThat(experiment.getCreativeGenerationStatus()).isEqualTo(CreativeGenerationStatus.FAILED);
        assertThat(experiment.getCreativeGenerationError()).contains("Image generation returned no URL");
    }

    /**
     * Ensures pipeline generation clears the request when image generation returns no URL.
     */
    @Test
    void pipelineModeSkipsCreativeWhenImageUrlIsMissing() {
        experiment.setCreativeGenerationMode(CreativeGenerationMode.PIPELINE_ADS);
        experiment.setCreativesToGenerate(1);
        experiment.setAdCopy("""
                {"adCopy":{"primaryTextVariants":[{"label":"dor","primaryText":"Texto","headline":"Headline","description":"Descrição","ctaText":"Saiba mais"}]}}
                """);
        experiment.setAdImageBriefing("""
                {"adImageBriefing":{"briefings":[{"mustMatchAdVariant":"dor","visualBriefing":"Use contraste simples","hierarchy":"1) promessa 2) CTA","safeMargins":"10%","assetType":"estatico"}]}}
                """);
        when(experimentRepository.findAllToGenerateCreatives()).thenReturn(List.of(experiment));
        when(imageClient.generateImage(anyString(), anyString(), anyString())).thenReturn(" ");

        Map<Long, List<Creative>> result = service.generate();

        verify(creativeService, never()).create(anyLong(), any(CreateCreativeRequest.class));
        assertThat(result).doesNotContainKey(1L);
        assertThat(experiment.getCreativesToGenerate()).isZero();
        assertThat(experiment.getCreativeGenerationMode()).isEqualTo(CreativeGenerationMode.DEFAULT);
        verify(experimentRepository, times(2)).save(experiment);
        assertThat(experiment.getCreativeGenerationStatus()).isEqualTo(CreativeGenerationStatus.FAILED);
        assertThat(experiment.getCreativeGenerationError()).contains("Image generation returned no URL");
    }

}
