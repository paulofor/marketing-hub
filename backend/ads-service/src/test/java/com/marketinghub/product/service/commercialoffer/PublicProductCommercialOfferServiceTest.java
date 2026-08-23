package com.marketinghub.product.service.commercialoffer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

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
    assertThat(offer.acquisitionChannel()).isEqualTo("DIRECT_ONE_TO_ONE");
    assertThat(offer.priceBrl()).isEqualByComparingTo("349.00");
    assertThat(offer.primaryCta()).isEqualTo("Quero meu atendimento sob medida");
    assertThat(offer.checkoutUrl()).isEqualTo("https://pay.example/kit-whatsapp");
    assertThat(offer.salesPageUrl()).isEqualTo("https://kit-whatsapp-pronto.digicomdigital.com.br");
    assertThat(offer.supplierLegalName()).isEqualTo("Fornecedor de Homologação Ltda.");
    assertThat(offer.termsUrl()).endsWith("/terms");
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

  /** Cria o serviço isolado usado pelos cenários de contrato. */
  private PublicProductCommercialOfferService service() {
    return new PublicProductCommercialOfferService(
        productRepository,
        slotRepository,
        experimentRepository,
        "Fornecedor de Homologação Ltda.",
        "00.000.000/0001-00",
        "Endereço de homologação, 100",
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
        .productSlug("kit-whatsapp-pronto")
        .status(PdeProductionSlotStatus.ACTIVE)
        .sourceExperimentId(89L)
        .publicUrl("https://kit-whatsapp-pronto.digicomdigital.com.br")
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
        .funnelPromise("Implantação personalizada em até 48 horas")
        .primaryCta("Quero meu atendimento sob medida")
        .unitPrice(new BigDecimal("349.00"))
        .commercialCheckoutUrl("https://pay.example/kit-whatsapp")
        .build();
  }
}
