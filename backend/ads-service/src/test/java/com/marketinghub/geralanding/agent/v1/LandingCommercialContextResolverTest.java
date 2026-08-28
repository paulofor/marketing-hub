package com.marketinghub.geralanding.agent.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.product.Product;
import com.marketinghub.product.service.commercialoffer.PublicProductCommercialOfferResponse;
import com.marketinghub.product.service.commercialoffer.PublicProductCommercialOfferService;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: validar o contexto comercial confiável entregue ao gerador de landing. */
class LandingCommercialContextResolverTest {

  /** Deve expor escopo, identidade comercial mínima e políticas do mesmo experimento. */
  @Test
  void shouldExposeStructuredServiceAndTrustContract() {
    PublicProductCommercialOfferService offers = mock(PublicProductCommercialOfferService.class);
    Product product = product();
    Experiment experiment = experiment(product);
    when(offers.getOffer("kit-whatsapp-pronto")).thenReturn(offer(89L));

    Map<String, Object> context =
        new LandingCommercialContextResolver(new ObjectMapper(), offers).resolve(experiment);

    assertThat(context.get("targetAudience")).isEqualTo("Prestadores de serviço");
    assertThat(context.get("productFormat")).isEqualTo("Serviço personalizado");
    assertThat(context.get("serviceExperienceContract").toString())
        .contains("10 a 20 respostas", "Briefing guiado");
    assertThat(context.get("commercialTrustContract").toString())
        .contains("AVAILABLE", "Digicom Digital", "/privacy")
        .doesNotContain("legalName", "address");
  }

  /** Deve tornar indisponível a confiança pública sem impedir a criação do primeiro rascunho. */
  @Test
  void shouldExposeAuditableBlockWhenPublicOfferIsUnavailable() {
    PublicProductCommercialOfferService offers = mock(PublicProductCommercialOfferService.class);
    Product product = product();
    Experiment experiment = experiment(product);
    when(offers.getOffer("kit-whatsapp-pronto"))
        .thenThrow(
            new ResponseStatusException(
                HttpStatus.PRECONDITION_FAILED, "Produto sem slot PDE pronto."));

    Map<String, Object> context =
        new LandingCommercialContextResolver(new ObjectMapper(), offers).resolve(experiment);

    assertThat(context.get("commercialTrustContract").toString())
        .contains("UNAVAILABLE", "Produto sem slot PDE pronto.");
  }

  /** Deve recusar políticas de uma oferta pública ligada a outro experimento. */
  @Test
  void shouldRejectPublicTrustContractFromDifferentExperiment() {
    PublicProductCommercialOfferService offers = mock(PublicProductCommercialOfferService.class);
    Product product = product();
    Experiment experiment = experiment(product);
    when(offers.getOffer("kit-whatsapp-pronto")).thenReturn(offer(88L));

    Map<String, Object> context =
        new LandingCommercialContextResolver(new ObjectMapper(), offers).resolve(experiment);

    assertThat(context.get("commercialTrustContract").toString())
        .contains("UNAVAILABLE", "outro experimento");
  }

  /** Monta um produto mínimo com contrato PDE para os cenários de contexto. */
  private Product product() {
    return Product.builder()
        .id(9L)
        .slug("kit-whatsapp-pronto")
        .targetAudience("Prestadores de serviço")
        .productFormat("Serviço personalizado")
        .deliveryMode("Assistida")
        .valueUnit("Um atendimento implantado")
        .pdeExperienceJson(
            "{\"serviceScope\":{\"includedItems\":[\"10 a 20 respostas\"]},"
                + "\"commercialProcess\":[{\"title\":\"Briefing guiado\"}]}")
        .build();
  }

  /** Monta o experimento comercial que será apresentado ao agente. */
  private Experiment experiment(Product product) {
    Experiment experiment = new Experiment();
    experiment.setId(89L);
    experiment.setProduct(product);
    return experiment;
  }

  /** Monta a oferta pública canônica usada para validar a vinculação e a confiança. */
  private PublicProductCommercialOfferResponse offer(Long experimentId) {
    return new PublicProductCommercialOfferResponse(
        "kit-whatsapp-pronto",
        "kit-whatsapp-pronto-pde-v2",
        "assisted-service-v2",
        experimentId,
        "PLANNED",
        "DIRECT_ONE_TO_ONE",
        "Dor",
        "Prova",
        "Promessa",
        "Quero meu atendimento sob medida",
        BigDecimal.valueOf(349),
        "https://checkout.example/rigel",
        "https://kit.example",
        "Prestadores de serviço",
        "Serviço personalizado",
        "Assistida",
        "Um atendimento implantado",
        "Digicom Digital",
        "00.000.000/0001-00",
        "teste@sandbox.local",
        "https://kit.example/terms",
        "https://kit.example/privacy",
        "https://kit.example/refund-policy");
  }
}
