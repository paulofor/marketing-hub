package com.marketinghub.productdiscovery.v1.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agenttask.AgentTask;
import com.marketinghub.businessprocess.independent.service.IndependentBusinessProcessExecutionReportProvider;
import com.marketinghub.businessprocess.independent.service.executions.IndependentBusinessProcessFlowReportResponse;
import com.marketinghub.opportunitydossier.OpportunityDossier;
import com.marketinghub.productdiscovery.v1.ProductDiscoveryCycle;
import com.marketinghub.productdiscovery.v1.ProductDiscoveryCycleStatus;
import com.marketinghub.productdiscovery.v1.ProductDiscoveryOpportunity;
import com.marketinghub.productdiscovery.v1.ProductDiscoveryOpportunityMaturity;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.opportunitydossier.OpportunityDossierRepository;
import com.marketinghub.repository.jpa.productdiscovery.ProductDiscoveryCycleRepository;
import com.marketinghub.repository.jpa.productdiscovery.ProductDiscoveryIndependentStatusProjection;
import com.marketinghub.repository.jpa.productdiscovery.ProductDiscoveryOpportunityRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Responsabilidade: consolidar a jornada visível de Argos até o produto PDE planejado. */
@Service
public class ProductDiscoveryIndependentExecutionReportService
    implements IndependentBusinessProcessExecutionReportProvider {
  private static final Logger log =
      LoggerFactory.getLogger(ProductDiscoveryIndependentExecutionReportService.class);
  private static final String PROCESS_CODE = "pde-opportunity-discovery";
  private static final String SOURCE_PREFIX = "product-discovery-cycle:";
  private static final String META_COVERAGE_STATUS_CODE =
      "(?:UNAVAILABLE|OBSERVED|NO_MATCHING_ACTIVE_ADS|NO_ACTIVE_ADS|NO_RELEVANT_PLATFORM_EVIDENCE|AWAITING_PUBLIC_BROWSER|AWAITING_SUPERVISED_OBSERVATION|AWAITING_OFFICIAL_COLLECTION|NOT_REQUESTED|STALE|UNKNOWN)";
  private static final Pattern TECHNICAL_META_COVERAGE =
      Pattern.compile(
          "cobertura\\s+"
              + META_COVERAGE_STATUS_CODE
              + "(?:,\\s*"
              + META_COVERAGE_STATUS_CODE
              + ")*");
  private final ProductDiscoveryCycleRepository cycleRepository;
  private final ProductDiscoveryOpportunityRepository opportunityRepository;
  private final OpportunityDossierRepository dossierRepository;
  private final AgentTaskRepository taskRepository;
  private final ObjectMapper objectMapper;

  /** Configura as fontes persistidas usadas pelo relatório sem recomputar decisões no navegador. */
  public ProductDiscoveryIndependentExecutionReportService(
      ProductDiscoveryCycleRepository cycleRepository,
      ProductDiscoveryOpportunityRepository opportunityRepository,
      OpportunityDossierRepository dossierRepository,
      AgentTaskRepository taskRepository,
      ObjectMapper objectMapper) {
    this.cycleRepository = cycleRepository;
    this.opportunityRepository = opportunityRepository;
    this.dossierRepository = dossierRepository;
    this.taskRepository = taskRepository;
    this.objectMapper = objectMapper;
  }

  /** Identifica o processo independente de descoberta PDE. */
  @Override
  public String processCode() {
    return PROCESS_CODE;
  }

  /**
   * Resolve o estado funcional dos cards sem carregar prompts, evidências ou candidatas completas.
   */
  @Override
  @Transactional(readOnly = true)
  public Map<String, String> summaryStatuses(Map<String, String> technicalStatuses) {
    if (technicalStatuses.isEmpty()) return Map.of();
    Map<Long, String> referencesByCycle = new LinkedHashMap<>();
    technicalStatuses
        .keySet()
        .forEach(reference -> referencesByCycle.put(cycleId(reference), reference));
    Map<Long, ProductDiscoveryIndependentStatusProjection> snapshotsByCycle = new LinkedHashMap<>();
    cycleRepository
        .findIndependentStatusSnapshotsByIds(referencesByCycle.keySet())
        .forEach(snapshot -> snapshotsByCycle.put(snapshot.getCycleId(), snapshot));
    Map<String, String> statuses = new LinkedHashMap<>();
    referencesByCycle.forEach(
        (id, reference) -> {
          ProductDiscoveryIndependentStatusProjection snapshot = snapshotsByCycle.get(id);
          if (snapshot == null) {
            throw new IllegalArgumentException("Ciclo de Argos não foi encontrado.");
          }
          statuses.put(reference, summaryStatus(snapshot, technicalStatuses.get(reference)));
        });
    return Map.copyOf(statuses);
  }

  /** Consolida fontes, candidatas, gates, dossiês e produto na linhagem do ciclo. */
  @Override
  @Transactional(readOnly = true)
  public IndependentBusinessProcessFlowReportResponse report(String sourceReference) {
    ProductDiscoveryCycle cycle = requiredCycle(sourceReference);
    List<ProductDiscoveryOpportunity> opportunities =
        opportunityRepository.findAllByCycleIdOrderByScoreDesc(cycle.getId());
    List<OpportunityDossier> dossiers =
        dossierRepository.findAllByProductDiscoveryCycleIdOrderByIdAsc(cycle.getId());
    List<AgentTask> tasks =
        taskRepository.findBySourceReferenceOrderByCreatedAtAscIdAsc(sourceReference);
    Map<String, AgentTask> latestTasks = latestTasks(tasks);
    Long selectedDossierId = selectedDossierId(latestTasks.get("marketStrategy"));
    List<IndependentBusinessProcessFlowReportResponse.Candidate> candidates =
        opportunities.stream()
            .map(
                opportunity ->
                    candidate(
                        opportunity,
                        dossier(opportunity, dossiers),
                        latestTasks,
                        selectedDossierId))
            .toList();
    int ready =
        (int)
            opportunities.stream()
                .filter(
                    item -> item.getMaturity() == ProductDiscoveryOpportunityMaturity.DOSSIER_READY)
                .count();
    int products = (int) dossiers.stream().filter(item -> item.getCreatedProduct() != null).count();
    JsonNode evidenceReport =
        read(cycle.getResearchEvidenceReportJson(), "relatório", cycle.getId());
    List<IndependentBusinessProcessFlowReportResponse.SourceCoverage> sourceCoverage =
        sourceCoverage(cycle, evidenceReport, opportunities);
    return new IndependentBusinessProcessFlowReportResponse(
        "PDE_OPPORTUNITY_TO_PRIVATE_VALIDATION_V2",
        reportStatus(cycle, List.copyOf(latestTasks.values()), ready, products),
        businessHeadline(cycle, sourceCoverage),
        firstText(cycle.getAcquisitionChannel(), "Instagram"),
        opportunities.size(),
        ready,
        products,
        privateValidationHandoff(cycle, latestTasks, ready, products),
        sourceCoverage,
        marketExpansion(evidenceReport),
        candidates);
  }

  /** Decide no backend se o ciclo pode retomar a validação privada sem repetir Argos. */
  private IndependentBusinessProcessFlowReportResponse.PrivateValidationHandoff
      privateValidationHandoff(
          ProductDiscoveryCycle cycle,
          Map<String, AgentTask> latestTasks,
          int ready,
          int products) {
    if (cycle.getStatus() != ProductDiscoveryCycleStatus.COMPLETED) {
      return handoffUnavailable(
          cycle.getId(), "WAITING_RESEARCH", "A pesquisa factual ainda não foi concluída.");
    }
    if (ready == 0) {
      return handoffUnavailable(
          cycle.getId(),
          "NO_READY_DOSSIER",
          "Argos ainda não formou um dossiê factual pronto para protótipo privado.");
    }
    if (products > 0) {
      return handoffUnavailable(
          cycle.getId(),
          "PRODUCT_PLANNED",
          "O produto planejado já existe; o avanço continua pela cadeia do produto.");
    }
    AgentTask strategy = latestTasks.get("marketStrategy");
    if (strategy != null
        && "COMPLETED".equals(strategy.getStatus())
        && !hasCurrentPrivateValidationStrategy(strategy)) {
      return new IndependentBusinessProcessFlowReportResponse.PrivateValidationHandoff(
          true,
          cycle.getId(),
          "STALE_STRATEGY_CONTRACT",
          "Reiniciar com Atena atual",
          "A seleção histórica usa um contrato anterior; a nova tentativa preservará Argos e reiniciará Atena, Plutus e Dédalo na versão vigente.");
    }
    for (String activityId : List.of("marketStrategy", "economics", "productArchitecture")) {
      AgentTask task = latestTasks.get(activityId);
      if (task == null || List.of("BLOCKED", "CANCELLED").contains(task.getStatus())) {
        return new IndependentBusinessProcessFlowReportResponse.PrivateValidationHandoff(
            true,
            cycle.getId(),
            "AVAILABLE",
            "Retomar com Atena",
            "Os dossiês atuais podem seguir pela versão vigente sem repetir a pesquisa de Argos.");
      }
      if ("COMPLETED".equals(task.getStatus())) continue;
      return handoffUnavailable(
          cycle.getId(),
          "IN_PROGRESS",
          "O handoff privado já possui uma atividade pendente ou em execução.");
    }
    return handoffUnavailable(
        cycle.getId(),
        "MATERIALIZATION_PENDING",
        "Os gates terminaram e o backend ainda precisa materializar o produto planejado.");
  }

  /** Cria uma indisponibilidade funcional com rótulo estável para a tela administrativa. */
  private IndependentBusinessProcessFlowReportResponse.PrivateValidationHandoff handoffUnavailable(
      Long cycleId, String status, String reason) {
    return new IndependentBusinessProcessFlowReportResponse.PrivateValidationHandoff(
        false, cycleId, status, "Retomar com Atena", reason);
  }

  /** Reconhece somente a estratégia capaz de sustentar as duas leituras privadas atuais. */
  private boolean hasCurrentPrivateValidationStrategy(AgentTask task) {
    JsonNode contract =
        read(task.getResultJson(), "resultado", task.getId()).path("marketStrategicContract");
    JsonNode plan = contract.path("privateValidationPlan");
    return "MARKET_STRATEGY_V3".equals(contract.path("contractVersion").asText())
        && "READY_FOR_PRIVATE_VALIDATION".equals(contract.path("status").asText())
        && plan.path("minimumIndependentReadings").asInt(0) == 2;
  }

  /** Substitui códigos técnicos legados por uma leitura compreensível da cobertura Meta. */
  private String businessHeadline(
      ProductDiscoveryCycle cycle,
      List<IndependentBusinessProcessFlowReportResponse.SourceCoverage> sourceCoverage) {
    String headline =
        firstText(
            cycle.getDecisionSummary(),
            "Argos ainda está reunindo evidências para formar candidatas factuais.");
    Matcher matcher = TECHNICAL_META_COVERAGE.matcher(headline);
    if (!matcher.find()) return headline;
    String status =
        sourceCoverage.stream()
            .filter(item -> "META".equals(item.sourceCode()))
            .map(IndependentBusinessProcessFlowReportResponse.SourceCoverage::status)
            .findFirst()
            .orElse("MISSING");
    String replacement =
        switch (status) {
          case "OBSERVED" -> "cobertura da Biblioteca Meta observada";
          case "OBSERVED_EMPTY" -> "cobertura da Biblioteca Meta executada sem anúncio aderente";
          case "AWAITING_OBSERVATION" -> "cobertura da Biblioteca Meta aguardando observação";
          case "UNAVAILABLE" ->
              "cobertura da Biblioteca Meta não executada por falha de integração";
          default -> "cobertura da Biblioteca Meta sem resultado auditável";
        };
    return matcher.replaceAll(Matcher.quoteReplacement(replacement));
  }

  /** Converte as rodadas persistidas pelo worker em um resumo gerencial tipado. */
  private IndependentBusinessProcessFlowReportResponse.MarketExpansion marketExpansion(
      JsonNode evidenceReport) {
    JsonNode expansion = evidenceReport.path("marketExpansion");
    if (!expansion.isObject()) return null;
    List<IndependentBusinessProcessFlowReportResponse.MarketExpansionAttempt> attempts =
        new ArrayList<>();
    JsonNode persistedAttempts = expansion.path("attempts");
    JsonNode metaCoverages = evidenceReport.path("metaCoverage");
    if (persistedAttempts.isArray()) {
      for (JsonNode item : persistedAttempts) {
        int attemptNumber = item.path("attemptNumber").asInt();
        JsonNode metaCoverage = metaCoverageForAttempt(metaCoverages, attemptNumber);
        attempts.add(
            new IndependentBusinessProcessFlowReportResponse.MarketExpansionAttempt(
                attemptNumber,
                optionalText(item, "researchLens"),
                optionalText(item, "expansionAxis"),
                optionalText(item, "rationale"),
                item.path("newPublicEvidenceCount").asInt(),
                item.path("newComparableOfferCount").asInt(),
                item.path("newMetaAdCount").asInt(),
                item.path("candidateCount").asInt(),
                item.path("dossierReadyCount").asInt(),
                optionalText(item, "outcome"),
                optionalText(metaCoverage, "query"),
                optionalText(metaCoverage, "sourceStatus"),
                optionalText(metaCoverage, "collectionMode"),
                metaCoverage.path("adsObserved").asInt(),
                metaCoverage.path("advertisersObserved").asInt(),
                optionalText(metaCoverage, "interpretation"),
                optionalText(metaCoverage, "searchUrl")));
      }
    }
    return new IndependentBusinessProcessFlowReportResponse.MarketExpansion(
        optionalText(expansion, "strategyCode"),
        expansion.path("attemptsCompleted").asInt(attempts.size()),
        expansion.path("maxAttempts").asInt(),
        optionalText(expansion, "stopReason"),
        optionalText(expansion, "stopSummary"),
        optionalText(expansion, "finalResearchLens"),
        List.copyOf(attempts));
  }

  /**
   * Correlaciona a cobertura Meta pelo número persistido e preserva o fallback histórico por ordem.
   */
  private JsonNode metaCoverageForAttempt(JsonNode coverages, int attemptNumber) {
    if (!coverages.isArray()) return objectMapper.createObjectNode();
    for (JsonNode coverage : coverages) {
      if (coverage.path("attemptNumber").asInt(-1) == attemptNumber) return coverage;
    }
    int historicalIndex = attemptNumber - 1;
    return historicalIndex >= 0 && historicalIndex < coverages.size()
        ? coverages.get(historicalIndex)
        : objectMapper.createObjectNode();
  }

  /** Converte uma candidata na visão gerencial até a validação privada posterior. */
  private IndependentBusinessProcessFlowReportResponse.Candidate candidate(
      ProductDiscoveryOpportunity opportunity,
      OpportunityDossier dossier,
      Map<String, AgentTask> tasks,
      Long selectedDossierId) {
    JsonNode evidence = read(opportunity.getEvidenceJson(), "evidência", opportunity.getId());
    JsonNode candidateEvidence = evidence.path("candidateEvidence");
    boolean selected = dossier != null && Objects.equals(dossier.getId(), selectedDossierId);
    List<IndependentBusinessProcessFlowReportResponse.Stage> stages = new ArrayList<>();
    stages.add(
        stage(
            "ARGOS",
            "Pesquisa factual",
            "Argos",
            tasks.get("marketEvidence"),
            opportunity.getDecision().name(),
            opportunity.getMaturity().name(),
            null));
    stages.add(
        candidateStage(
            "ATENA",
            "Escolha para protótipo privado",
            "Atena",
            tasks.get("marketStrategy"),
            selected,
            selectedDossierId));
    stages.add(
        candidateStage(
            "PLUTUS",
            "Economia e limites",
            "Plutus",
            tasks.get("economics"),
            selected,
            selectedDossierId));
    stages.add(
        candidateStage(
            "DEDALO",
            "Harness e experiência PDE",
            "Dédalo",
            tasks.get("productArchitecture"),
            selected,
            selectedDossierId));
    stages.add(productStage(dossier, selected, selectedDossierId));
    stages.add(privateValidationStage(dossier, selected, selectedDossierId, evidence));
    return new IndependentBusinessProcessFlowReportResponse.Candidate(
        opportunity.getId(),
        opportunity.getName(),
        opportunity.getPrimaryAudience(),
        opportunity.getRootPain(),
        opportunity.getScore(),
        opportunity.getMaturity().name(),
        opportunity.getDecision().name(),
        optionalText(candidateEvidence, "purchaseSituation"),
        strings(candidateEvidence.path("observedLanguage")),
        strings(candidateEvidence.path("currentAlternatives")),
        optionalText(candidateEvidence, "residualEffort"),
        optionalText(candidateEvidence, "instagramFitEvidence"),
        opportunity.getCommercialRisk(),
        dossier == null ? null : dossier.getId(),
        dossier == null ? null : dossier.getStatus().name(),
        dossier == null || dossier.getConvertedPlan() == null
            ? null
            : dossier.getConvertedPlan().getId(),
        dossier == null || dossier.getCreatedProduct() == null
            ? null
            : dossier.getCreatedProduct().getId(),
        dossier == null || dossier.getCreatedProduct() == null
            ? null
            : dossier.getCreatedProduct().getName(),
        dossier == null || dossier.getCreatedProduct() == null
            ? null
            : dossier.getCreatedProduct().getCommercialStatus(),
        nextAction(opportunity, dossier, selected, selectedDossierId, tasks),
        sources(evidence),
        List.copyOf(stages));
  }

  /** Monta um gate por candidata sem atribuir o parecer da vencedora às demais. */
  private IndependentBusinessProcessFlowReportResponse.Stage candidateStage(
      String code,
      String label,
      String agent,
      AgentTask task,
      boolean selected,
      Long selectedDossierId) {
    if (selectedDossierId != null && !selected) {
      return new IndependentBusinessProcessFlowReportResponse.Stage(
          code,
          label,
          agent,
          "NOT_SELECTED",
          "NOT_SELECTED",
          "Atena preservou esta candidata, mas priorizou outro dossiê para o plano atual.",
          task == null ? null : task.getId(),
          null,
          null,
          task == null ? null : task.getUpdatedAt());
    }
    if (selectedDossierId == null && !"ATENA".equals(code)) {
      return new IndependentBusinessProcessFlowReportResponse.Stage(
          code,
          label,
          agent,
          "WAITING",
          null,
          "Aguardando Atena selecionar uma candidata factual.",
          task == null ? null : task.getId(),
          null,
          null,
          task == null ? null : task.getUpdatedAt());
    }
    return stage(code, label, agent, task, null, null, null);
  }

  /** Expõe o produto somente para a candidata selecionada e realmente materializada. */
  private IndependentBusinessProcessFlowReportResponse.Stage productStage(
      OpportunityDossier dossier, boolean selected, Long selectedDossierId) {
    if (selectedDossierId != null && !selected) {
      return new IndependentBusinessProcessFlowReportResponse.Stage(
          "PRODUCT",
          "Produto planejado",
          "Backend",
          "NOT_SELECTED",
          null,
          "Nenhum produto é criado para candidata não priorizada.",
          null,
          null,
          null,
          dossier == null ? null : dossier.getUpdatedAt());
    }
    if (dossier != null && dossier.getCreatedProduct() != null) {
      return new IndependentBusinessProcessFlowReportResponse.Stage(
          "PRODUCT",
          "Produto planejado",
          "Backend",
          "COMPLETED",
          "PLANNED",
          "Produto #"
              + dossier.getCreatedProduct().getId()
              + " criado em PLANNED, sem publicação ou gasto.",
          null,
          null,
          null,
          dossier.getUpdatedAt());
    }
    return new IndependentBusinessProcessFlowReportResponse.Stage(
        "PRODUCT",
        "Produto planejado",
        "Backend",
        "WAITING",
        null,
        "Aguardando aprovação sequencial de Atena, Plutus e Dédalo.",
        null,
        null,
        null,
        dossier == null ? null : dossier.getUpdatedAt());
  }

  /** Expõe o gate privado sem interpretar pesquisa ou anúncio como uso real do produto. */
  private IndependentBusinessProcessFlowReportResponse.Stage privateValidationStage(
      OpportunityDossier dossier, boolean selected, Long selectedDossierId, JsonNode evidence) {
    if (selectedDossierId != null && !selected) {
      return new IndependentBusinessProcessFlowReportResponse.Stage(
          "PURCHASE_MOMENT",
          "Validação privada do Momento de Compra",
          "Backend · Psique · Têmis",
          "NOT_SELECTED",
          "NOT_SELECTED",
          "A validação fica preservada para uma rodada futura desta candidata.",
          null,
          null,
          null,
          dossier == null ? null : dossier.getUpdatedAt());
    }
    if (dossier == null || dossier.getCreatedProduct() == null) {
      return new IndependentBusinessProcessFlowReportResponse.Stage(
          "PURCHASE_MOMENT",
          "Validação privada do Momento de Compra",
          "Backend · Psique · Têmis",
          "WAITING",
          "AGUARDAR_PROTOTIPO",
          "Aguardando produto PLANNED e protótipo privado utilizável.",
          null,
          null,
          null,
          dossier == null ? null : dossier.getUpdatedAt());
    }
    JsonNode gate = evidence.path("purchaseMomentGate");
    boolean completed =
        "PASS".equals(gate.path("status").asText())
            && gate.path("sourceQualityPassed").asBoolean(false)
            && gate.path("finalPrioritizationEligible").asBoolean(false);
    boolean sourceQualityPassed = gate.path("sourceQualityPassed").asBoolean(false);
    return new IndependentBusinessProcessFlowReportResponse.Stage(
        "PURCHASE_MOMENT",
        "Validação privada do Momento de Compra",
        "Backend · Psique · Têmis",
        completed ? "COMPLETED" : "WAITING",
        completed ? "CONTINUAR" : "AGUARDAR_VALIDACAO",
        completed
            ? "Duas leituras privadas consistentes e os gates independentes foram comprovados."
            : "O produto planejado ainda precisa de protótipo utilizável e duas leituras privadas independentes; checkout de teste não conta como venda.",
        null,
        null,
        completed
            ? null
            : sourceQualityPassed
                ? "Construir o protótipo privado e registrar duas leituras acima dos critérios predeclarados."
                : "Atualizar as fontes comerciais antes das leituras e construir o protótipo privado em paralelo.",
        dossier.getUpdatedAt());
  }

  /** Converte uma tarefa persistida em gate legível, com decisão e bloqueio reais. */
  private IndependentBusinessProcessFlowReportResponse.Stage stage(
      String code,
      String label,
      String agent,
      AgentTask task,
      String fallbackDecision,
      String fallbackSummary,
      String fallbackBlocker) {
    JsonNode result =
        task == null
            ? objectMapper.createObjectNode()
            : read(task.getResultJson(), "resultado", task.getId());
    String decision = firstText(optionalText(result, "decision"), fallbackDecision);
    String summary =
        firstText(
            optionalText(result, "rationale"),
            firstText(optionalText(result, "selectedApproach"), fallbackSummary));
    return new IndependentBusinessProcessFlowReportResponse.Stage(
        code,
        label,
        agent,
        task == null ? "NOT_STARTED" : task.getStatus(),
        decision,
        summary,
        task == null ? null : task.getId(),
        task == null ? null : task.getEstimatedCostUsd(),
        task == null
            ? fallbackBlocker
            : firstText(task.getBlockerAction(), task.getExecutionError()),
        task == null ? null : task.getUpdatedAt());
  }

  /** Lista fontes Web, marketplace, Meta e acervo interno sem duplicar URLs. */
  private List<IndependentBusinessProcessFlowReportResponse.Source> sources(JsonNode evidence) {
    Map<String, IndependentBusinessProcessFlowReportResponse.Source> sources =
        new LinkedHashMap<>();
    JsonNode referenced =
        evidence.path("referencedEvidence").isObject()
            ? evidence.path("referencedEvidence")
            : evidence;
    addSources(sources, "WEB", referenced.path("publicEvidence"), "title", "snippet", "url");
    addSources(
        sources,
        "MARKETPLACE",
        referenced.path("marketplaceOffers"),
        "title",
        "tractionSignal",
        "url");
    addSources(
        sources,
        "META",
        referenced.path("metaAdEvidence"),
        "advertiserName",
        "adText",
        "adLibraryUrl");
    addSources(
        sources, "PESQUISAS", referenced.path("repositoryEvidence"), "title", "excerpt", "path");
    return List.copyOf(sources.values());
  }

  /** Acrescenta uma coleção de fontes com nomes de campo próprios da origem. */
  private void addSources(
      Map<String, IndependentBusinessProcessFlowReportResponse.Source> target,
      String type,
      JsonNode items,
      String titleField,
      String evidenceField,
      String urlField) {
    if (!items.isArray()) return;
    for (JsonNode item : items) {
      String url = optionalText(item, urlField);
      if (url == null && "PESQUISAS".equals(type)) {
        url = optionalText(item, "sourceReference");
      }
      String title = firstText(optionalText(item, titleField), type);
      String evidence = optionalText(item, evidenceField);
      String key = type + ":" + firstText(url, title + ":" + evidence);
      target.putIfAbsent(
          key, new IndependentBusinessProcessFlowReportResponse.Source(type, title, url, evidence));
    }
  }

  /** Consolida a cobertura das quatro fontes obrigatórias de Argos. */
  private List<IndependentBusinessProcessFlowReportResponse.SourceCoverage> sourceCoverage(
      ProductDiscoveryCycle cycle,
      JsonNode report,
      List<ProductDiscoveryOpportunity> opportunities) {
    int web = count(report, "publicEvidence", opportunities, "publicEvidence");
    int offers = count(report, "marketplaceOffers", opportunities, "marketplaceOffers");
    int meta = count(report, "metaAdEvidence", opportunities, "metaAdEvidence");
    int repository = count(report, "repositoryEvidence", opportunities, "repositoryEvidence");
    return List.of(
        coverage("WEB", "Internet", cycle, web, "Fontes públicas independentes coletadas."),
        metaCoverage(cycle, report, meta),
        coverage(
            "PESQUISAS",
            "Acervo /pesquisas",
            cycle,
            repository,
            "Referências versionadas usadas para abrir e confrontar hipóteses."),
        coverage(
            "MARKETPLACE",
            "Ofertas comparáveis",
            cycle,
            offers,
            "Alternativas pagas públicas; oferta observada não comprova venda."));
  }

  /** Distingue pesquisa Meta vazia, espera de observação e integração não executada. */
  private IndependentBusinessProcessFlowReportResponse.SourceCoverage metaCoverage(
      ProductDiscoveryCycle cycle, JsonNode report, int count) {
    if (count > 0) {
      return new IndependentBusinessProcessFlowReportResponse.SourceCoverage(
          "META",
          "Biblioteca Meta / Instagram",
          "OBSERVED",
          count,
          "Anúncios públicos observados; atividade não equivale a venda.");
    }
    JsonNode attempts = report.path("metaCoverage");
    int attemptCount = attempts.isArray() ? attempts.size() : 0;
    boolean unavailable = false;
    boolean awaiting = false;
    boolean observedEmpty = attemptCount > 0;
    if (attempts.isArray()) {
      for (JsonNode attempt : attempts) {
        String status = attempt.path("sourceStatus").asText("");
        unavailable |= "UNAVAILABLE".equals(status);
        awaiting |= status.startsWith("AWAITING_");
        observedEmpty &=
            List.of("NO_MATCHING_ACTIVE_ADS", "NO_ACTIVE_ADS", "NO_RELEVANT_PLATFORM_EVIDENCE")
                .contains(status);
      }
    }
    if (unavailable) {
      return new IndependentBusinessProcessFlowReportResponse.SourceCoverage(
          "META",
          "Biblioteca Meta / Instagram",
          "UNAVAILABLE",
          0,
          attemptCount
              + " tentativa(s) não chegaram à observação da Biblioteca; falha de integração não comprova ausência de mercado.");
    }
    if (awaiting) {
      return new IndependentBusinessProcessFlowReportResponse.SourceCoverage(
          "META",
          "Biblioteca Meta / Instagram",
          "AWAITING_OBSERVATION",
          0,
          attemptCount
              + " tentativa(s) aguardam navegador público, coleta oficial ou observação supervisionada.");
    }
    if (observedEmpty) {
      return new IndependentBusinessProcessFlowReportResponse.SourceCoverage(
          "META",
          "Biblioteca Meta / Instagram",
          "OBSERVED_EMPTY",
          0,
          attemptCount
              + " consulta(s) executadas sem anúncio ativo aderente; isso não comprova ausência de mercado.");
    }
    return coverage(
        "META",
        "Biblioteca Meta / Instagram",
        cycle,
        0,
        "Nenhum anúncio aderente foi comprovado e não há cobertura executada auditável.");
  }

  /** Produz o estado de cobertura sem transformar fonte vazia em aprovação. */
  private IndependentBusinessProcessFlowReportResponse.SourceCoverage coverage(
      String code, String label, ProductDiscoveryCycle cycle, int count, String summary) {
    String status =
        count > 0
            ? "OBSERVED"
            : cycle.getStatus() == ProductDiscoveryCycleStatus.COMPLETED
                ? "MISSING"
                : cycle.getStatus() == ProductDiscoveryCycleStatus.FAILED
                    ? "BLOCKED"
                    : "IN_PROGRESS";
    return new IndependentBusinessProcessFlowReportResponse.SourceCoverage(
        code, label, status, count, summary);
  }

  /** Conta itens no relatório do ciclo e usa as candidatas apenas como fallback histórico. */
  private int count(
      JsonNode report,
      String reportField,
      List<ProductDiscoveryOpportunity> opportunities,
      String evidenceField) {
    if (report.path(reportField).isArray()) return report.path(reportField).size();
    return opportunities.stream()
        .map(item -> read(item.getEvidenceJson(), "evidência", item.getId()).path(evidenceField))
        .filter(JsonNode::isArray)
        .mapToInt(JsonNode::size)
        .max()
        .orElse(0);
  }

  /** Define o próximo movimento pela maturidade e pelos gates persistidos. */
  private String nextAction(
      ProductDiscoveryOpportunity opportunity,
      OpportunityDossier dossier,
      boolean selected,
      Long selectedDossierId,
      Map<String, AgentTask> tasks) {
    if (opportunity.getMaturity() != ProductDiscoveryOpportunityMaturity.DOSSIER_READY) {
      return "Argos deve aprofundar as lacunas factuais antes de liberar Atena.";
    }
    if (selectedDossierId != null && !selected) {
      return "Candidata preservada para comparação futura; o ciclo atual priorizou outro dossiê.";
    }
    if (dossier != null && dossier.getCreatedProduct() != null) {
      JsonNode gate =
          read(opportunity.getEvidenceJson(), "evidência", opportunity.getId())
              .path("purchaseMomentGate");
      if (!gate.path("sourceQualityPassed").asBoolean(false)) {
        return "Abrir a cadeia de valor do produto, atualizar as fontes comerciais e construir o protótipo privado; depois registrar duas leituras independentes.";
      }
      return "Abrir a cadeia de valor do produto, construir o protótipo privado e registrar duas leituras independentes antes da priorização comercial final.";
    }
    for (String activity : List.of("marketStrategy", "economics", "productArchitecture")) {
      AgentTask task = tasks.get(activity);
      if (task != null && "BLOCKED".equals(task.getStatus())) {
        if ("marketStrategy".equals(activity)
            && opportunity.getMaturity() == ProductDiscoveryOpportunityMaturity.DOSSIER_READY) {
          return "Retentar Atena com o contrato de protótipo privado: uso, preferência e checkout são gates posteriores, não pré-requisitos da estratégia.";
        }
        return firstText(
            task.getBlockerAction(), "Corrigir o bloqueio persistido e reiniciar o gate.");
      }
    }
    if (selectedDossierId == null) return "Aguardar Atena priorizar no máximo uma candidata.";
    if (!"COMPLETED".equals(status(tasks.get("economics"))))
      return "Aguardar Plutus validar a economia.";
    if (!"COMPLETED".equals(status(tasks.get("productArchitecture")))) {
      return "Aguardar Dédalo projetar o harness e a experiência PDE.";
    }
    return "Aguardar o backend materializar o produto planejado.";
  }

  /** Consolida o estado funcional da cadeia sem tratar pesquisa como venda. */
  private String reportStatus(
      ProductDiscoveryCycle cycle, List<AgentTask> tasks, int ready, int products) {
    if (cycle.getStatus() == ProductDiscoveryCycleStatus.FAILED) return "BLOCKED";
    if (tasks.stream().anyMatch(task -> "BLOCKED".equals(task.getStatus()))) return "BLOCKED";
    if (tasks.stream().anyMatch(task -> "IN_PROGRESS".equals(task.getStatus())))
      return "IN_PROGRESS";
    if (tasks.stream().anyMatch(task -> "PENDING".equals(task.getStatus()))) return "PENDING";
    if (ready == 0 && cycle.getStatus() == ProductDiscoveryCycleStatus.COMPLETED) return "BLOCKED";
    if (products > 0) return "COMPLETED";
    return cycle.getStatus() == ProductDiscoveryCycleStatus.COMPLETED ? "PENDING" : "IN_PROGRESS";
  }

  /** Mantém na projeção leve as mesmas regras funcionais usadas pelo relatório detalhado. */
  private String summaryStatus(
      ProductDiscoveryIndependentStatusProjection snapshot, String technicalStatus) {
    if (ProductDiscoveryCycleStatus.FAILED.name().equals(snapshot.getCycleStatus())) {
      return "BLOCKED";
    }
    if (List.of("BLOCKED", "IN_PROGRESS", "PENDING").contains(technicalStatus)) {
      return technicalStatus;
    }
    boolean completedCycle =
        ProductDiscoveryCycleStatus.COMPLETED.name().equals(snapshot.getCycleStatus());
    if (completedCycle && snapshot.getReadyOpportunityCount() == 0) return "BLOCKED";
    if (snapshot.getProductCount() > 0) return "COMPLETED";
    return completedCycle ? "PENDING" : "IN_PROGRESS";
  }

  /** Indexa a tentativa mais recente de cada atividade da mesma execução. */
  private Map<String, AgentTask> latestTasks(List<AgentTask> tasks) {
    Map<String, AgentTask> latest = new LinkedHashMap<>();
    tasks.stream()
        .sorted(Comparator.comparing(AgentTask::getId))
        .filter(task -> task.getProcessActivityId() != null)
        .forEach(task -> latest.put(task.getProcessActivityId(), task));
    return latest;
  }

  /** Lê a seleção de Atena somente depois de uma resposta estruturada. */
  private Long selectedDossierId(AgentTask task) {
    if (task == null || task.getResultJson() == null) return null;
    JsonNode result = read(task.getResultJson(), "resultado", task.getId());
    return result.path("selectedDossierId").canConvertToLong()
        ? result.path("selectedDossierId").longValue()
        : null;
  }

  /** Localiza o dossiê exato da candidata atual, preservando reanálises históricas. */
  private OpportunityDossier dossier(
      ProductDiscoveryOpportunity opportunity, List<OpportunityDossier> dossiers) {
    return dossiers.stream()
        .filter(item -> item.getProductDiscoveryOpportunity() != null)
        .filter(item -> opportunity.getId().equals(item.getProductDiscoveryOpportunity().getId()))
        .findFirst()
        .orElse(null);
  }

  /** Exige uma referência canônica de ciclo para impedir mistura entre execuções. */
  private ProductDiscoveryCycle requiredCycle(String sourceReference) {
    Long cycleId = cycleId(sourceReference);
    return cycleRepository
        .findById(cycleId)
        .orElseThrow(() -> new IllegalArgumentException("Ciclo de Argos não foi encontrado."));
  }

  /** Extrai a identidade canônica do ciclo sem consultar seus campos extensos. */
  private Long cycleId(String sourceReference) {
    if (sourceReference == null || !sourceReference.startsWith(SOURCE_PREFIX)) {
      throw new IllegalArgumentException("Execução independente não referencia um ciclo de Argos.");
    }
    return Long.valueOf(sourceReference.substring(SOURCE_PREFIX.length()));
  }

  /** Interpreta JSON opcional e registra corrupção histórica sem esconder a execução inteira. */
  private JsonNode read(String value, String field, Long referenceId) {
    if (value == null || value.isBlank()) return objectMapper.createObjectNode();
    try {
      return objectMapper.readTree(value);
    } catch (JsonProcessingException ex) {
      log.error(
          "Falha ao interpretar JSON do relatório independente. campo={} referenceId={}",
          field,
          referenceId,
          ex);
      return objectMapper.createObjectNode();
    }
  }

  /** Converte um array textual em lista estável. */
  private List<String> strings(JsonNode values) {
    if (!values.isArray()) return List.of();
    List<String> result = new ArrayList<>();
    values.forEach(
        value -> {
          String text = value.asText("").trim();
          if (!text.isBlank()) result.add(text);
        });
    return List.copyOf(result);
  }

  /** Lê texto opcional preservando ausência real. */
  private String optionalText(JsonNode node, String field) {
    String value = node.path(field).asText("").trim();
    return value.isBlank() ? null : value;
  }

  /** Retorna o primeiro texto preenchido. */
  private String firstText(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value;
  }

  /** Retorna o estado técnico ou ausência explícita da tarefa. */
  private String status(AgentTask task) {
    return task == null ? "NOT_STARTED" : task.getStatus();
  }
}
