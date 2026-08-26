package com.marketinghub.geralanding.agent.v1;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.geralanding.qualityreview.service.BackendQualityReviewService;
import com.marketinghub.gerasalespage.v1.GeraSalesPagePublicationAudit;
import com.marketinghub.planning.service.CommercialPlanLandingAssetService;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.gerasalespage.v1.GeraSalesPagePublicationAuditRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Responsabilidade: validar o gate do HTML integral produzido por Dédalo. */
class GovernedLandingHtmlServiceTest {
  private ExperimentRepository repository;
  private BackendQualityReviewService qualityReviewService;
  private GeraSalesPagePublicationAuditRepository publicationRepository;
  private CommercialPlanLandingAssetService landingAssetService;
  private GovernedLandingHtmlService service;
  private Experiment experiment;

  /** Prepara um experimento com contrato comercial protegido. */
  @BeforeEach
  void setUp() {
    repository = mock(ExperimentRepository.class);
    qualityReviewService = mock(BackendQualityReviewService.class);
    publicationRepository = mock(GeraSalesPagePublicationAuditRepository.class);
    landingAssetService = mock(CommercialPlanLandingAssetService.class);
    service =
        new GovernedLandingHtmlService(
            repository,
            new LandingCheckoutContractResolver(publicationRepository),
            qualityReviewService,
            landingAssetService);
    experiment = mock(Experiment.class);
    when(repository.findById(88L)).thenReturn(Optional.of(experiment));
    when(experiment.getPrimaryCta()).thenReturn("Comprar o kit por R$ 67");
    when(experiment.getId()).thenReturn(88L);
    when(experiment.getHtmlGeraLanding())
        .thenReturn(html("#checkout_oficial", "Comprar o kit por R$ 67"));
  }

  /** Permite substituir âncora quebrada somente pelo checkout canônico publicado. */
  @Test
  void replacesBrokenAnchorWithCanonicalPublishedCheckout() {
    String canonicalCheckout = "https://www.mercadopago.com.br/checkout/v1/redirect?pref_id=88";
    GeraSalesPagePublicationAudit publication = new GeraSalesPagePublicationAudit();
    publication.setCheckoutUrl(canonicalCheckout);
    when(publicationRepository.findTopByExperimentIdOrderByPublishedAtDesc(88L))
        .thenReturn(Optional.of(publication));

    service.apply(88L, html(canonicalCheckout, "Comprar o kit por R$ 67"));

    verify(experiment)
        .setHtmlGeraLanding(html(canonicalCheckout, "Comprar o kit por R$ 67").trim());
  }

  /** Permite a primeira landing quando o checkout comercial já está congelado no experimento. */
  @Test
  void acceptsCommercialCheckoutBeforeFirstPublication() {
    String canonicalCheckout = "https://www.mercadopago.com.br/checkout/rigel";
    when(experiment.getCommercialCheckoutUrl()).thenReturn(canonicalCheckout);
    when(experiment.getHtmlGeraLanding()).thenReturn(null);

    service.apply(88L, html(canonicalCheckout, "Comprar o kit por R$ 67"));

    verify(experiment)
        .setHtmlGeraLanding(html(canonicalCheckout, "Comprar o kit por R$ 67").trim());
    verify(qualityReviewService).reviewAfterHtmlGeneration(experiment);
  }

  /** Aceita o CTA protegido quando um gerador válido escreve href antes de id. */
  @Test
  void acceptsCheckoutAttributesInAnyHtmlOrder() {
    String canonicalCheckout = "https://www.mercadopago.com.br/checkout/v1/redirect?pref_id=88";
    GeraSalesPagePublicationAudit publication = new GeraSalesPagePublicationAudit();
    publication.setCheckoutUrl(canonicalCheckout);
    when(publicationRepository.findTopByExperimentIdOrderByPublishedAtDesc(88L))
        .thenReturn(Optional.of(publication));
    String candidate =
        html(canonicalCheckout, "Comprar o kit por R$ 67")
            .replace(
                "id=\"checkout-cta-primary\" href=\"" + canonicalCheckout + "\"",
                "href=\"" + canonicalCheckout + "\" id=\"checkout-cta-primary\"");

    service.apply(88L, candidate);

    verify(experiment).setHtmlGeraLanding(candidate.trim());
    verify(qualityReviewService).reviewAfterHtmlGeneration(experiment);
  }

