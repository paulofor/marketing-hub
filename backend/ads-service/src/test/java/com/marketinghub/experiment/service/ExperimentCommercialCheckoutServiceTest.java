package com.marketinghub.experiment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentStatus;
import com.marketinghub.leadportal.integration.LeadPortalPaymentsClient;
import com.marketinghub.pde.PdeProductionSlot;
import com.marketinghub.pde.PdeProductionSlotStatus;
import com.marketinghub.pde.service.PdeCommercialCheckoutContractResolver;
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
import org.mockito.ArgumentCaptor;
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
            experimentRepository,
            productRepository,
            pdeProductionSlotRepository,
            paymentsClient,
            new PdeCommercialCheckoutContractResolver(new ObjectMapper()));
  }

  /** Deve reconciliar o checkout versionado sem criar uma preferência concorrente. */
  @Test
  void reconcilesVersionedCheckoutWithoutCallingFallbackProvider() {
    Product product =
        Product.builder()
            .id(4L)
            .slug("metodo-musa-7-dias")
            .name("Método MUSA")
            .pdeExperienceJson(
                """
                {"commercialCheckout":{"provider":"PEPPER","checkoutUrl":"https://go.pepper.com.br/owm6x","offerReference":"owm6x","priceBrl":67,"currency":"BRL","billingModel":"ONE_TIME"}}
                """)
            .build();
    Experiment experiment =
        Experiment.builder()
            .id(90L)
            .product(product)
            .status(ExperimentStatus.PLANNED)
            .unitPrice(new BigDecimal("67.00"))
            .commercialCheckoutUrl("https://mercadopago.example/preferencia-antiga")
            .followUpActionUrl("https://v7.clubemusa.com.br")
            .build();
    PdeProductionSlot slot = activeSlot("v7", "https://v7.clubemusa.com.br", 90L);
    when(experimentRepository.findById(90L)).thenReturn(Optional.of(experiment));
    when(pdeProductionSlotRepository.findByProductSlugOrderBySlotCodeAsc(product.getSlug()))
        .thenReturn(List.of(slot));

    var response = service.create(90L);

    assertThat(response.preferenceId()).isEqualTo("owm6x");
    assertThat(response.checkoutUrl()).isEqualTo("https://go.pepper.com.br/owm6x");
    assertThat(experiment.getCommercialCheckoutUrl()).isEqualTo(response.checkoutUrl());
    verify(paymentsClient, never()).createCommercialProductCheckout(any());
    verify(productRepository).save(product);
    verify(experimentRepository).save(experiment);
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

  /** Deve revalidar checkout persistido para substituir preferência ligada a uma entrega antiga. */
  @Test
  void refreshesExistingCheckoutAgainstCurrentContractIdempotently() {
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
    PdeProductionSlot slot =
        PdeProductionSlot.builder()
            .productSlug(product.getSlug())
            .slotCode("v7")
            .publicUrl("https://v7.produto.test")
            .status(PdeProductionSlotStatus.ACTIVE)
            .validationStatus("OK")
            .sourceExperimentId(89L)
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
                "pref-v7",
                "https://checkout.mercadopago.com.br/pref-v7",
                experiment.getUnitPrice(),
                "BRL",
                slot.getPublicUrl()));

    var response = service.create(89L);

    assertThat(response.checkoutUrl()).endsWith("pref-v7");
    assertThat(experiment.getCommercialCheckoutUrl()).endsWith("pref-v7");
    verify(paymentsClient).createCommercialProductCheckout(any());
  }

  /** Deve escolher o slot cujo domínio corresponde ao destino mesmo com versões antigas ativas. */
  @Test
  void selectsDestinationSlotInsteadOfFirstActiveVersion() {
    Product product =
        Product.builder().id(4L).slug("metodo-musa-7-dias").name("Método MUSA").build();
    Experiment experiment =
        Experiment.builder()
            .id(90L)
            .product(product)
            .status(ExperimentStatus.PLANNED)
            .unitPrice(new BigDecimal("67.00"))
            .followUpActionUrl("https://v7.clubemusa.com.br?mh_preview=qa")
            .build();
    PdeProductionSlot v5 = activeSlot("v5", "https://v5.clubemusa.com.br", 74L);
    PdeProductionSlot v7 = activeSlot("v7", "https://v7.clubemusa.com.br", 90L);
    when(experimentRepository.findById(90L)).thenReturn(Optional.of(experiment));
    when(pdeProductionSlotRepository.findByProductSlugOrderBySlotCodeAsc(product.getSlug()))
        .thenReturn(List.of(v5, v7));
    when(paymentsClient.createCommercialProductCheckout(any()))
        .thenReturn(
            new LeadPortalPaymentsClient.CommercialProductCheckoutResponse(
                product.getSlug(),
                product.getId(),
                experiment.getId(),
                "pref-v7",
                "https://checkout.mercadopago.com.br/pref-v7",
                experiment.getUnitPrice(),
                "BRL",
                v7.getPublicUrl()));

    service.create(90L);

    ArgumentCaptor<LeadPortalPaymentsClient.CommercialProductCheckoutRequest> request =
        ArgumentCaptor.forClass(LeadPortalPaymentsClient.CommercialProductCheckoutRequest.class);
    verify(paymentsClient).createCommercialProductCheckout(request.capture());
    assertThat(request.getValue().deliveryPageUrl()).isEqualTo(v7.getPublicUrl());
    assertThat(product.getPublicUrl()).isEqualTo(v7.getPublicUrl());
  }

  /** Deve bloquear quando o provedor devolver versão ou valor divergente do contrato solicitado. */
  @Test
  void rejectsCheckoutResponseFromDifferentDeliveryVersion() {
    Product product =
        Product.builder().id(4L).slug("metodo-musa-7-dias").name("Método MUSA").build();
    Experiment experiment =
        Experiment.builder()
            .id(90L)
            .product(product)
            .status(ExperimentStatus.PLANNED)
            .unitPrice(new BigDecimal("67.00"))
            .followUpActionUrl("https://v7.clubemusa.com.br")
            .build();
    PdeProductionSlot v7 = activeSlot("v7", "https://v7.clubemusa.com.br", 90L);
    when(experimentRepository.findById(90L)).thenReturn(Optional.of(experiment));
    when(pdeProductionSlotRepository.findByProductSlugOrderBySlotCodeAsc(product.getSlug()))
        .thenReturn(List.of(v7));
    when(paymentsClient.createCommercialProductCheckout(any()))
        .thenReturn(
            new LeadPortalPaymentsClient.CommercialProductCheckoutResponse(
                product.getSlug(),
                product.getId(),
                experiment.getId(),
                "pref-v5",
                "https://checkout.mercadopago.com.br/pref-v5",
                experiment.getUnitPrice(),
                "BRL",
                "https://v5.clubemusa.com.br"));

    assertThatThrownBy(() -> service.create(90L))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("divergente");
    verify(productRepository, never()).save(any());
    verify(experimentRepository, never()).save(any());
  }

  /** Deve bloquear versões ativas concorrentes quando nenhuma corresponde ao destino informado. */
  @Test
  void blocksAmbiguousActiveDeliveryVersions() {
    Product product =
        Product.builder().id(4L).slug("metodo-musa-7-dias").name("Método MUSA").build();
    Experiment experiment =
        Experiment.builder()
            .id(90L)
            .product(product)
            .status(ExperimentStatus.PLANNED)
            .unitPrice(new BigDecimal("67.00"))
            .followUpActionUrl("https://v8.clubemusa.com.br")
            .build();
    when(experimentRepository.findById(90L)).thenReturn(Optional.of(experiment));
    when(pdeProductionSlotRepository.findByProductSlugOrderBySlotCodeAsc(product.getSlug()))
        .thenReturn(
            List.of(
                activeSlot("v5", "https://v5.clubemusa.com.br", 74L),
                activeSlot("v7", "https://v7.clubemusa.com.br", 91L)));

    assertThatThrownBy(() -> service.create(90L))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("correspondente ao destino");
    verify(paymentsClient, never()).createCommercialProductCheckout(any());
  }

  /** Monta um slot ativo e validado para cenários com versões paralelas do PDE. */
  private PdeProductionSlot activeSlot(String slotCode, String publicUrl, Long experimentId) {
    return PdeProductionSlot.builder()
        .productSlug("metodo-musa-7-dias")
        .slotCode(slotCode)
        .publicUrl(publicUrl)
        .status(PdeProductionSlotStatus.ACTIVE)
        .validationStatus("OK")
        .sourceExperimentId(experimentId)
        .build();
  }
}
