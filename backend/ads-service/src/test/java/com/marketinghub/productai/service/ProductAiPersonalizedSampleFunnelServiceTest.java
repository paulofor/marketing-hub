package com.marketinghub.productai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.leadportal.LeadPortalFlow;
import com.marketinghub.leadportal.integration.LeadPortalFlowPublisher;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.productai.PersonalizedSampleFunnelTemplate;
import com.marketinghub.productai.ProductAiSubtype;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.leadportal.LeadPortalFlowRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Valida a criação configurável dos funis reutilizáveis de amostra personalizada. */
@ExtendWith(MockitoExtension.class)
class ProductAiPersonalizedSampleFunnelServiceTest {
  @Mock private ExperimentRepository experimentRepository;
  @Mock private LeadPortalFlowRepository flowRepository;
  @Mock private LeadPortalFlowPublisher publisher;
  private ProductAiPersonalizedSampleFunnelService service;
  private Experiment experiment;

  /** Prepara um experimento Produto IA válido e repositórios idempotentes. */
  @BeforeEach
  void setUp() {
    service =
        new ProductAiPersonalizedSampleFunnelService(
            experimentRepository, flowRepository, publisher);
    MarketNiche niche = MarketNiche.builder().name("Nail Design").build();
    experiment =
        Experiment.builder()
            .id(1L)
            .name("Agenda Cheia")
            .niche(niche)
            .productAiSubtype(ProductAiSubtype.AI_PERSONALIZED_SAMPLE)
            .build();
    when(experimentRepository.findById(1L)).thenReturn(Optional.of(experiment));
    when(flowRepository.findAllByExperimentIdOrderByCreatedAtDesc(1L))
        .thenReturn(java.util.List.of());
    when(flowRepository.findBySlug(any())).thenReturn(Optional.empty());
    when(flowRepository.save(any()))
        .thenAnswer(
            invocation -> {
              LeadPortalFlow flow = invocation.getArgument(0);
              flow.setId(10L);
              return flow;
            });
  }

  /** Garante que a microamostra social preserva três decisões e contato/foto auxiliares. */
  @Test
  void createsReusableSocialMediaMicroSampleTemplate() {
    var result =
        service.createOrUpdate(1L, PersonalizedSampleFunnelTemplate.SOCIAL_MEDIA_MICRO_SAMPLE);

    assertThat(result.dataKeys())
        .containsExactly(
            "nome_profissional", "servico_divulgado", "estilo_visual", "email", "foto_referencia");
    assertThat(experiment.getLeadPortalFlow().getQuestions().get(2).getOptions())
        .containsExactly("Elegante e minimalista", "Delicada e romântica", "Marcante e moderna");
    assertThat(experiment.getLeadPortalFlow().getQuestions().get(4).isRequired()).isFalse();
    verify(publisher).publish(experiment.getLeadPortalFlow());
  }

  /** Garante que a escolha do template substitui a antiga inferência pelo nome do produto. */
  @Test
  void usesExplicitGenericTemplateEvenForNailExperiment() {
    var result = service.createOrUpdate(1L, PersonalizedSampleFunnelTemplate.GENERIC);

    assertThat(result.dataKeys())
        .containsExactly(
            "email",
            "negocio_projeto",
            "contexto_atual",
            "objetivo_visual",
            "dados_personalizacao");
  }
}
