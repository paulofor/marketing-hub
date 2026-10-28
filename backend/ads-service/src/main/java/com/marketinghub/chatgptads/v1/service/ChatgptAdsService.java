package com.marketinghub.chatgptads.v1.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.chatgptads.v1.service.managecampaign.ChatgptAdsContracts.*;
import com.marketinghub.creative.CreativeAgentReviewStatus;
import com.marketinghub.creative.CreativeStatus;
import com.marketinghub.experiment.ExperimentPlatform;
import com.marketinghub.experiment.ExperimentStatus;
import com.marketinghub.experiment.run.*;
import com.marketinghub.repository.jdbc.chatgptads.ChatgptAdsRepository;
import com.marketinghub.repository.jdbc.chatgptads.ChatgptAdsRepository.Campaign;
import com.marketinghub.repository.jpa.creative.CreativeRepository;
import com.marketinghub.repository.jpa.experiment.*;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Coordena preparação, autorização, publicação e recuperação auditável do canal ChatGPT Ads. */
@Service
@Transactional
public class ChatgptAdsService {
  private static final org.slf4j.Logger log =
      org.slf4j.LoggerFactory.getLogger(ChatgptAdsService.class);
  private final ChatgptAdsRepository repository;
  private final ExperimentRepository experiments;
  private final CreativeRepository creatives;
  private final ExperimentRunRepository runs;
  private final ExperimentRunGateResultRepository gates;
  private final ObjectMapper mapper;

  /** Recebe as fontes canônicas; somente o backend decide as próximas operações. */
  public ChatgptAdsService(
      ChatgptAdsRepository repository,
      ExperimentRepository experiments,
      CreativeRepository creatives,
      ExperimentRunRepository runs,
      ExperimentRunGateResultRepository gates,
      ObjectMapper mapper) {
    this.repository = repository;
    this.experiments = experiments;
    this.creatives = creatives;
    this.runs = runs;
    this.gates = gates;
    this.mapper = mapper;
  }

  /** Lista fatos persistidos e ações permitidas, sem consultar externamente. */
  @Transactional(readOnly = true)
  public Overview overview(Long experimentId) {
    return new Overview(
        json(repository.account()),
        repository.list(experimentId).stream().map(this::view).toList());
  }

  /** Prepara material sem publicar ou aproveitar autorização de outro canal. */
  public CampaignView save(Long id, Draft draft) {
    require(draft != null, "Informe a preparação do anúncio.");
    require(
        ChatgptAdsPolicy.validate(draft, Instant.now()).isEmpty(),
        String.join(" ", ChatgptAdsPolicy.validate(draft, Instant.now())));
    require(experiments.existsById(draft.experimentId()), "Experimento inexistente.");
    repository.lockQueue();
    if (id == null) {
      require(
          repository.list(draft.experimentId()).isEmpty(),
          "Já existe preparação deste experimento. Abra o rascunho existente.");
      id =
          repository.insert(
              draft.experimentId(),
              tree(draft).toString(),
              ChatgptAdsPolicy.fingerprint(mapper, draft),
              Instant.now());
    } else {
      Campaign old = repository.lock(id);
      require(
          old.experimentId() == draft.experimentId()
              && old.status().equals("DRAFT")
              && !repository.busy(id),
          "Somente rascunhos sem publicação podem ser editados.");
      repository.edit(
          id, tree(draft).toString(), ChatgptAdsPolicy.fingerprint(mapper, draft), Instant.now());
    }
    return view(repository.get(id));
  }

  /** Solicita diagnóstico seguro da conexão dedicada de anúncios. */
  public void connect() {
    repository.lockQueue();
    queue(null, "ACCOUNT", "GET", "/ad_account", Map.of());
  }

