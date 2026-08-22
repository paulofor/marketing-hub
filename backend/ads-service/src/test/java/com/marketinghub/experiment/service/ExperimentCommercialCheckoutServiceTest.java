package com.marketinghub.experiment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentStatus;
import com.marketinghub.leadportal.integration.LeadPortalPaymentsClient;
import com.marketinghub.pde.PdeProductionSlot;
import com.marketinghub.pde.PdeProductionSlotStatus;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.pde.PdeProductionSlotRepository;
import com.marketinghub.repository.jpa.product.ProductRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

/**
 * Responsabilidade: validar o vínculo seguro entre experimento, entrega PDE e checkout comercial.
 */
@ExtendWith(MockitoExtension.class)
class ExperimentCommercialCheckoutServiceTest {

  @Mock private ExperimentRepository experimentRepository;
  @Mock private ProductRepository productRepository;
  @Mock private PdeProductionSlotRepository pdeProductionSlotRepository;
  @Mock private LeadPortalPaymentsClient paymentsClient;
  private ExperimentCommercialCheckoutService service;

  /** Prepara o serviço com dependências controladas para cada cenário. */
  @BeforeEach
  void setUp() {
    service =
        new ExperimentCommercialCheckoutService(
            experimentRepository, productRepository, pdeProductionSlotRepository, paymentsClient);
  }

  /** Deve criar checkout de R$ 349 somente após existir uma entrega PDE validada. */
  @Test
  void createsCheckoutFromValidatedDeliveryAndPersistsSeparateUrls() {
    Product product =
        Product.builder().id(9L).slug("kit-whatsapp-pronto").name("Kit WhatsApp Pronto").build();
    Experiment experiment =
        Experiment.builder()
            .id(89L)
            .product(product)
            .status(ExperimentStatus.PLANNED)
            .unitPrice(new BigDecimal("349.00"))
            .followUpActionUrl("https://landing.exemplo.test")
            .build();
    PdeProductionSlot slot =
        PdeProductionSlot.builder()
            .productSlug(product.getSlug())
            .slotCode("v1")
            .publicUrl("https://kit-whatsapp-pronto.digicomdigital.com.br")
            .status(PdeProductionSlotStatus.ACTIVE)
            .validationStatus("OK")
            .build();
    when(experimentRepository.findById(89L)).thenReturn(Optional.of(experiment));
    when(pdeProductionSlotRepository.findByProductSlugOrderBySlotCodeAsc(product.getSlug()))
        .thenReturn(List.of(slot));
    when(paymentsClient.createCommercialProductCheckout(any()))
        .thenReturn(
            new LeadPortalPaymentsClient.CommercialProductCheckoutResponse(
                product.getSlug(),
                product.getId(),
                experiment.getId(),
                "pref-89",
                "https://checkout.mercadopago.com.br/pref-89",
                experiment.getUnitPrice(),
                "BRL",
                slot.getPublicUrl()));

    var response = service.create(89L);

    assertThat(response.amount()).isEqualByComparingTo("349.00");
    assertThat(experiment.getCommercialCheckoutUrl()).isEqualTo(response.checkoutUrl());
    assertThat(experiment.getFollowUpActionUrl()).isEqualTo("https://landing.exemplo.test");
    assertThat(product.getPublicUrl()).isEqualTo(slot.getPublicUrl());
    verify(productRepository).save(product);
    verify(experimentRepository).save(experiment);
  }

  /** Deve rejeitar checkout antes da entrega para não vender um produto indisponível. */
  @Test
  void blocksCheckoutWithoutValidatedDelivery() {
    Product product =
        Product.builder().id(9L).slug("kit-whatsapp-pronto").name("Kit WhatsApp Pronto").build();
    Experiment experiment =
        Experiment.builder()
            .id(89L)
            .product(product)
            .status(ExperimentStatus.PLANNED)
            .unitPrice(new BigDecimal("349.00"))
            .build();
    when(experimentRepository.findById(89L)).thenReturn(Optional.of(experiment));
    when(pdeProductionSlotRepository.findByProductSlugOrderBySlotCodeAsc(product.getSlug()))
        .thenReturn(List.of());

    assertThatThrownBy(() -> service.create(89L))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("área de entrega PDE");
    verify(paymentsClient, never()).createCommercialProductCheckout(any());
  }

  /** Deve devolver o checkout já persistido sem criar outra preferência no provedor. */
  @Test
  void reusesExistingCheckoutIdempotently() {
    Product product =
        Product.builder()
            .id(9L)
            .slug("kit-whatsapp-pronto")
            .name("Kit WhatsApp Pronto")
            .publicUrl("https://kit-whatsapp-pronto.digicomdigital.com.br")
            .build();
    Experiment experiment =
        Experiment.builder()
            .id(89L)
            .product(product)
            .status(ExperimentStatus.PLANNED)
            .unitPrice(new BigDecimal("349.00"))
            .commercialCheckoutUrl("https://checkout.mercadopago.com.br/pref-89")
            .build();
    when(experimentRepository.findById(89L)).thenReturn(Optional.of(experiment));

    var response = service.create(89L);

    assertThat(response.checkoutUrl()).isEqualTo(experiment.getCommercialCheckoutUrl());
    verify(paymentsClient, never()).createCommercialProductCheckout(any());
  }
}
