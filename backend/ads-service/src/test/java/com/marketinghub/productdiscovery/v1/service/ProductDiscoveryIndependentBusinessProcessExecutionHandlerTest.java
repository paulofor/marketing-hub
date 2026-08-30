package com.marketinghub.productdiscovery.v1.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.productdiscovery.v1.ProductDiscoveryCycleStatus;
import com.marketinghub.productdiscovery.v1.ProductDiscoveryMarketType;
import com.marketinghub.productdiscovery.v1.ProductDiscoveryResearchMode;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Responsabilidade: comprovar que o cockpit reutiliza o ciclo canônico de descoberta PDE. */
class ProductDiscoveryIndependentBusinessProcessExecutionHandlerTest {

  /** Mapeia o briefing normalizado e devolve a mesma referência usada pela auditoria BPM. */
  @Test
  void startsProductDiscoveryCycleThroughCanonicalService() throws Exception {
    ProductDiscoveryService service = mock(ProductDiscoveryService.class);
    when(service.createCycle(any()))
        .thenReturn(
            new ProductDiscoveryCycleResponse(
                77L,
                "agenda vazia para manicures",
                "Manicures autônomas",
                "BR",
                "pt-BR",
                "Instagram",
                ProductDiscoveryCycleStatus.READY_FOR_RESEARCH,
                "research",
                null,
                null,
                Instant.parse("2026-08-30T14:00:00Z"),
                Instant.parse("2026-08-30T14:00:00Z")));
    var handler = new ProductDiscoveryIndependentBusinessProcessExecutionHandler(service);
    var input =
        new ObjectMapper()
            .readTree(
                "{\"researchMode\":\"DISCOVER_MARKETS\",\"marketType\":\"B2C\",\"theme\":\"agenda vazia para manicures\",\"targetAudience\":\"Manicures autônomas\",\"country\":\"BR\",\"language\":\"pt-BR\",\"acquisitionChannel\":\"Instagram\",\"referenceSources\":\"https://example.com/revista\"}");

    var result = handler.start(input);

    ArgumentCaptor<CreateProductDiscoveryCycleRequest> request =
        ArgumentCaptor.forClass(CreateProductDiscoveryCycleRequest.class);
    verify(service).createCycle(request.capture());
    assertThat(request.getValue().theme()).isEqualTo("agenda vazia para manicures");
    assertThat(request.getValue().targetAudience()).isEqualTo("Manicures autônomas");
    assertThat(request.getValue().researchMode())
        .isEqualTo(ProductDiscoveryResearchMode.DISCOVER_MARKETS);
    assertThat(request.getValue().marketType()).isEqualTo(ProductDiscoveryMarketType.B2C);
    assertThat(request.getValue().referenceSources()).isEqualTo("https://example.com/revista");
    assertThat(result.sourceReference()).isEqualTo("product-discovery-cycle:77");
    assertThat(result.displayName()).isEqualTo("agenda vazia para manicures");
  }

  /** Declara campos obrigatórios e defaults de país e idioma para a tela dinâmica. */
  @Test
  void publishesDynamicInputContract() {
    var handler =
        new ProductDiscoveryIndependentBusinessProcessExecutionHandler(
            mock(ProductDiscoveryService.class));

    assertThat(handler.inputFields()).hasSize(11);
    assertThat(handler.inputFields())
        .anySatisfy(
            field -> {
              assertThat(field.key()).isEqualTo("theme");
              assertThat(field.required()).isTrue();
            })
        .anySatisfy(
            field -> {
              assertThat(field.key()).isEqualTo("country");
              assertThat(field.defaultValue()).isEqualTo("BR");
            })
        .anySatisfy(
            field -> {
              assertThat(field.key()).isEqualTo("researchMode");
              assertThat(field.controlType()).isEqualTo("SELECT");
              assertThat(field.defaultValue()).isEqualTo("DISCOVER_MARKETS");
              assertThat(field.options())
                  .extracting("value")
                  .containsExactly("DISCOVER_MARKETS", "VALIDATE_MARKET");
            });
  }
}