  /** Registra decisão expressa vinculada a conta, material, valor e janela. */
  public CampaignView authorize(long id, Authorization decision) {
    require(decision != null, "Informe a decisão sobre esta preparação.");
    repository.lockQueue();
    var c = repository.lock(id);
    var account = json(repository.account());
    require(
        Objects.equals(c.fingerprint(), decision.fingerprint()),
        "O rascunho mudou; confira os limites novamente.");
    require(
        "AUTORIZO CHATGPT ADS".equals(decision.confirmation())
            && decision.decidedBy() != null
            && !decision.decidedBy().isBlank()
            && decision.decidedBy().length() <= 200,
        "Informe o responsável e a confirmação de autorização.");
    require(
        "active".equals(account.path("status").asText()) && account.path("id").isTextual(),
        "Conecte a conta de anúncios antes de autorizar.");
    require(
        draft(c).currency().equals(account.path("currency_code").asText()),
        "A moeda precisa ser a mesma da conta. Não há conversão automática.");
    require(
        Set.of("DRAFT", "PAUSED", "READY").contains(c.status()),
        "Aguarde a operação atual antes de alterar a autorização.");
    repository.authorize(
        id,
        tree(Map.of(
                "fingerprint",
                c.fingerprint(),
                "accountId",
                account.path("id").asText(),
                "decidedBy",
                decision.decidedBy(),
                "confirmation",
                decision.confirmation(),
                "at",
                Instant.now().toString()))
            .toString(),
        Instant.now());
    return view(repository.get(id));
  }

  /** Publica o material integralmente pausado depois dos aceites existentes. */
  public CampaignView publish(long id) {
    repository.lockQueue();
    var c = repository.lock(id);
    if (!c.status().equals("DRAFT")) return view(c);
    require(blockers(c).isEmpty(), String.join(" ", blockers(c)));
    state(c, "PUBLISHING", json(c.remote()));
    queue(id, "CAMPAIGN", "POST", "/campaigns", ChatgptAdsPolicy.campaign(draft(c)));
    return view(repository.get(id));
  }

  /** Ativa primeiro os componentes; a campanha e seu teto são liberados por último. */
  public CampaignView activate(long id) {
    repository.lockQueue();
    var c = repository.lock(id);
    require(
        Set.of("READY", "PAUSED").contains(c.status()) && !repository.busy(id),
        "A campanha ainda não está pronta para ativação.");
    require(blockers(c).isEmpty(), String.join(" ", blockers(c)));
    require(
        json(c.remote()).path("reviewApproved").asBoolean(),
        "A revisão do provedor precisa estar aprovada.");
    require(!draft(c).startsAt().isAfter(Instant.now()), "A janela autorizada ainda não começou.");
    state(c, "ACTIVATING", json(c.remote()));
    queue(id, "VERIFY_CAMPAIGN", "GET", "/campaigns/" + remoteId(c, "campaignId"), Map.of());
    return view(repository.get(id));
  }

  /** Pausa somente esta campanha, preservando histórico e autorização. */
  public CampaignView pause(long id) {
    repository.lockQueue();
    var c = repository.lock(id);
    require(json(c.remote()).hasNonNull("campaignId"), "Ainda não há campanha externa.");
    state(c, "PAUSING", json(c.remote()));
    queue(id, "PAUSE", "POST", "/campaigns/" + remoteId(c, "campaignId") + "/pause", Map.of());
    return view(repository.get(id));
  }

  /** Reconsulta revisão e métricas sem recriar recursos ou ativar campanha. */
  public CampaignView sync(long id) {
    repository.lockQueue();
    var c = repository.lock(id);
    require(!repository.busy(id), "Há uma operação em andamento.");
    require(json(c.remote()).hasNonNull("adId"), "A publicação ainda não terminou.");
    queue(
        id,
        "INSPECT",
        "GET",
        "/ads/" + remoteId(c, "adId") + "?include[]=serving_issues",
        Map.of());
    return view(c);
  }

  /** Retoma a mesma operação falha com a mesma chave idempotente, após conferência das entradas. */
  public CampaignView retry(long id) {
    repository.lockQueue();
    var c = repository.lock(id);
    require(
        c.status().equals("BLOCKED") && !repository.busy(id),
        "Não há falha recuperável nesta campanha.");
    if (!"PAUSE".equals(repository.lastFailedKind(id)))
      require(blockers(c).isEmpty(), String.join(" ", blockers(c)));
    repository.retry(id);
    state(c, "RECOVERING", json(c.remote()));
    return view(repository.get(id));
  }

