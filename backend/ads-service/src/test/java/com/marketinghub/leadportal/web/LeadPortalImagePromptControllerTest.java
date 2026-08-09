package com.marketinghub.leadportal.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.marketinghub.leadportal.dto.LeadPortalImagePromptMetadataDto;
import com.marketinghub.leadportal.mapper.LeadPortalFlowMapper;
import com.marketinghub.leadportal.service.LeadPortalFlowService;
import com.marketinghub.leadportal.support.LeadPortalPublicUrlResolver;
import org.junit.jupiter.api.Test;

/** Valida o contrato administrativo de configuração dos prompts de imagem do Lead Portal. */
class LeadPortalImagePromptControllerTest {

  /** Confirma que novos fluxos recebem o GPT Image 2 como modelo padrão. */
  @Test
  void metadataUsesGptImage2ByDefault() {
    LeadPortalImagePromptController controller =
        new LeadPortalImagePromptController(
            mock(LeadPortalFlowService.class),
            mock(LeadPortalFlowMapper.class),
            mock(LeadPortalPublicUrlResolver.class));

    LeadPortalImagePromptMetadataDto metadata = controller.metadata();

    assertThat(metadata.defaultModel()).isEqualTo("gpt-image-2");
  }
}