  /** Aceita os múltiplos CTAs semânticos gerados por Dédalo quando todos preservam o checkout. */
  @Test
  void acceptsSemanticCheckoutRoleWithCanonicalDestination() {
    String canonicalCheckout = "https://www.mercadopago.com.br/checkout/v1/redirect?pref_id=88";
    GeraSalesPagePublicationAudit publication = new GeraSalesPagePublicationAudit();
    publication.setCheckoutUrl(canonicalCheckout);
    when(publicationRepository.findTopByExperimentIdOrderByPublishedAtDesc(88L))
        .thenReturn(Optional.of(publication));
    String candidate =
        html(canonicalCheckout, "Comprar o kit por R$ 67")
            .replace(
                "id=\"checkout-cta-primary\"",
                "data-analytics-role=\"primary-checkout\" data-cta-location=\"hero\"")
            .replace(
                "</body>",
                "<a data-analytics-role=\"primary-checkout\" href=\""
                    + canonicalCheckout
                    + "\">Comprar o kit por R$ 67</a></body>");

    service.apply(88L, candidate);

    verify(experiment).setHtmlGeraLanding(candidate.trim());
  }

  /** Bloqueia um CTA secundário que tente desviar o checkout apesar do CTA principal válido. */
  @Test
  void rejectsAnySemanticCheckoutWithDifferentDestination() {
    String canonicalCheckout = "https://www.mercadopago.com.br/checkout/v1/redirect?pref_id=88";
    GeraSalesPagePublicationAudit publication = new GeraSalesPagePublicationAudit();
    publication.setCheckoutUrl(canonicalCheckout);
    when(publicationRepository.findTopByExperimentIdOrderByPublishedAtDesc(88L))
        .thenReturn(Optional.of(publication));
    String candidate =
        html(canonicalCheckout, "Comprar o kit por R$ 67")
            .replace(
                "</body>",
                "<a data-analytics-role=\"primary-checkout\" href=\"https://outro\">"
                    + "Comprar o kit por R$ 67</a></body>");

    assertThatThrownBy(() -> service.apply(88L, candidate))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("checkout");
  }

  /** Persiste o documento seguro e abre revisão independente. */
  @Test
  void appliesFullHtmlAndRequestsIndependentReview() {
    String candidate = html("#checkout_oficial", "Comprar o kit por R$ 67");

    service.apply(88L, candidate);

    verify(experiment).setHtmlGeraLanding(candidate.trim());
    verify(repository).save(experiment);
    verify(qualityReviewService).reviewAfterHtmlGeneration(experiment);
  }

  /** Bloqueia mudança silenciosa do checkout protegido. */
  @Test
  void rejectsCheckoutContractChange() {
    assertThatThrownBy(() -> service.apply(88L, html("https://outro", "Comprar o kit por R$ 67")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("checkout");
  }

  /** Bloqueia scripts introduzidos pelo documento autônomo. */
  @Test
  void rejectsExecutableScript() {
    String candidate =
        html("#checkout_oficial", "Comprar o kit por R$ 67")
            .replace("</body>", "<script>alert(1)</script></body>");

    assertThatThrownBy(() -> service.apply(88L, candidate))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("execução");
  }

  /** Monta HTML completo com tamanho mínimo e CTA canônico. */
  private String html(String href, String cta) {
    return "<!doctype html><html><body><main>"
        + "conteúdo comercial ".repeat(40)
        + "</main><a id=\"checkout-cta-primary\" href=\""
        + href
        + "\">"
        + cta
        + "</a></body></html>";
  }
}