  /** Entrega uma operação reservada e reconcilia travas quando o executor consulta a fila. */
  public Work pending() {
    repository.lockQueue();
    for (var c : repository.monitored()) {
      if (repository.busy(c.id())) continue;
      if (!draft(c).endsAt().isAfter(Instant.now())
          || (!blockers(c).isEmpty() && c.status().equals("ACTIVE"))) {
        pause(c.id());
        continue;
      }
      if (c.updatedAt().isBefore(Instant.now().minusSeconds(90))) {
        if (c.status().equals("ACTIVE")) metrics(c);
        else sync(c.id());
      }
    }
    var op = repository.pending(Instant.now());
    if (op == null) return null;
    if (op.campaignId() != null
        && (op.kind().startsWith("ACTIVATE") || op.kind().startsWith("VERIFY_"))) {
      var campaign = repository.get(op.campaignId());
      if (!Set.of("ACTIVATING", "RECOVERING").contains(campaign.status())
          || !blockers(campaign).isEmpty()) {
        repository.finish(
            op.id(),
            "CANCELLED",
            tree(Map.of("reason", "Autorização ou gate mudou antes do envio")).toString(),
            Instant.now());
        pause(campaign.id());
        return null;
      }
    }
    if (op.attempts() >= 3) {
      repository.finish(
          op.id(),
          "FAILED",
          tree(Map.of("errorCode", "DELIVERY_UNCERTAIN")).toString(),
          Instant.now());
      if (op.campaignId() != null) {
        var uncertain = repository.get(op.campaignId());
        var remote = (ObjectNode) json(uncertain.remote());
        remote.put("errorCode", "DELIVERY_UNCERTAIN");
        state(uncertain, "BLOCKED", remote);
        if (op.kind().startsWith("ACTIVATE") || uncertain.status().equals("ACTIVE"))
          queue(
              uncertain.id(),
              "PAUSE",
              "POST",
              "/campaigns/" + remoteId(uncertain, "campaignId") + "/pause",
              Map.of());
      }
      return null;
    }
    String lease = UUID.randomUUID().toString();
    repository.lease(op.id(), lease, Instant.now().plusSeconds(180));
    var request = json(op.request());
    return new Work(
        op.id(),
        op.campaignId(),
        lease,
        op.kind(),
        request.path("method").asText(),
        request.path("path").asText(),
        request.path("body"),
        "mkt-chatgpt-v1-" + op.id(),
        op.campaignId() == null
            ? null
            : json(repository.get(op.campaignId()).authorization()).path("accountId").asText());
  }

