package com.marketinghub.gerasalespage.v1.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.gerasalespage.v1.GeraSalesPagePublicationAudit;
import com.marketinghub.gerasalespage.v1.service.republish.RepublishPublicationRequest;
import com.marketinghub.gerasalespage.v1.web.GeraSalesPageController;
import com.marketinghub.leadportal.LeadPortalFlow;
import com.marketinghub.leadportal.integration.LeadPortalFlowPublisher;
import com.marketinghub.leadportal.integration.LeadPortalPublicationException;
import com.marketinghub.leadportal.support.LeadPortalPublicUrlResolver;
import com.marketinghub.planning.service.CommercialPlanLandingAssetService;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.gerasalespage.v1.*;
import com.marketinghub.repository.jpa.leadportal.LeadPortalFlowRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: provar recuperação sem geração, perda de histórico ou troca de versão. */
class GeraSalesPagePublicationRecoveryTest {
  final ExperimentRepository experiments = mock(ExperimentRepository.class);
  final GeraSalesPageStageExecutionRepository executions =
      mock(GeraSalesPageStageExecutionRepository.class);
  final GeraSalesPagePublicationAuditRepository publications =
      mock(GeraSalesPagePublicationAuditRepository.class);
  final GeraSalesPagePublicationStageAuditRepository stages =
      mock(GeraSalesPagePublicationStageAuditRepository.class);
  final LeadPortalFlowRepository flows = mock(LeadPortalFlowRepository.class);
  final LeadPortalFlowPublisher publisher = mock(LeadPortalFlowPublisher.class);
  final LeadPortalPublicUrlResolver urls = mock(LeadPortalPublicUrlResolver.class);
  final CommercialPlanLandingAssetService assets = mock(CommercialPlanLandingAssetService.class);
  final GeraSalesPagePublicationAuditService service =
      new GeraSalesPagePublicationAuditService(
          experiments,
          executions,
          publications,
          stages,
          flows,
          publisher,
          urls,
          assets,
          new ObjectMapper());
  final Experiment experiment = new Experiment();
  final LeadPortalFlow flow = new LeadPortalFlow();
  final GeraSalesPagePublicationAudit audit = new GeraSalesPagePublicationAudit();

  /** Prepara fonte sintética exatamente igual à página previamente aprovada. */
  @BeforeEach
  void setup() {
    configure(701L);
  }

  /** Varia as identidades sem alterar o contrato genérico de recuperação. */
  private void configure(long id) {
    experiment.setId(id);
    experiment.setFollowUpActionUrl("https://example.test/flows/exp-" + id + "-gerasalespage-v1");
    experiment.setLeadPortalFlow(flow);
    flow.setId(id + 100);
    flow.setExperiment(experiment);
    flow.setSlug("exp-" + id + "-gerasalespage-v1");
    flow.setName("Kit de teste local " + id);
    flow.setModel("GERA_SALES_PAGE_V1_STANDALONE");
    flow.setApproved(true);
    audit.setId(id + 200);
    audit.setExperimentId(id);
    audit.setSalesPageUrl(experiment.getFollowUpActionUrl());
    audit.setPublicationJobId("audited-job-" + id);
    audit.setPublishedAt(Instant.parse("2026-08-10T00:00:00Z"));
    audit.setHtml(
        "<!doctype html><html><head><title>Kit "
            + id
            + "</title></head><body><h1>Kit aprovado "
            + id
            + "</h1><a href='https://checkout.test'>Comprar</a></body></html>");
    flow.setCustomFormHtml(audit.getHtml());
    when(experiments.findById(id)).thenReturn(Optional.of(experiment));
    when(experiments.findForSalesPageRecovery(id)).thenReturn(Optional.of(experiment));
    when(publications.findById(audit.getId())).thenReturn(Optional.of(audit));
    when(publications.findTopByExperimentIdOrderByPublishedAtDesc(id))
        .thenReturn(Optional.of(audit));
    when(flows.findBySlug(flow.getSlug())).thenReturn(Optional.of(flow));
    when(urls.resolve(flow)).thenReturn(audit.getSalesPageUrl());
    when(publisher.isAvailable()).thenReturn(true);
  }

  /** Reproduz a falta de marcador e conserva auditoria, versão e conteúdo em novas execuções. */
  @ParameterizedTest
  @ValueSource(longs = {701, 9843})
  void recoversLegacyPublicationWithoutGeneratingOrChangingHistory(long id) throws Exception {
    configure(id);
    String original = audit.getHtml();
    var view = service.recovery(id, audit.getId());
    assertThat(view.available()).isTrue();
    assertThat(view.submittedAt()).isNull();
    verify(publisher, never()).publish(any());
    var request = new RepublishPublicationRequest(view.sourceSha256());
    var submitted = service.republish(id, audit.getId(), request);
    String sent = flow.getCustomFormHtml();
    service.republish(id, audit.getId(), request);
    assertThat(flow.getCustomFormHtml()).isEqualTo(sent);
    assertThat(sent)
        .contains("name=\"mh-publication-source-sha256\" content=\"" + hash(original) + "\"");
    assertThat(
            sent.replaceFirst(
                "\\n<meta name=\"mh-publication-source-sha256\" content=\"[a-f0-9]{64}\">", ""))
        .isEqualTo(original);
    assertThat(audit.getHtml()).isEqualTo(original);
    assertThat(audit.getPublishedAt()).isEqualTo(Instant.parse("2026-08-10T00:00:00Z"));
    assertThat(submitted.submittedAt()).isNotNull();
    assertThat(experiment.getFollowUpActionUrl()).isEqualTo(audit.getSalesPageUrl());
    verify(publisher, times(2)).publish(flow);
    verify(experiments, times(2)).findForSalesPageRecovery(id);
    verify(publications, never()).save(any());
    verifyNoInteractions(executions, stages);
  }

