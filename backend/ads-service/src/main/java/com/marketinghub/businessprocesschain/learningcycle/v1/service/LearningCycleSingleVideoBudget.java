package com.marketinghub.businessprocesschain.learningcycle.v1.service;

import static com.marketinghub.businessprocesschain.learningcycle.v1.service.LearningCycleRules.require;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.businessprocesschain.learningcycle.v1.*;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.singleVideoBudget.*;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleEventRepository;
import com.marketinghub.repository.jpa.salesvideo.VideoProjectRepository;
import com.marketinghub.salesvideo.tenant.TenantContextHolder;
import java.time.Instant;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Registra o teto incremental de uma produção sem ampliar os tetos históricos ou executar gasto.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LearningCycleSingleVideoBudget {
  private static final String ACTION = "AUTHORIZE_SINGLE_VIDEO";
  public static final String SCOPE = "SINGLE_VIDEO_INCREMENTAL_ALL_IN_V1";
  private final LearningSalesCycleEventRepository events;
  private final VideoProjectRepository projects;
  private final com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository cycles;
  private final LearningCycleJson json;

  /** Lê recibos explícitos; ausência de prontidão financeira nunca se transforma em saldo livre. */
  public SingleVideoBudgetResponse read(LearningSalesCycle cycle) {
    return read(cycle, TenantContextHolder.requireTenant());
  }

  /** Projeta somente os recibos do tenant explicitamente resolvido pelo contexto persistido. */
  private SingleVideoBudgetResponse read(LearningSalesCycle cycle, String tenantId) {
    var history = events.findByCycleIdAndActionOrderByRevisionDesc(cycle.getId(), ACTION);
    var latestByProject = new HashSet<Long>();
    var receipts = new ArrayList<SingleVideoBudgetResponse.Authorization>();
    for (var event : history) {
      var proof = json.read(event.getEvidenceJson());
      if (!Objects.equals(tenantId, proof.path("tenantId").asText())) continue;
      boolean current =
          "OPEN".equals(cycle.getStatus())
              && Objects.equals(cycle.getProductVersion(), proof.path("productVersion").asText())
              && Objects.equals(cycle.getExperimentId(), proof.path("experimentId").asLong())
              && Objects.equals(cycle.getProductId(), proof.path("productId").asLong())
              && Objects.equals(
                  cycle.getChainDefinitionId(), proof.path("chainDefinitionId").asLong())
              && SCOPE.equals(proof.path("scope").asText())
              && "USD".equals(proof.path("currency").asText())
              && (cycle.getVersionChangedAt() == null
                  || !event.getCreatedAt().isBefore(cycle.getVersionChangedAt()))
              && currentProject(cycle, proof, tenantId)
              && latestByProject.add(proof.path("videoProjectId").asLong());
      receipts.add(
          new SingleVideoBudgetResponse.Authorization(
              event.getId(),
              event.getEvidenceReference(),
              proof.path("videoProjectId").asLong(),
              proof.path("profileId").asLong(),
              proof.path("totalLimitUsd").decimalValue(),
              event.getOperatorName(),
              proof.path("authorizationSource").asText(),
              event.getCreatedAt(),
              current));
    }
    boolean open = "OPEN".equals(cycle.getStatus());
    return new SingleVideoBudgetResponse(
        cycle.getProductId(),
        cycle.getChainDefinitionId(),
        cycle.getId(),
        cycle.getExperimentId(),
        cycle.getProductVersion(),
        cycle.getRevision(),
        SCOPE,
        "USD",
        open,
        open ? null : "Ciclo encerrado: autorização disponível apenas para consulta.",
        false,
        "O recibo não libera consumo. Antes de cada custo, é necessária cobertura integral de agentes, imagens, clipes, voz, trilha, montagem, revisões, taxas e tentativas, com reservas e conciliação no mesmo teto.",
        List.copyOf(receipts));
  }

  /** Invalida a vigência se o projeto, perfil ou tenant mudar depois do recibo. */
  private boolean currentProject(
      LearningSalesCycle cycle, com.fasterxml.jackson.databind.JsonNode proof, String tenantId) {
    return projects
        .findById(proof.path("videoProjectId").asLong())
        .filter(
            project ->
                Objects.equals(project.getProductId(), cycle.getProductId())
                    && Objects.equals(project.getExperimentId(), cycle.getExperimentId())
                    && Objects.equals(project.getCampaignKey(), cycle.getProductVersion())
                    && Objects.equals(
                        project.getSalesVideoProfileId(), proof.path("profileId").asLong())
                    && Objects.equals(project.getTenantId(), tenantId))
        .isPresent();
  }

  /**
   * Entrega a Plutus o recibo do projeto persistido, sem inferir saldo ou substituir seu parecer.
   */
  public SingleVideoBudgetResponse.Authorization forProject(
      com.marketinghub.salesvideo.VideoProject project) {
    if (project == null || project.getExperimentId() == null || project.getTenantId() == null)
      return null;
    var cycle =
        cycles
            .findByExperimentId(project.getExperimentId())
            .filter(
                c ->
                    Objects.equals(c.getProductId(), project.getProductId())
                        && Objects.equals(c.getProductVersion(), project.getCampaignKey()))
            .orElse(null);
    if (cycle == null) return null;
    return read(cycle, project.getTenantId()).authorizations().stream()
        .filter(
            a ->
                a.current()
                    && Objects.equals(a.videoProjectId(), project.getId())
                    && Objects.equals(a.profileId(), project.getSalesVideoProfileId()))
        .findFirst()
        .orElse(null);
  }

  /**
   * Mantém proteção mesmo após STOP, fechamento ou mudança de versão; recibo vencido não vira
   * ausência de teto.
   */
  public SingleVideoBudgetResponse.Authorization protectedReceipt(
      com.marketinghub.salesvideo.VideoProject project) {
    if (project == null || project.getExperimentId() == null || project.getTenantId() == null)
      return null;
    var cycle = cycles.findByExperimentId(project.getExperimentId()).orElse(null);
    if (cycle == null) return null;
    return read(cycle, project.getTenantId()).authorizations().stream()
        .filter(a -> Objects.equals(a.videoProjectId(), project.getId()))
        .findFirst()
        .orElse(null);
  }

  /**
   * Expõe o bloqueio real enquanto os executores não oferecem limite compartilhado antes do gasto.
   */
  public String paidExecutionBlocker(com.marketinghub.salesvideo.VideoProject project) {
    var receipt = forProject(project);
    if (receipt == null) return null;
    return "Recibo #"
        + receipt.eventId()
        + ": US$ "
        + receipt.totalLimitUsd()
        + " no total para um vídeo. A reserva de clipes e a previsão integral não limitam as chamadas de agentes e revisões: os executores atuais contabilizam tokens depois da execução. Produção pausada antes de Plutus até existir controle compartilhado comprovado em todos os executores; não há nova aprovação de teto pendente.";
  }

  /** Persiste decisão idempotente sob o lock do ciclo, preservando histórico e confirmações. */
  public void authorize(LearningSalesCycle cycle, SingleVideoBudgetRequest request, Instant now) {
    require(request != null && request.requestKey() != null, "Informe a identidade da decisão.");
    require(
        Objects.equals(cycle.getChainDefinitionId(), request.chainDefinitionId())
            && Objects.equals(cycle.getExperimentId(), request.experimentId())
            && Objects.equals(cycle.getProductVersion(), request.productVersion()),
        "Autorização pertence a outro contexto.");
    var project =
        projects
            .findById(request.videoProjectId())
            .orElseThrow(
                () ->
                    new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Projeto não encontrado."));
    require(
        Objects.equals(TenantContextHolder.requireTenant(), project.getTenantId()),
        "Projeto não pertence ao tenant ativo.");
    require(
        Objects.equals(cycle.getProductId(), project.getProductId())
            && Objects.equals(cycle.getExperimentId(), project.getExperimentId())
            && Objects.equals(cycle.getProductVersion(), project.getCampaignKey())
            && Objects.equals(request.profileId(), project.getSalesVideoProfileId()),
        "Projeto ou perfil não pertence à versão deste ciclo.");
    String input = json.write(request);
    var previous =
        events.findByCycleIdAndRequestKey(cycle.getId(), request.requestKey().toString());
    if (previous.isPresent()) {
      require(
          ACTION.equals(previous.get().getAction())
              && json.read(input).equals(json.read(previous.get().getRequestJson())),
          "Chave de decisão já usada com outros valores.");
      return;
    }
    require(
        "OPEN".equals(cycle.getStatus()),
        "Ciclo encerrado: não é possível autorizar consumo novo.");
    require(
        Objects.equals(cycle.getRevision(), request.expectedRevision()),
        "O ciclo mudou. Atualize a leitura antes de registrar.");
    require(
        Boolean.TRUE.equals(request.confirmed()),
        "Confirme o teto integral para um único vídeo novo.");
    require(
        request.totalLimitUsd() != null
            && request.totalLimitUsd().signum() > 0
            && request.totalLimitUsd().scale() <= 2
            && request.totalLimitUsd().compareTo(new java.math.BigDecimal("999999.99")) <= 0,
        "Informe teto positivo em USD, com até duas casas decimais.");
    require(
        request.operatorName() != null
            && !request.operatorName().isBlank()
            && request.operatorName().length() <= 160
            && request.authorizationSource() != null
            && !request.authorizationSource().isBlank()
            && request.authorizationSource().length() <= 4000,
        "Informe o operador real e a origem da autorização do cliente.");
    var existing =
        read(cycle).authorizations().stream()
            .filter(a -> Objects.equals(a.videoProjectId(), request.videoProjectId()))
            .findFirst();
    require(
        existing.isEmpty(),
        "Este projeto já possui um recibo. Reutilize sua referência; não abra outro teto para retries.");
    cycle.setRevision(cycle.getRevision() + 1);
    cycle.setUpdatedAt(now);
    var event = new LearningSalesCycleEvent();
    event.setCycleId(cycle.getId());
    event.setRevision(cycle.getRevision());
    event.setRequestKey(request.requestKey().toString());
    event.setRequestJson(input);
    event.setFromStage(cycle.getStage());
    event.setToStage(cycle.getStage());
    event.setAction(ACTION);
    event.setOperatorName(request.operatorName().trim());
    event.setSummary(
        "Teto incremental integral de USD "
            + request.totalLimitUsd().toPlainString()
            + " para um único vídeo no projeto #"
            + project.getId()
            + ", incluindo agentes, revisões e tentativas. Histórico não autorizado.");
    event.setEvidenceReference(
        "internal://learning-cycles/"
            + cycle.getId()
            + "/single-video-budget/"
            + request.requestKey());
    ObjectNode proof = (ObjectNode) json.read(input);
    proof.put("scope", SCOPE);
    proof.put("currency", "USD");
    proof.put("productId", cycle.getProductId());
    proof.put("tenantId", project.getTenantId());
    proof.put("videoCount", 1);
    proof.put("historicalCostsAuthorized", false);
    proof.put("mediaAuthorized", false);
    proof.put("commercialPublicationAuthorized", false);
    proof.put("financialReviewApproved", false);
    proof
        .putArray("includedCosts")
        .add("AGENTS_AND_REVIEWS")
        .add("IMAGES")
        .add("CLIPS")
        .add("VOICE")
        .add("MUSIC")
        .add("ASSEMBLY")
        .add("FEES")
        .add("RETRIES");
    event.setEvidenceJson(json.write(proof));
    event.setCreatedAt(now);
    events.saveAndFlush(event);
    log.info(
        "Autorização incremental registrada cycleId={} projectId={} eventId={} totalUsd={}",
        cycle.getId(),
        project.getId(),
        event.getId(),
        request.totalLimitUsd());
  }
}