  /** Aplica uma resposta uma única vez e persiste a próxima passagem na mesma transação. */
  public void receive(long operationId, Receipt receipt) {
    repository.lockQueue();
    var op = repository.lockOperation(operationId);
    if (Set.of("SUCCEEDED", "FAILED", "CANCELLED").contains(op.status())) return;
    require(
        "RUNNING".equals(op.status()) && Objects.equals(op.lease(), receipt.lease()),
        "Reserva da operação expirou; releia a fila.");
    require(
        receipt.response() != null && receipt.response().toString().length() <= 262144,
        "Resposta ausente ou excessiva.");
    boolean success =
        receipt.httpStatus() >= 200 && receipt.httpStatus() < 300 && receipt.errorCode() == null;
    repository.finish(
        operationId, success ? "SUCCEEDED" : "FAILED", tree(receipt).toString(), Instant.now());
    if (op.campaignId() == null) {
      ObjectNode account = mapper.createObjectNode();
      if (success) {
        for (String field : List.of("id", "name", "status", "timezone", "currency_code", "review"))
          if (receipt.response().has(field)) account.set(field, receipt.response().get(field));
      } else {
        account.put("status", "NOT_CONNECTED");
        account.put(
            "message", "Conexão indisponível. Confira a chave dedicada de anúncios no Hermes.");
      }
      account.put("checkedAt", Instant.now().toString());
      repository.account(account.toString());
      return;
    }
    var c = repository.lock(op.campaignId());
    var remote = (ObjectNode) json(c.remote());
    if (!success) {
      remote.put(
          "errorCode",
          receipt.errorCode() == null ? "HTTP_" + receipt.httpStatus() : receipt.errorCode());
      state(c, "BLOCKED", remote);
      if ((c.status().equals("ACTIVE") || op.kind().startsWith("ACTIVATE"))
          && !op.kind().equals("PAUSE"))
        queue(
            c.id(),
            "PAUSE",
            "POST",
            "/campaigns/" + remote.path("campaignId").asText() + "/pause",
            Map.of());
      return;
    }
    if (!op.kind().equals("PAUSE")) remote.remove("errorCode");
    JsonNode response = receipt.response();
    Draft d = draft(c);
    try {
      switch (op.kind()) {
        case "CAMPAIGN" -> {
          remote.put("campaignId", ChatgptAdsPolicy.id(response, "id"));
          if (!ChatgptAdsPolicy.budgetMatches(d, response)
              || !"paused".equals(response.path("status").asText())) {
            remote.put("errorCode", "NATIVE_BUDGET_MISMATCH");
            repository.finish(operationId, "FAILED", tree(receipt).toString(), Instant.now());
            state(c, "PAUSING", remote);
            queue(
                c.id(),
                "PAUSE",
                "POST",
                "/campaigns/" + remote.path("campaignId").asText() + "/pause",
                Map.of());
            return;
          }
          remote.put("nativeBudgetVerified", true);
          state(c, "PUBLISHING", remote);
          queue(
              c.id(),
              "GROUP",
              "POST",
              "/ad_groups",
              ChatgptAdsPolicy.group(d, remote.path("campaignId").asText()));
        }
        case "GROUP" -> {
          remote.put("groupId", ChatgptAdsPolicy.id(response, "id"));
          state(c, "PUBLISHING", remote);
          queue(c.id(), "IMAGE", "POST", "/upload", Map.of("image_url", d.imageUrl()));
        }
        case "IMAGE" -> {
          remote.put("fileId", ChatgptAdsPolicy.id(response, "file_id"));
          state(c, "PUBLISHING", remote);
          queue(
              c.id(),
              "AD",
              "POST",
              "/ads",
              ChatgptAdsPolicy.ad(
                  d, remote.path("groupId").asText(), remote.path("fileId").asText()));
        }
        case "AD" -> {
          remote.put("adId", ChatgptAdsPolicy.id(response, "id"));
          state(c, "AWAITING_REVIEW", remote);
          queue(
              c.id(),
              "INSPECT",
              "GET",
              "/ads/" + remote.path("adId").asText() + "?include[]=serving_issues",
              Map.of());
        }
        case "INSPECT" -> {
          boolean approved = "approved".equals(response.path("review_status").asText());
          remote.put("reviewApproved", approved);
          remote.set("review", response.path("review_status"));
          remote.set("servingIssues", response.path("serving_issues"));
          state(
              c,
              c.status().equals("ACTIVE") ? "ACTIVE" : approved ? "READY" : "AWAITING_REVIEW",
              remote);
        }
        case "VERIFY_CAMPAIGN" -> {
          if (!ChatgptAdsPolicy.budgetMatches(d, response)
              || !remote.path("campaignId").asText().equals(response.path("id").asText())) {
            remote.put("errorCode", "NATIVE_BUDGET_MISMATCH");
            state(c, "BLOCKED", remote);
            return;
          }
          if (stopActivation(c, remote)) return;
          queue(
              c.id(),
              "VERIFY_GROUP",
              "GET",
              "/ad_groups/" + remote.path("groupId").asText(),
              Map.of());
        }
        case "VERIFY_GROUP" -> {
          require(
              remote.path("groupId").asText().equals(response.path("id").asText())
                  && response.path("bidding_config").path("max_bid_micros").asLong()
                      == ChatgptAdsPolicy.micros(d.maxCpc())
                  && "fixed_bid".equals(response.path("bidding_config").path("strategy").asText()),
              "Lance nativo divergente da autorização.");
          if (stopActivation(c, remote)) return;
          queue(c.id(), "VERIFY_AD", "GET", "/ads/" + remote.path("adId").asText(), Map.of());
        }
        case "VERIFY_AD" -> {
          var material = response.path("creative");
          require(
              remote.path("adId").asText().equals(response.path("id").asText())
                  && "approved".equals(response.path("review_status").asText())
                  && d.title().equals(material.path("title").asText())
                  && d.body().equals(material.path("body").asText())
                  && d.targetUrl().equals(material.path("target_url").asText())
                  && remote.path("fileId").asText().equals(material.path("file_id").asText()),
              "Material externo divergiu da peça revisada.");
          if (stopActivation(c, remote)) return;
          queue(
              c.id(),
              "ACTIVATE_AD",
              "POST",
              "/ads/" + remote.path("adId").asText() + "/activate",
              Map.of());
        }
        case "ACTIVATE_AD" -> {
          if (stopActivation(c, remote)) return;
          queue(
              c.id(),
              "ACTIVATE_GROUP",
              "POST",
              "/ad_groups/" + remote.path("groupId").asText() + "/activate",
              Map.of());
        }
        case "ACTIVATE_GROUP" -> {
          if (stopActivation(c, remote)) return;
          queue(
              c.id(),
              "ACTIVATE_CAMPAIGN",
              "POST",
              "/campaigns/" + remote.path("campaignId").asText() + "/activate",
              Map.of());
        }
        case "ACTIVATE_CAMPAIGN" -> {
          if (stopActivation(c, remote)) return;
          require(
              "active".equals(response.path("status").asText()),
              "Ativação não confirmada pelo provedor.");
          state(c, "ACTIVE", remote);
          lifecycle(c, "PUBLISHED", null);
          metrics(repository.get(c.id()));
        }
        case "PAUSE" -> {
          require("paused".equals(response.path("status").asText()), "Pausa não confirmada.");
          boolean unresolved = repository.lastFailedKind(c.id()) != null;
          if (!unresolved) remote.remove("errorCode");
          state(c, unresolved ? "BLOCKED" : "PAUSED", remote);
          lifecycle(c, "PAUSED", null);
        }
        case "METRICS" -> {
          require(
              !response.path("has_more").asBoolean(),
              "Métricas parciais não podem ser consolidadas.");
          var rows = response.path("data");
          require(rows.isArray() && rows.size() <= 1, "Esperado um agregado único da campanha.");
          ObjectNode m = mapper.createObjectNode();
          m.put("observedAt", Instant.now().toString());
          m.put("currency", d.currency());
          if (rows.size() == 1) {
            var row = rows.get(0);
            require(
                remote.path("campaignId").asText().equals(row.path("campaign_id").asText()),
                "Métricas de outra campanha.");
            for (String field : List.of("impressions", "clicks", "spend"))
              if (row.hasNonNull(field)) {
                require(
                    row.get(field).isNumber() && row.get(field).decimalValue().signum() >= 0,
                    "Métrica inválida.");
                m.set(field, row.get(field));
              }
          }
          repository.metrics(c.id(), m.toString(), Instant.now());
          if (c.status().equals("RECOVERING")) state(c, "PAUSED", remote);
          if (c.status().equals("ACTIVE") && m.path("impressions").asLong() > 0)
            lifecycle(c, "EXPOSED", Instant.now());
          if (m.has("spend") && m.path("spend").decimalValue().compareTo(d.totalBudget()) >= 0)
            pause(c.id());
        }
        default -> throw new ResponseStatusException(HttpStatus.CONFLICT, "Operação desconhecida.");
      }
    } catch (ResponseStatusException ex) {
      log.error(
          "ChatGPT Ads operação={} campanha={} contrato de resposta inválido",
          operationId,
          c.id(),
          ex);
      repository.finish(operationId, "FAILED", tree(receipt).toString(), Instant.now());
      remote.put("errorCode", "INVALID_PROVIDER_RESPONSE");
      state(c, "BLOCKED", remote);
      if (op.kind().startsWith("ACTIVATE") || c.status().equals("ACTIVE")) {
        queue(
            c.id(),
            "PAUSE",
            "POST",
            "/campaigns/" + remote.path("campaignId").asText() + "/pause",
            Map.of());
      }
    }
  }

