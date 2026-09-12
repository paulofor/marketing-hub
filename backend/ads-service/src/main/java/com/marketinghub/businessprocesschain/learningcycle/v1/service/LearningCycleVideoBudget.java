package com.marketinghub.businessprocesschain.learningcycle.v1.service;

import static com.marketinghub.businessprocesschain.learningcycle.v1.service.LearningCycleRules.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.businessprocesschain.learningcycle.v1.*;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.videoBudget.*;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleEventRepository;
import java.time.Instant;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Responsabilidade: preservar o teto humano dos vídeos no ledger do ciclo sem executar consumo. */
@Component
@RequiredArgsConstructor
@Slf4j
public class LearningCycleVideoBudget {
  private static final String ACTION = "AUTHORIZE_VIDEO_BUDGET";
  private static final String SCOPE = "TWO_VIDEOS_PRODUCTION_AND_REVIEW";
  private final LearningSalesCycleEventRepository events;
  private final LearningCycleJson json;

  /** Projeta autorização vigente, bloqueios e recibos históricos com a identidade do ciclo. */
  public VideoBudgetResponse response(LearningSalesCycle cycle, Product product, boolean videos) {
    var history = history(cycle);
    var current =
        history.stream()
            .filter(VideoBudgetResponse.Authorization::current)
            .findFirst()
            .orElse(null);
    String blocker = blocker(cycle, videos);
    return new VideoBudgetResponse(
        cycle.getProductId(),
        product.getName(),
        product.getInternalName(),
        cycle.getChainDefinitionId(),
        cycle.getProcessDefinitionId(),
        cycle.getId(),
        cycle.getExperimentId(),
        cycle.getProductVersion(),
        cycle.getRevision(),
        label(cycle.getStage()),
        current == null ? "AWAITING_LIMIT" : "LIMIT_RECORDED",
        "Informe o teto total autorizado para produzir e revisar os dois vídeos: anúncio e demonstração na entrada. Isso permitirá prosseguir com a avaliação financeira, sem autorizar mídia, cobrança ou publicação comercial.",
        SCOPE,
        "USD",
        blocker == null,
        blocker,
        current,
        history,
        "/business-process-chains/learning-cycles?chainId="
            + cycle.getChainDefinitionId()
            + "&productId="
            + cycle.getProductId()
            + "&cycleId="
            + cycle.getId(),
        financeUrl(cycle));
  }

  /** Registra um recibo imutável sob o lock transacional adquirido pelo service canônico. */
  public void authorize(
      LearningSalesCycle cycle, AuthorizeVideoBudgetRequest request, boolean videos, Instant now) {
    require(
        Objects.equals(request.chainDefinitionId(), cycle.getChainDefinitionId())
            && Objects.equals(request.experimentId(), cycle.getExperimentId())
            && Objects.equals(request.productVersion(), cycle.getProductVersion()),
        "Produto, cadeia, experimento ou versão divergentes. Reabra o financeiro pelo ciclo correto.");
    String input = json.write(request);
    var replay = events.findByCycleIdAndRequestKey(cycle.getId(), request.requestKey().toString());
    if (replay.isPresent()) {
      require(
          ACTION.equals(replay.get().getAction())
              && json.read(input).equals(json.read(replay.get().getRequestJson())),
          "A chave desta autorização já foi usada com conteúdo diferente.");
      return;
    }
    require(
        cycle.getRevision() == request.expectedRevision(),
        "O ciclo mudou. Atualize a leitura antes de autorizar.");
    String blocker = blocker(cycle, videos);
    require(blocker == null, blocker);
    require(Boolean.TRUE.equals(request.confirmed()), "Confirme o escopo do teto das duas peças.");
    require(
        request.budgetLimitUsd() != null
            && request.budgetLimitUsd().signum() > 0
            && request.budgetLimitUsd().compareTo(new java.math.BigDecimal("999999.99")) <= 0
            && request.budgetLimitUsd().scale() <= 2,
        "Informe um teto positivo em USD com até duas casas decimais.");
    require(
        request.operatorName() != null
            && !request.operatorName().isBlank()
            && request.operatorName().length() <= 160
            && request.justification() != null
            && !request.justification().isBlank()
            && request.justification().length() <= 4000,
        "Informe responsável e justificativa dentro dos limites do formulário.");
    cycle.setRevision(cycle.getRevision() + 1);
    cycle.setUpdatedAt(now);
    var event = new LearningSalesCycleEvent();
    event.setCycleId(cycle.getId());
    event.setRequestKey(request.requestKey().toString());
    event.setRequestJson(input);
    event.setRevision(cycle.getRevision());
    event.setFromStage(cycle.getStage());
    event.setToStage(cycle.getStage());
    event.setAction(ACTION);
    event.setOperatorName(request.operatorName().trim());
    event.setSummary(
        "Teto total de USD "
            + request.budgetLimitUsd().toPlainString()
            + " registrado para produção e revisão do anúncio e da demonstração na entrada.");
    event.setEvidenceReference(
        "internal://learning-cycles/" + cycle.getId() + "/video-budget/" + request.requestKey());
    var proof = (ObjectNode) json.read(input);
    proof.put("scope", SCOPE);
    proof.put("currency", "USD");
    proof.put("productId", cycle.getProductId());
    proof.put("financialReviewApproved", false);
    proof.put("mediaAuthorized", false);
    proof.put("billingAuthorized", false);
    proof.put("commercialPublicationAuthorized", false);
    proof.putArray("videoRoles").add("AD").add("LANDING_HERO");
    event.setEvidenceJson(json.write(proof));
    event.setCreatedAt(now);
    events.saveAndFlush(event);
    log.info(
        "Financeiro de vídeos: teto registrado productId={} cycleId={} experimentId={} eventId={} revision={} budgetLimitUsd={}",
        cycle.getProductId(),
        cycle.getId(),
        cycle.getExperimentId(),
        event.getId(),
        cycle.getRevision(),
        request.budgetLimitUsd());
  }

