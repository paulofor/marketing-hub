package com.marketinghub.leadportal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketinghub.leadportal.LeadPortalSimpleFormStyle;
import com.marketinghub.leadportal.LeadPortalSimpleFormStyleDefinition;
import com.marketinghub.leadportal.dto.CreateLeadPortalSimpleFormStyleRequest;
import com.marketinghub.leadportal.dto.UpdateLeadPortalSimpleFormStyleRequest;
import com.marketinghub.leadportal.repository.LeadPortalSimpleFormStyleRepository;
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

    @InjectMocks
    private LeadPortalSimpleFormStyleService service;

    private LeadPortalSimpleFormStyleDefinition definition;

    @BeforeEach
    void setUp() {
        definition = new LeadPortalSimpleFormStyleDefinition(
                "#000000", null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null,
                "image-right", null, null);
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

        assertThat(created.getDefinition()).isNull();
        assertThat(created.getTextModel()).isEqualTo("gpt-4o-mini");
        assertThat(created.getTextPrompt()).isEqualTo("Use neon contrast");
        assertThat(created.getGenerationStatus()).isEqualTo(LeadPortalSimpleFormStyleService.GENERATION_STATUS_PENDING);
        assertThat(created.getGenerationCostUsd()).isNull();
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
        assertThat(updated.getGenerationStatus()).isNull();
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
        assertThat(updated.getDefinition()).isNull();
        assertThat(updated.getGenerationStatus()).isEqualTo(LeadPortalSimpleFormStyleService.GENERATION_STATUS_PENDING);
    }
}
