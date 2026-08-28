package com.marketinghub.product.service.commercialoffer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentPlatform;
import com.marketinghub.experiment.ExperimentStatus;
import com.marketinghub.pde.PdeProductionSlot;
import com.marketinghub.pde.PdeProductionSlotStatus;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.pde.PdeProductionSlotRepository;
import com.marketinghub.repository.jpa.product.ProductRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

/** Valida que a superfície pública recebe uma oferta vendável e vinculada ao experimento. */
@ExtendWith(MockitoExtension.class)
class PublicProductCommercialOfferServiceTest {
  @Mock private ProductRepository productRepository;
  @Mock private PdeProductionSlotRepository slotRepository;
  @Mock private ExperimentRepository experimentRepository;

  /** Monta a oferta de R$ 349 sem duplicar checkout ou copy no frontend. */
  @Test
  void returnsCanonicalCommercialOffer() {
    Product product = product();
    PdeProductionSlot slot = slot();
    Experiment experiment = experiment(product);
    when(productRepository.findBySlug("kit-whatsapp-pronto")).thenReturn(Optional.of(product));
    when(slotRepository.findByProductSlugOrderBySlotCodeAsc("kit-whatsapp-pronto"))
        .thenReturn(List.of(slot));
    when(experimentRepository.findById(89L)).thenReturn(Optional.of(experiment));

    var offer = service().getOffer("kit-whatsapp-pronto");

    assertThat(offer.experimentId()).isEqualTo(89L);
    assertThat(offer.experienceVersion()).isEqualTo("kit-whatsapp-pronto-pde-v2");
    assertThat(offer.layoutKey()).isEqualTo("assisted-service-v2");
    assertThat(offer.acquisitionChannel()).isEqualTo("DIRECT_ONE_TO_ONE");
    assertThat(offer.priceBrl()).isEqualByComparingTo("349.00");
    assertThat(offer.primaryCta()).isEqualTo("Quero meu atendimento sob medida");
    assertThat(offer.checkoutUrl()).isEqualTo("https://pay.example/kit-whatsapp");
    assertThat(offer.salesPageUrl()).isEqualTo("https://kit-whatsapp-pronto.digicomdigital.com.br");
    assertThat(offer.supplierDisplayName()).isEqualTo("Digicom Digital");
    assertThat(offer.termsUrl()).endsWith("/terms");
  }

  /** Impede que a resposta pública volte a expor razão social ou endereço do fornecedor. */
  @Test
  void minimizesPublicSupplierIdentity() throws Exception {
    Product product = product();
    when(productRepository.findBySlug("kit-whatsapp-pronto")).thenReturn(Optional.of(product));
    when(slotRepository.findByProductSlugOrderBySlotCodeAsc("kit-whatsapp-pronto"))
        .thenReturn(List.of(slot()));
    when(experimentRepository.findById(89L)).thenReturn(Optional.of(experiment(product)));

    String json = new ObjectMapper().writeValueAsString(service().getOffer("kit-whatsapp-pronto"));

    assertThat(json)
        .contains("\"supplierDisplayName\":\"Digicom Digital\"")
        .doesNotContain("supplierLegalName", "supplierAddress");
  }

  /** Bloqueia a oferta quando o checkout deixa de ser seguro ou atribuível. */
  @Test
  void rejectsInsecureCheckout() {
    Product product = product();
    Experiment experiment = experiment(product);
    experiment.setCommercialCheckoutUrl("http://pay.example/kit-whatsapp");
    when(productRepository.findBySlug("kit-whatsapp-pronto")).thenReturn(Optional.of(product));
    when(slotRepository.findByProductSlugOrderBySlotCodeAsc("kit-whatsapp-pronto"))
        .thenReturn(List.of(slot()));
    when(experimentRepository.findById(89L)).thenReturn(Optional.of(experiment));

    assertThatThrownBy(() -> service().getOffer("kit-whatsapp-pronto"))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Checkout comercial precisa usar HTTPS");
  }

