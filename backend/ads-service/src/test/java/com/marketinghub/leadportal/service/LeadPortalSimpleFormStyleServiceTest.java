package com.marketinghub.leadportal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketinghub.leadportal.LeadPortalSimpleFormStyle;
import com.marketinghub.leadportal.LeadPortalSimpleFormStyleDefinition;
import com.marketinghub.leadportal.dto.CreateLeadPortalSimpleFormStyleRequest;
import com.marketinghub.leadportal.dto.UpdateLeadPortalSimpleFormStyleRequest;
import com.marketinghub.leadportal.repository.LeadPortalSimpleFormStyleRepository;
import com.marketinghub.openai.OpenAiResponse;
import com.marketinghub.openai.service.OpenAiPricingService;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LeadPortalSimpleFormStyleServiceTest {

    @Mock
    private LeadPortalSimpleFormStyleRepository repository;

    @Mock
    private LeadPortalSimpleFormStyleGenerator generator;

    @Mock
    private OpenAiPricingService pricingService;

    @InjectMocks
    private LeadPortalSimpleFormStyleService service;

    private LeadPortalSimpleFormStyleDefinition definition;
    private LeadPortalSimpleFormStyleGenerator.Generation generation;

    @BeforeEach
    void setUp() {
        definition = new LeadPortalSimpleFormStyleDefinition(
                "#000000", null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null,
                "image-right", null, null);
        OpenAiResponse.OpenAiUsage usage = new OpenAiResponse.OpenAiUsage(100, 50, null, null, null);
        generation = new LeadPortalSimpleFormStyleGenerator.Generation(definition, usage, "rendered", "raw");
        lenient().when(generator.generate(org.mockito.ArgumentMatchers.any())).thenReturn(generation);
        lenient().when(pricingService.estimateBatchCost(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(new BigDecimal("1.2345"));
        lenient().when(repository.findBySlug(org.mockito.ArgumentMatchers.anyString())).thenReturn(Optional.empty());
        when(repository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createGeneratesDefinitionAndStoresCost() {
        CreateLeadPortalSimpleFormStyleRequest request = new CreateLeadPortalSimpleFormStyleRequest();
        request.setName("Estilo Neon");
        request.setSlug("estilo-neon");
        request.setTextModel("gpt-4o-mini");
        request.setTextPrompt("Use neon contrast");

        LeadPortalSimpleFormStyle created = service.create(request);

        assertThat(created.getDefinition()).isEqualTo(definition);
        assertThat(created.getTextModel()).isEqualTo("gpt-4o-mini");
        assertThat(created.getTextPrompt()).isEqualTo("Use neon contrast");
        assertThat(created.getGenerationCostUsd()).isEqualByComparingTo("1.2345");
        verify(repository).save(created);
    }

    @Test
    void updateSkipsGenerationWhenOnlyMetadataChanges() {
        LeadPortalSimpleFormStyle existing = LeadPortalSimpleFormStyle.builder()
                .id(10L)
                .name("Atual")
                .slug("atual")
                .textModel("gpt-4o-mini")
                .textPrompt("Original")
                .definition(definition)
                .build();
        when(repository.findById(10L)).thenReturn(Optional.of(existing));

        UpdateLeadPortalSimpleFormStyleRequest request = new UpdateLeadPortalSimpleFormStyleRequest();
        request.setPreviewImageUrl("https://example.com/preview.png");

        LeadPortalSimpleFormStyle updated = service.update(10L, request);

        assertThat(updated.getPreviewImageUrl()).isEqualTo("https://example.com/preview.png");
        verify(generator, never()).generate(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void updateRegeneratesWhenPromptChanges() {
        LeadPortalSimpleFormStyle existing = LeadPortalSimpleFormStyle.builder()
                .id(11L)
                .name("Atual")
                .slug("atual")
                .textModel("gpt-4o-mini")
                .textPrompt("Original")
                .definition(definition)
                .build();
        when(repository.findById(11L)).thenReturn(Optional.of(existing));

        UpdateLeadPortalSimpleFormStyleRequest request = new UpdateLeadPortalSimpleFormStyleRequest();
        request.setTextPrompt("Nova direção criativa");

        LeadPortalSimpleFormStyle updated = service.update(11L, request);

        assertThat(updated.getTextPrompt()).isEqualTo("Nova direção criativa");
        assertThat(updated.getDefinition()).isEqualTo(definition);
        verify(generator).generate(org.mockito.ArgumentMatchers.any());
    }
}