  /** Interrompe a cadeia de ativação se qualquer condição mudar enquanto a API responde. */
  private boolean stopActivation(Campaign c, ObjectNode remote) {
    if (!Set.of("ACTIVATING", "RECOVERING").contains(c.status()) || !blockers(c).isEmpty()) {
      state(c, "PAUSING", remote);
      queue(
          c.id(),
          "PAUSE",
          "POST",
          "/campaigns/" + remote.path("campaignId").asText() + "/pause",
          Map.of());
      return true;
    }
    return false;
  }

  /** Atualiza o run real somente após confirmação externa, distinguindo publicação de exposição. */
  private void lifecycle(Campaign c, String event, Instant exposure) {
    var exp = experiments.findById(c.experimentId()).orElseThrow();
    runs.findTopByExperimentIdAndModeOrderByRunNumberDesc(
            c.experimentId(), ExperimentRunMode.PRODUCTION)
        .ifPresent(
            run -> {
              if (Set.of(
                      ExperimentRunStatus.COMPLETED,
                      ExperimentRunStatus.FAILED,
                      ExperimentRunStatus.CANCELLED)
                  .contains(run.getStatus())) return;
              if (event.equals("PUBLISHED")) {
                run.setStatus(ExperimentRunStatus.PUBLISHED_AWAITING_EXPOSURE);
                if (run.getPublishedAt() == null) run.setPublishedAt(Instant.now());
              }
              if (event.equals("EXPOSED")) {
                run.setStatus(ExperimentRunStatus.RUNNING);
                if (run.getFirstVerifiedImpressionAt() == null) {
                  run.setFirstVerifiedImpressionAt(exposure);
                  run.setCommercialWindowStartedAt(exposure);
                }
                if (exp.getStatus() == ExperimentStatus.PLANNED
                    || exp.getStatus() == ExperimentStatus.PAUSED) {
                  exp.setStatus(ExperimentStatus.RUNNING);
                  exp.setLastStatusChangeAction("CHATGPT_ADS_EXPOSURE");
                  exp.setLastStatusChangedAt(Instant.now());
                  exp.setLastStatusChangeReason(
                      "Primeira impressão informada pelo provedor ChatGPT Ads.");
                  experiments.save(exp);
                }
              }
              if (event.equals("PAUSED")
                  && Set.of(
                          ExperimentRunStatus.RUNNING,
                          ExperimentRunStatus.PUBLISHED_AWAITING_EXPOSURE)
                      .contains(run.getStatus())) {
                run.setStatus(ExperimentRunStatus.PAUSED);
                if (exp.getStatus() == ExperimentStatus.RUNNING) {
                  exp.setStatus(ExperimentStatus.PAUSED);
                  experiments.save(exp);
                }
              }
              runs.save(run);
            });
  }

