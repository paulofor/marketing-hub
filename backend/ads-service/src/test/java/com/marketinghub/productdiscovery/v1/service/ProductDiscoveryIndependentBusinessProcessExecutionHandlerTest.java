package com.marketinghub.productdiscovery.v1.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.productdiscovery.v1.ProductDiscoveryCycleStatus;
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
                "{\"theme\":\"agenda vazia para manicures\",\"targetAudience\":\"Manicures autônomas\",\"country\":\"BR\",\"language\":\"pt-BR\",\"acquisitionChannel\":\"Instagram\"}");

    var result = handler.start(input);

    ArgumentCaptor<CreateProductDiscoveryCycleRequest> request =
        ArgumentCaptor.forClass(CreateProductDiscoveryCycleRequest.class);
    verify(service).createCycle(request.capture());
    assertThat(request.getValue().theme()).isEqualTo("agenda vazia para manicures");
    assertThat(request.getValue().targetAudience()).isEqualTo("Manicures autônomas");
    assertThat(result.sourceReference()).isEqualTo("product-discovery-cycle:77");
    assertThat(result.displayName()).isEqualTo("agenda vazia para manicures");
  }

  /** Declara campos obrigatórios e defaults de país e idioma para a tela dinâmica. */
  @Test
  void publishesDynamicInputContract() {
    var handler =
        new ProductDiscoveryIndependentBusinessProcessExecutionHandler(
            mock(ProductDiscoveryService.class));

    assertThat(handler.inputFields()).hasSize(8);
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
            });
  }
}