  /** Bloqueia a oferta quando o contrato publicado preserva CTA de outra versão. */
  @Test
  void rejectsDivergentVersionedCommercialBinding() {
    Product product = product();
    PdeProductionSlot slot = slot();
    slot.setPublishedExperienceJson(
        slot.getPublishedExperienceJson().replace("sob medida", "personalizado"));
    Experiment experiment = experiment(product);
    when(productRepository.findBySlug("kit-whatsapp-pronto")).thenReturn(Optional.of(product));
    when(slotRepository.findByProductSlugOrderBySlotCodeAsc("kit-whatsapp-pronto"))
        .thenReturn(List.of(slot));
    when(experimentRepository.findById(89L)).thenReturn(Optional.of(experiment));

    assertThatThrownBy(() -> service().getOffer("kit-whatsapp-pronto"))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("diverge da oferta comercial canônica");
  }

  /** Bloqueia a oferta quando a promessa do experimento não pertence à experiência publicada. */
  @Test
  void rejectsDivergentVersionedPromise() {
    Product product = product();
    Experiment experiment = experiment(product);
    experiment.setFunnelPromise("Promessa antiga de outra versão");
    when(productRepository.findBySlug("kit-whatsapp-pronto")).thenReturn(Optional.of(product));
    when(slotRepository.findByProductSlugOrderBySlotCodeAsc("kit-whatsapp-pronto"))
        .thenReturn(List.of(slot()));
    when(experimentRepository.findById(89L)).thenReturn(Optional.of(experiment));

    assertThatThrownBy(() -> service().getOffer("kit-whatsapp-pronto"))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("diverge da oferta comercial canônica");
  }

  /** Cria o serviço isolado usado pelos cenários de contrato. */
  private PublicProductCommercialOfferService service() {
    return new PublicProductCommercialOfferService(
        productRepository,
        slotRepository,
        experimentRepository,
        "Digicom Digital",
        "00.000.000/0001-00",
        "teste@sandbox.local");
  }

  /** Cria o produto personalizado usado como fonte de posicionamento. */
  private Product product() {
    return Product.builder()
        .id(9L)
        .slug("kit-whatsapp-pronto")
        .targetAudience("Pequenos prestadores que atendem pelo WhatsApp")
        .productFormat("IMPLANTACAO_PERSONALIZADA")
        .deliveryMode("ASSISTIDA_MANUAL")
        .valueUnit("Atendimento sob medida pronto para usar")
        .build();
  }

  /** Cria o slot produtivo diretamente vinculado ao experimento da oferta. */
  private PdeProductionSlot slot() {
    return PdeProductionSlot.builder()
        .slotCode("v2")
        .productSlug("kit-whatsapp-pronto")
        .experienceVersion("kit-whatsapp-pronto-pde-v2")
        .layoutKey("assisted-service-v2")
        .status(PdeProductionSlotStatus.ACTIVE)
        .sourceExperimentId(89L)
        .publicUrl("https://kit-whatsapp-pronto.digicomdigital.com.br")
        .publishedExperienceJson(
            "{\"slug\":\"kit-whatsapp-pronto\",\"experienceVersion\":\"kit-whatsapp-pronto-pde-v2\",\"layoutKey\":\"assisted-service-v2\",\"promise\":\"Após o pagamento confirmado e o briefing mínimo completo, em até 48 horas receba seu atendimento de WhatsApp personalizado e revisado: respostas, perguntas, follow-ups e regras prontas para conduzir cada conversa ao próximo passo com mais clareza e menos improviso.\",\"commercialBinding\":{\"experimentId\":89,\"primaryCta\":\"Quero meu atendimento sob medida\",\"priceBrl\":349,\"billingModel\":\"ONE_TIME\"}}")
        .updatedAt(Instant.parse("2026-08-22T20:00:00Z"))
        .build();
  }

  /** Cria o experimento orgânico com copy, preço e checkout completos. */
  private Experiment experiment(Product product) {
    return Experiment.builder()
        .id(89L)
        .product(product)
        .status(ExperimentStatus.PLANNED)
        .platform(ExperimentPlatform.DIRECT_ONE_TO_ONE)
        .singlePain("Conversas improvisadas terminam sem próximo passo")
        .freeReward("Demonstração personalizada do método")
        .funnelPromise(
            "Após o pagamento confirmado e o briefing mínimo completo, em até 48 horas receba seu atendimento de WhatsApp personalizado e revisado: respostas, perguntas, follow-ups e regras prontas para conduzir cada conversa ao próximo passo com mais clareza e menos improviso.")
        .primaryCta("Quero meu atendimento sob medida")
        .unitPrice(new BigDecimal("349.00"))
        .commercialCheckoutUrl("https://pay.example/kit-whatsapp")
        .build();
  }
}