  /** Prepara consulta agregada em horas completas, preservando a moeda da conta. */
  private void metrics(Campaign c) {
    long end = Instant.now().getEpochSecond() / 3600 * 3600;
    long start = draft(c).startsAt().getEpochSecond() / 3600 * 3600;
    if (end <= start) return;
    String range =
        java.net.URLEncoder.encode(
            tree(Map.of("type", "unix_range", "start", start, "end", end)).toString(),
            java.nio.charset.StandardCharsets.UTF_8);
    queue(
        c.id(),
        "METRICS",
        "GET",
        "/campaigns/"
            + remoteId(c, "campaignId")
            + "/insights?time_granularity=none&aggregation_level=campaign&fields[]=campaign.impressions&fields[]=campaign.clicks&fields[]=campaign.spend&time_ranges[]="
            + range,
        Map.of());
  }

  /**
   * Revalida fontes, conta, moeda e homologação; uma autorização antiga não substitui evidências.
   */
  private List<String> blockers(Campaign c) {
    Draft d = draft(c);
    var issues = new ArrayList<>(ChatgptAdsPolicy.validate(d, Instant.now()));
    var exp = experiments.findById(c.experimentId()).orElseThrow();
    if (exp.getExperimentType() == com.marketinghub.experiment.ExperimentType.FAKE_EXPERIMENT)
      issues.add("Experimentos simulados não podem criar campanhas reais.");
    if (exp.getPlatform() != ExperimentPlatform.CHATGPT_ADS)
      issues.add("Use um experimento próprio do canal ChatGPT Ads; não reutilize a campanha Meta.");
    if (!Set.of(ExperimentStatus.PLANNED, ExperimentStatus.RUNNING, ExperimentStatus.PAUSED)
        .contains(exp.getStatus())) issues.add("O experimento está encerrado.");
    var account = json(repository.account());
    var authorization = json(c.authorization());
    if (!"active".equals(account.path("status").asText())
        || !d.currency().equals(account.path("currency_code").asText()))
      issues.add("Conecte a conta e confira a mesma moeda do orçamento.");
    if (!c.fingerprint().equals(authorization.path("fingerprint").asText())
        || !account.path("id").asText("missing").equals(authorization.path("accountId").asText()))
      issues.add("Registre autorização própria para esta conta, anúncio, teto e janela.");
    var creative = creatives.findById(d.creativeId()).orElse(null);
    if (creative == null
        || creative.getExperiment() == null
        || !Objects.equals(creative.getExperiment().getId(), d.experimentId())
        || creative.getStatus() != CreativeStatus.READY
        || creative.getAgentReviewStatus() != CreativeAgentReviewStatus.APPROVED
        || !Objects.equals(creative.getHeadline(), d.title())
        || !Objects.equals(creative.getPrimaryText(), d.body())
        || !Objects.equals(creative.getImageUrl(), d.imageUrl())
        || !Objects.equals(creative.getDestinationUrl(), d.targetUrl()))
      issues.add("Revise e aprove o criativo com esta imagem, título, mensagem e destino exatos.");
    if (!Objects.equals(exp.getFollowUpActionUrl(), d.targetUrl()))
      issues.add("O destino precisa ser o homologado no próprio experimento.");
    var run =
        runs.findTopByExperimentIdAndModeOrderByRunNumberDesc(
                c.experimentId(), ExperimentRunMode.PRODUCTION)
            .orElse(null);
    if (run == null
        || !Set.of(
                ExperimentRunStatus.READY_TO_PUBLISH,
                ExperimentRunStatus.PUBLISHED_AWAITING_EXPOSURE,
                ExperimentRunStatus.RUNNING,
                ExperimentRunStatus.PAUSED)
            .contains(run.getStatus())
        || run.getPreflightCompletedAt() == null
        || run.getPreflightCompletedAt().isBefore(Instant.now().minus(Duration.ofDays(7)))
        || run.getDataQualityStatus() != ExperimentRunDataQualityStatus.VALID)
      issues.add(
          "Conclua a homologação produtiva atual de página, compra, entrega, canal e medição.");
    else {
      var checks = gates.findByExperimentRunIdOrderByGateGroupAscGateCodeAsc(run.getId());
      var required =
          Set.of(
              "LANDING_QUALITY_REVIEW_APPROVED",
              "CHECKOUT_AND_DELIVERY_CAN_BE_COMPLETED",
              "CHATGPT_ADS_READINESS_CONFIRMED",
              "DATA_FRESHNESS_VALID");
      if (checks.isEmpty()
          || checks.stream()
              .anyMatch(
                  g ->
                      g.getStatus() != ExperimentRunGateStatus.PASS
                          && g.getStatus() != ExperimentRunGateStatus.NOT_APPLICABLE)
          || required.stream()
              .anyMatch(
                  code ->
                      checks.stream()
                          .noneMatch(
                              g ->
                                  code.equals(g.getGateCode())
                                      && g.getStatus() == ExperimentRunGateStatus.PASS
                                      && g.getEvidenceReference() != null
                                      && !g.getEvidenceReference().isBlank())))
        issues.add("Há gates comerciais pendentes ou sem evidência do canal ChatGPT Ads.");
    }
    return issues;
  }