  /** Impede usar uma publicação pertencente a outro experimento. */
  @Test
  void rejectsForeignPublication() {
    audit.setExperimentId(999L);
    assertThatThrownBy(() -> service.recovery(experiment.getId(), audit.getId()))
        .isInstanceOf(jakarta.persistence.EntityNotFoundException.class);
    verify(publisher, never()).publish(any());
  }

  /** Recusa versão antiga, HTML modificado, vínculo/destino trocado e integração desligada. */
  @ParameterizedTest
  @ValueSource(
      strings = {
        "old",
        "html",
        "destination",
        "owner",
        "approval",
        "integration",
        "flow",
        "format",
        "empty"
      })
  void blocksUnsafeRecoveryBeforeSending(String change) {
    switch (change) {
      case "old" ->
          when(publications.findTopByExperimentIdOrderByPublishedAtDesc(experiment.getId()))
              .thenReturn(Optional.of(GeraSalesPagePublicationAudit.builder().id(12345L).build()));
      case "html" -> flow.setCustomFormHtml("<html>nova página não auditada</html>");
      case "destination" -> experiment.setFollowUpActionUrl("https://example.test/another-page");
      case "owner" -> flow.setExperiment(new Experiment());
      case "approval" -> flow.setApproved(false);
      case "integration" -> when(publisher.isAvailable()).thenReturn(false);
      case "flow" -> experiment.setLeadPortalFlow(null);
      case "format" -> flow.setModel("PERSONALIZED_SAMPLE");
      case "empty" -> audit.setHtml("");
      default -> throw new AssertionError(change);
    }
    var view = service.recovery(experiment.getId(), audit.getId());
    assertThat(view.available()).isFalse();
    assertThat(view.reason()).isNotBlank();
    assertThatThrownBy(
            () ->
                service.republish(
                    experiment.getId(),
                    audit.getId(),
                    new RepublishPublicationRequest("a".repeat(64))))
        .isInstanceOf(ResponseStatusException.class);
    verify(publisher, never()).publish(any());
    verify(flows, never()).save(any());
  }

  /** Exige o hash confirmado na consulta antes de qualquer envio. */
  @Test
  void rejectsChangedIdentityBeforeSending() {
    assertThatThrownBy(
            () ->
                service.republish(
                    experiment.getId(),
                    audit.getId(),
                    new RepublishPublicationRequest("a".repeat(64))))
        .hasMessageContaining("identidade da publicação mudou");
    verify(publisher, never()).publish(any());
  }

  /** Reconfere direitos dos ativos atuais antes de reenviar o snapshot aprovado. */
  @Test
  void respectsRevokedAssetApproval() {
    doThrow(
            new ResponseStatusException(
                org.springframework.http.HttpStatus.CONFLICT, "Ativo revogado"))
        .when(assets)
        .validateApprovedAssetReferences(anyLong(), anyString());
    var view = service.recovery(experiment.getId(), audit.getId());
    assertThatThrownBy(
            () ->
                service.republish(
                    experiment.getId(),
                    audit.getId(),
                    new RepublishPublicationRequest(view.sourceSha256())))
        .hasMessageContaining("Ativo revogado");
    verify(publisher, never()).publish(any());
  }

  /**
   * Preserva entrada e histórico se o portal não confirmar o envio e permite retentativa segura.
   */
  @Test
  void retriesSamePayloadAfterPortalFailure() {
    var request =
        new RepublishPublicationRequest(
            service.recovery(experiment.getId(), audit.getId()).sourceSha256());
    doThrow(new LeadPortalPublicationException("indisponível"))
        .doNothing()
        .when(publisher)
        .publish(flow);
    assertThatThrownBy(() -> service.republish(experiment.getId(), audit.getId(), request))
        .hasMessageContaining("Não foi possível reenviar");
    assertThat(flow.getCustomFormHtml()).isEqualTo(audit.getHtml());
    verify(flows, never()).save(any());
    service.republish(experiment.getId(), audit.getId(), request);
    verify(publisher, times(2)).publish(flow);
    verify(flows).save(flow);
    verify(publications, never()).save(any());
  }

  /** Exercita o controller real e a validação HTTP do hash imutável. */
  @Test
  void exposesValidatedRecoveryContract() throws Exception {
    var mvc =
        MockMvcBuilders.standaloneSetup(
                new GeraSalesPageController(mock(GeraSalesPageStageService.class), service))
            .build();
    String base = "/api/experiments/701/gerasalespage/v1/publications/901";
    mvc.perform(get(base + "/recovery"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.available").value(true));
    mvc.perform(post(base + "/republish").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isBadRequest());
    verify(publisher, never()).publish(any());
    mvc.perform(
            post(base + "/republish")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"expectedSourceSha256\":\"" + hash(audit.getHtml()) + "\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.submittedAt").exists());
  }

  /** Calcula a identidade esperada independentemente do serviço de publicação. */
  private String hash(String value) throws Exception {
    return HexFormat.of()
        .formatHex(
            MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
  }
}