  /** Entrega ao briefing o teto oficial, recusando referência alterada ou de outra versão. */
  public void attachToBrief(LearningSalesCycle cycle, JsonNode data) {
    var current = current(cycle);
    String reference = data.path("productionBudgetReference").asText();
    ((ObjectNode) data).remove("productionBudget");
    if (current == null) {
      require(
          !reference.startsWith("internal://learning-cycles/"),
          "A referência financeira não corresponde a um teto vigente desta versão.");
      return;
    }
    require(
        current.reference().equals(reference),
        "Use a referência do teto vigente exibida no financeiro dos vídeos.");
    var proof = ((ObjectNode) data).putObject("productionBudget");
    proof.put("reference", current.reference());
    proof.put("budgetLimitUsd", current.budgetLimitUsd());
    proof.put("scope", SCOPE);
    proof.put("currency", "USD");
    proof.put("financialReviewApproved", false);
  }

  /** Disponibiliza somente a autorização mais recente pertencente à versão vigente. */
  public VideoBudgetResponse.Authorization current(LearningSalesCycle cycle) {
    return history(cycle).stream()
        .filter(VideoBudgetResponse.Authorization::current)
        .findFirst()
        .orElse(null);
  }

  /** Mantém o caminho de ida com produto, cadeia e ocorrência explícitos. */
  public String financeUrl(LearningSalesCycle cycle) {
    return "/financial/videos?productId="
        + cycle.getProductId()
        + "&chainId="
        + cycle.getChainDefinitionId()
        + "&cycleId="
        + cycle.getId();
  }

  /** Lê no SQL apenas recibos financeiros e identifica substituições e versões históricas. */
  private List<VideoBudgetResponse.Authorization> history(LearningSalesCycle cycle) {
    var values = events.findByCycleIdAndActionOrderByRevisionDesc(cycle.getId(), ACTION);
    Long currentId =
        values.stream()
            .filter(event -> eligibleVersion(event, cycle))
            .map(LearningSalesCycleEvent::getId)
            .findFirst()
            .orElse(null);
    return values.stream()
        .map(
            event -> {
              var proof = json.read(event.getEvidenceJson());
              return new VideoBudgetResponse.Authorization(
                  event.getId(),
                  event.getEvidenceReference(),
                  proof.path("budgetLimitUsd").decimalValue(),
                  event.getOperatorName(),
                  proof.path("justification").asText(),
                  event.getCreatedAt(),
                  proof.path("productVersion").asText(),
                  Objects.equals(currentId, event.getId()));
            })
        .toList();
  }

  /** Recusa herança de teto após correção de versão ou de identidade financeira. */
  private boolean eligibleVersion(LearningSalesCycleEvent event, LearningSalesCycle cycle) {
    var proof = json.read(event.getEvidenceJson());
    return !event.getCreatedAt().isBefore(cycle.getVersionChangedAt())
        && cycle.getProductVersion().equals(proof.path("productVersion").asText())
        && cycle.getExperimentId() == proof.path("experimentId").asLong()
        && SCOPE.equals(proof.path("scope").asText())
        && "USD".equals(proof.path("currency").asText());
  }

  /** Restringe alterações à definição dos vídeos sem liberar consumo nas etapas seguintes. */
  private String blocker(LearningSalesCycle cycle, boolean videos) {
    if (!videos) return "Esta versão do processo não possui produção dos dois vídeos.";
    if (!"OPEN".equals(cycle.getStatus()))
      return "Ciclo encerrado: autorização disponível somente para consulta.";
    if (!"VIDEO_BRIEF".equals(cycle.getStage()))
      return "O teto pode ser registrado na etapa Definir os dois vídeos. Nesta etapa, consulte o histórico.";
    return null;
  }
}