  /** Projeta o estado legível e os comandos realmente permitidos. */
  private CampaignView view(Campaign c) {
    var exp = experiments.findById(c.experimentId()).orElseThrow();
    var issues = new ArrayList<>(blockers(c));
    String error = json(c.remote()).path("errorCode").asText();
    if (!error.isBlank())
      issues.add(
          error.equals("CREDENTIAL_MISSING")
              ? "A chave de anúncios ainda não foi configurada no Hermes."
              : error.startsWith("HTTP_401") || error.equals("ACCOUNT_CHANGED_OR_UNAVAILABLE")
                  ? "Reconecte e confira a conta de anúncios antes de retomar."
                  : error.equals("DELIVERY_UNCERTAIN")
                      ? "A resposta não foi confirmada. Confira os recibos antes de retomar a operação preservada."
                      : "A operação do canal precisa de conferência. Os recibos foram preservados; não crie outra campanha para contornar a falha.");
    boolean clear = issues.isEmpty() && !repository.busy(c.id());
    return new CampaignView(
        c.id(),
        c.experimentId(),
        exp.getProduct() == null ? exp.getName() : exp.getProduct().getName(),
        c.status(),
        draft(c),
        c.fingerprint(),
        json(c.authorization()).has("fingerprint"),
        issues,
        clear && c.status().equals("DRAFT"),
        clear
            && Set.of("READY", "PAUSED").contains(c.status())
            && json(c.remote()).path("reviewApproved").asBoolean()
            && !draft(c).startsAt().isAfter(Instant.now()),
        json(c.remote()).has("campaignId") && !c.status().equals("PAUSED"),
        json(c.remote()),
        json(c.metrics()),
        c.updatedAt());
  }

  /** Enfileira um request sem credenciais para o executor responsável. */
  private void queue(Long id, String kind, String method, String path, Object body) {
    repository.enqueue(
        id,
        kind,
        tree(Map.of("method", method, "path", path, "body", body)).toString(),
        Instant.now());
  }

  /** Persiste estado sem apagar as evidências externas já recebidas. */
  private void state(Campaign c, String status, JsonNode remote) {
    repository.state(c.id(), status, remote.toString(), Instant.now());
  }

  /** Recupera o rascunho já validado e persistido. */
  private Draft draft(Campaign c) {
    return mapper.convertValue(json(c.draft()), Draft.class);
  }

  /** Lê IDs persistidos com a mesma proteção aplicada às respostas do provedor. */
  private String remoteId(Campaign c, String key) {
    return ChatgptAdsPolicy.id(json(c.remote()), key);
  }

  /** Converte contratos tipados para a representação auditável. */
  private JsonNode tree(Object value) {
    return mapper.valueToTree(value);
  }

  /** Lê JSON persistido, recusando corrupção antes de qualquer efeito externo. */
  private JsonNode json(String value) {
    if (value == null) return mapper.createObjectNode();
    try {
      return mapper.readTree(value);
    } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
      log.error("ChatGPT Ads falha ao interpretar registro persistido", ex);
      throw new IllegalStateException("Registro ChatGPT Ads inválido", ex);
    }
  }

  /** Interrompe comandos que não cumprem as condições públicas do contrato. */
  private static void require(boolean condition, String message) {
    if (!condition) throw new ResponseStatusException(HttpStatus.CONFLICT, message);
  }
}
