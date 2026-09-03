package com.marketinghub.opportunitydossier.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agenttask.AgentTask;
import com.marketinghub.agenttask.AgentTaskService;
import com.marketinghub.agenttask.CreateAgentTaskRequest;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.opportunitydossier.OpportunityDossier;
import com.marketinghub.opportunitydossier.OpportunityDossierStatus;
import com.marketinghub.opportunitydossier.OpportunityEvidence;
import com.marketinghub.productdiscovery.v1.ProductDiscoveryOpportunity;
import com.marketinghub.productdiscovery.v1.ProductDiscoveryOpportunityMaturity;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessDefinitionRepository;
import com.marketinghub.repository.jpa.opportunitydossier.OpportunityDossierRepository;
import com.marketinghub.repository.jpa.opportunitydossier.OpportunityEvidenceRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Responsabilidade: materializar dossiês e o handoff governado das candidatas factuais de Argos.
 */
@Service
public class OpportunityDossierResearchSyncService {
  private static final Logger log =
      LoggerFactory.getLogger(OpportunityDossierResearchSyncService.class);
  private static final String COMMERCIAL_PROCESS_CODE = "pde-commercial-plan-offer";
  private static final List<String> PRIVATE_VALIDATION_SIGNALS =
      List.of(
          "EXPERIENCE_STARTED",
          "VALUE_MOMENT",
          "READY_RESULT_USED",
          "PREFERRED_OVER_FREE",
          "CHECKOUT_STARTED");
  private final OpportunityDossierRepository dossierRepository;
  private final OpportunityEvidenceRepository evidenceRepository;
  private final AgentTaskRepository taskRepository;
  private final BusinessProcessDefinitionRepository processRepository;
  private final AgentTaskService agentTaskService;
  private final ObjectMapper objectMapper;

  /** Configura as fontes que preservam dossiê, evidência e tarefas sequenciais. */
  public OpportunityDossierResearchSyncService(
      OpportunityDossierRepository dossierRepository,
      OpportunityEvidenceRepository evidenceRepository,
      AgentTaskRepository taskRepository,
      BusinessProcessDefinitionRepository processRepository,
      AgentTaskService agentTaskService,
      ObjectMapper objectMapper) {
    this.dossierRepository = dossierRepository;
    this.evidenceRepository = evidenceRepository;
    this.taskRepository = taskRepository;
    this.processRepository = processRepository;
    this.agentTaskService = agentTaskService;
    this.objectMapper = objectMapper;
  }

  /**
   * Cria um dossiê por candidata e libera a cadeia apenas quando Argos informa prontidão factual.
   */
  @Transactional
  public void synchronize(Long cycleId, List<ProductDiscoveryOpportunity> opportunities) {
    List<CandidateHandoff> candidates = new ArrayList<>();
    for (ProductDiscoveryOpportunity opportunity : opportunities) {
      OpportunityDossier dossier = materializeDossier(cycleId, opportunity);
      persistSources(dossier, opportunity, cycleId);
      candidates.add(new CandidateHandoff(dossier, opportunity));
      finishResearchTask(dossier.getId(), "COMPLETED");
    }
    if (candidates.stream()
        .anyMatch(
            candidate ->
                candidate.opportunity().getMaturity()
                    == ProductDiscoveryOpportunityMaturity.DOSSIER_READY)) {
      openCommercialTasks(cycleId, candidates);
    }
  }

  /** Materializa ou atualiza a mesma candidata sem duplicar dossiê após reanálise do ciclo. */
  private OpportunityDossier materializeDossier(
      Long cycleId, ProductDiscoveryOpportunity opportunity) {
    OpportunityDossier dossier =
        dossierRepository
            .findByProductDiscoveryOpportunityId(opportunity.getId())
            .or(
                () ->
                    dossierRepository.findFirstByProductDiscoveryCycleIdAndTitleIgnoreCase(
                        cycleId, opportunity.getName()))
            .orElseGet(OpportunityDossier::new);
    boolean newDossier = dossier.getId() == null;
    dossier.setTitle(opportunity.getName());
    dossier.setOwnerAgentKey("ARGOS");
    dossier.setTargetAudience(opportunity.getPrimaryAudience());
    dossier.setMainPain(opportunity.getRootPain());
    dossier.setReferenceProduct(referenceProduct(opportunity));
    dossier.setAiAdvantage(
        textOrFallback(
            opportunity.getPdeExperience(),
            "Argos ainda não comprovou uma fronteira de valor PDE para esta candidata."));
    dossier.setKnownRisks(opportunity.getCommercialRisk());
    dossier.setExperimentRecommendation(
        opportunity.getMaturity() == ProductDiscoveryOpportunityMaturity.DOSSIER_READY
            ? "Atena seleciona para protótipo privado; Plutus limita a economia e Dédalo projeta o harness. Duas leituras independentes permanecem obrigatórias antes da priorização comercial final."
            : "Aprofundar as lacunas factuais registradas por Argos antes do handoff comercial.");
    dossier.setProductDiscoveryCycle(opportunity.getCycle());
    dossier.setProductDiscoveryOpportunity(opportunity);
    if (newDossier) {
      dossier.setStatus(
          opportunity.getMaturity() == ProductDiscoveryOpportunityMaturity.DOSSIER_READY
              ? OpportunityDossierStatus.UNDER_REVIEW
              : OpportunityDossierStatus.RESEARCHING);
    } else if (dossier.getStatus() == OpportunityDossierStatus.RESEARCHING
        && opportunity.getMaturity() == ProductDiscoveryOpportunityMaturity.DOSSIER_READY) {
      dossier.setStatus(OpportunityDossierStatus.UNDER_REVIEW);
    }
    return dossierRepository.save(dossier);
  }

  /** Resolve uma alternativa paga observada ou declara explicitamente que ela ainda não existe. */
  private String referenceProduct(ProductDiscoveryOpportunity opportunity) {
    JsonNode offers = readEvidence(opportunity).path("marketplaceOffers");
    if (offers.isArray() && !offers.isEmpty()) {
      String title = offers.get(0).path("title").asText("").trim();
      if (!title.isBlank()) return limit(title, 512);
    }
    return "Nenhuma oferta comparável confirmada por Argos.";
  }

  /** Persiste cada fonte real individualmente, com idempotência e proveniência. */
  private void persistSources(
      OpportunityDossier dossier, ProductDiscoveryOpportunity opportunity, Long cycleId) {
    JsonNode root = readEvidence(opportunity);
    JsonNode sources =
        root.path("referencedEvidence").isObject() ? root.path("referencedEvidence") : root;
    persistPublicSources(dossier, opportunity, cycleId, sources.path("publicEvidence"));
    persistMarketplaceOffers(dossier, opportunity, cycleId, sources.path("marketplaceOffers"));
    persistMetaAds(dossier, opportunity, cycleId, sources.path("metaAdEvidence"));
    persistResearchLibrary(dossier, opportunity, cycleId, sources.path("repositoryEvidence"));
  }

  /** Lê o envelope factual sem transformar corrupção de payload em evidência inexistente. */
  private JsonNode readEvidence(ProductDiscoveryOpportunity opportunity) {
    if (opportunity.getEvidenceJson() == null || opportunity.getEvidenceJson().isBlank()) {
      return objectMapper.createObjectNode();
    }
    try {
      return objectMapper.readTree(opportunity.getEvidenceJson());
    } catch (JsonProcessingException ex) {
      log.error(
          "Falha ao interpretar evidência de Argos. cycleId={} opportunityId={}",
          opportunity.getCycle().getId(),
          opportunity.getId(),
          ex);
      throw new IllegalArgumentException(
          "Evidência de candidata de Argos contém JSON inválido.", ex);
    }
  }

  /** Persiste fontes públicas individualmente sem perder a URL original. */
  private void persistPublicSources(
      OpportunityDossier dossier,
      ProductDiscoveryOpportunity opportunity,
      Long cycleId,
      JsonNode sources) {
    if (!sources.isArray()) return;
    for (JsonNode source : sources) {
      persistEvidence(
          dossier,
          source.path("url").asText(""),
          opportunity.getName()
              + " — "
              + source.path("snippet").asText(source.path("title").asText("")),
          "ARGOS:web:product-discovery-cycle:" + cycleId);
    }
  }

  /** Persiste ofertas comparáveis com preço e tração observados. */
  private void persistMarketplaceOffers(
      OpportunityDossier dossier,
      ProductDiscoveryOpportunity opportunity,
      Long cycleId,
      JsonNode offers) {
    if (!offers.isArray()) return;
    for (JsonNode offer : offers) {
      String summary =
          "%s — %s | preço=%s | tração=%s | coleta=%s"
              .formatted(
                  offer.path("marketplace").asText("MARKETPLACE"),
                  offer.path("title").asText("Oferta sem título"),
                  offer.path("price").asText("não informado"),
                  offer.path("tractionSignal").asText("não informado"),
                  offer.path("collectedAt").asText("não informada"));
      persistEvidence(
          dossier,
          offer.path("url").asText(""),
          opportunity.getName() + " — " + summary,
          "ARGOS:marketplace:product-discovery-cycle:" + cycleId);
    }
  }

  /** Persiste anúncios públicos observados sem tratá-los como venda ou receita. */
  private void persistMetaAds(
      OpportunityDossier dossier,
      ProductDiscoveryOpportunity opportunity,
      Long cycleId,
      JsonNode ads) {
    if (!ads.isArray()) return;
    for (JsonNode ad : ads) {
      String summary =
          "%s — anunciante=%s | texto=%s | plataformas=%s"
              .formatted(
                  opportunity.getName(),
                  ad.path("advertiserName").asText("não informado"),
                  ad.path("adText").asText(ad.path("text").asText("não informado")),
                  ad.path("publisherPlatforms").toString());
      persistEvidence(
          dossier,
          ad.path("adLibraryUrl").asText(ad.path("url").asText("")),
          summary,
          "ARGOS:meta-browser:product-discovery-cycle:" + cycleId);
    }
  }

  /** Persiste somente os artigos do acervo versionado citados pela candidata. */
  private void persistResearchLibrary(
      OpportunityDossier dossier,
      ProductDiscoveryOpportunity opportunity,
      Long cycleId,
      JsonNode references) {
    if (!references.isArray()) return;
    for (JsonNode reference : references) {
      String path = reference.path("path").asText(reference.path("sourceReference").asText(""));
      String summary =
          opportunity.getName()
              + " — "
              + reference.path("title").asText("Artigo do acervo /pesquisas")
              + " | "
              + reference.path("excerpt").asText("Trecho citado por Argos");
      persistEvidence(
          dossier, path, summary, "ARGOS:research-library:product-discovery-cycle:" + cycleId);
    }
  }

  /** Salva uma evidência somente quando URL e resumo reais estão presentes. */
  private void persistEvidence(
      OpportunityDossier dossier, String rawUrl, String rawSummary, String createdBy) {
    String url = rawUrl == null ? "" : rawUrl.trim();
    String summary = rawSummary == null ? "" : rawSummary.trim();
    if (url.isBlank()
        || summary.isBlank()
        || evidenceRepository.existsByDossierIdAndSourceUrl(dossier.getId(), url)) {
      return;
    }
    evidenceRepository.save(
        OpportunityEvidence.builder()
            .dossier(dossier)
            .sourceUrl(url)
            .summary(summary)
            .createdBy(createdBy)
            .build());
  }

  /** Abre idempotentemente os três gates do plano; predecessoras controlam a reserva sequencial. */
  private void openCommercialTasks(Long cycleId, List<CandidateHandoff> candidates) {
    BusinessProcessDefinition process =
        processRepository
            .findFirstByProcessCodeAndStatusOrderByVersionNumberDesc(
                COMMERCIAL_PROCESS_CODE, "PUBLISHED")
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "Processo comercial PDE publicado não foi encontrado para o handoff."));
    String sourceReference = "product-discovery-cycle:" + cycleId;
    String context = taskContext(cycleId, candidates);
    List<AgentTask> existingTasks =
        taskRepository.findBySourceReferenceOrderByCreatedAtAscIdAsc(sourceReference);
    boolean restartFromAtena = requiresCurrentStrategy(process, existingTasks);
    if (restartFromAtena) {
      agentTaskService.cancelActiveTasksBySourceReference(
          sourceReference,
          "Cadeia reiniciada porque a estratégia concluída usa contrato anterior ao MARKET_STRATEGY_V3.");
    }
    createTask(
        process,
        sourceReference,
        "experiment-strategist",
        "marketStrategy",
        "Atena · selecionar protótipo privado do ciclo #" + cycleId,
        "Escolha no máximo um dossiê factual para prototipação privada, predeclare duas leituras e preserve os fatos de Argos. A falta dessas leituras ainda não bloqueia esta atividade. Contexto: "
            + context,
        restartFromAtena);
    createTask(
        process,
        sourceReference,
        "financial-agent",
        "economics",
        "Plutus · validar economia do ciclo #" + cycleId,
        "Use somente a seleção MARKET_STRATEGY_V3 mais recente de Atena, entregue pelo contexto do processo. Valide preço de checkout simulado como hipótese, custo e travas da validação privada; não autorize campanha, gasto, pagamento ou venda.",
        restartFromAtena);
    createTask(
        process,
        sourceReference,
        "landing-generator",
        "productArchitecture",
        "Dédalo · projetar protótipo e harness do ciclo #" + cycleId,
        "Use a estratégia vigente de Atena e a economia aprovada por Plutus, entregues pelo contexto do processo. Projete somente o protótipo privado instrumentado e o harness PDE da candidata escolhida.",
        restartFromAtena);
  }

  /** Cria uma atividade regular e abre nova ocorrência somente após contrato anterior obsoleto. */
  private void createTask(
      BusinessProcessDefinition process,
      String sourceReference,
      String agentKey,
      String activityId,
      String title,
      String description,
      boolean forceNewAttempt) {
    CreateAgentTaskRequest request =
        new CreateAgentTaskRequest(
            agentKey,
            "Marketing Hub",
            limit(title, 160),
            description,
            "HIGH",
            sourceReference,
            process.getId(),
            activityId,
            false,
            null);
    if (forceNewAttempt) {
      agentTaskService.createByHuman(request);
    } else {
      agentTaskService.retryBlockedByHumanOrRefreshPending(request);
    }
  }

  /** Detecta estratégia concluída por runtime antigo antes de repetir apenas a etapa posterior. */
  private boolean requiresCurrentStrategy(
      BusinessProcessDefinition process, List<AgentTask> existingTasks) {
    if (process.getVersionNumber() == null || process.getVersionNumber() < 6) return false;
    AgentTask latestStrategy =
        existingTasks.stream()
            .filter(task -> "marketStrategy".equals(task.getProcessActivityId()))
            .max(java.util.Comparator.comparing(AgentTask::getId))
            .orElse(null);
    if (latestStrategy == null || !"COMPLETED".equals(latestStrategy.getStatus())) return false;
    try {
      JsonNode result = objectMapper.readTree(latestStrategy.getResultJson());
      JsonNode contract = result.path("marketStrategicContract");
      JsonNode plan = contract.path("privateValidationPlan");
      return !"MARKET_STRATEGY_V3".equals(contract.path("contractVersion").asText())
          || !"READY_FOR_PRIVATE_VALIDATION".equals(contract.path("status").asText())
          || plan.path("minimumIndependentReadings").asInt(0) != 2
          || !hasExactPrivateSignals(plan.path("requiredSignals"));
    } catch (Exception ex) {
      log.error(
          "Falha ao validar contrato concluído de Atena. taskId={} sourceReference={}",
          latestStrategy.getId(),
          latestStrategy.getSourceReference(),
          ex);
      return true;
    }
  }

  /** Confirma os cinco sinais canônicos antes de considerar a estratégia atual. */
  private boolean hasExactPrivateSignals(JsonNode values) {
    if (!values.isArray() || values.size() != PRIVATE_VALIDATION_SIGNALS.size()) return false;
    List<String> signals = new ArrayList<>();
    values.forEach(value -> signals.add(value.asText()));
    return signals.stream().distinct().count() == PRIVATE_VALIDATION_SIGNALS.size()
        && signals.containsAll(PRIVATE_VALIDATION_SIGNALS);
  }

  /** Serializa o dossiê factual entregue aos agentes sem esconder a evidência original. */
  private String taskContext(Long cycleId, List<CandidateHandoff> candidates) {
    Map<String, Object> context = new LinkedHashMap<>();
    context.put("cycleId", cycleId);
    context.put(
        "candidates",
        candidates.stream()
            .map(
                candidate -> {
                  ProductDiscoveryOpportunity opportunity = candidate.opportunity();
                  Map<String, Object> item = new LinkedHashMap<>();
                  item.put("dossierId", candidate.dossier().getId());
                  item.put("opportunityId", opportunity.getId());
                  item.put("maturity", opportunity.getMaturity());
                  item.put("name", opportunity.getName());
                  item.put("audience", opportunity.getPrimaryAudience());
                  item.put("rootPain", opportunity.getRootPain());
                  item.put("pdeBoundary", opportunity.getPdeExperience());
                  item.put("commercialRisk", opportunity.getCommercialRisk());
                  item.put("evidence", readEvidence(opportunity));
                  return item;
                })
            .toList());
    try {
      return objectMapper.writeValueAsString(context);
    } catch (JsonProcessingException ex) {
      log.error(
          "Falha ao serializar contexto do handoff. cycleId={} candidateCount={}",
          cycleId,
          candidates.size(),
          ex);
      throw new IllegalStateException("Não foi possível montar o contexto do handoff PDE.", ex);
    }
  }

  /** Marca a pesquisa manual como ativa quando o executor reserva o ciclo. */
  public void start(Long cycleId) {
    dossierRepository
        .findAllByProductDiscoveryCycleIdOrderByIdAsc(cycleId)
        .forEach(dossier -> finishResearchTask(dossier.getId(), "IN_PROGRESS"));
  }

  /** Expõe a falha da pesquisa nos dossiês já associados ao ciclo. */
  public void fail(Long cycleId) {
    dossierRepository
        .findAllByProductDiscoveryCycleIdOrderByIdAsc(cycleId)
        .forEach(dossier -> finishResearchTask(dossier.getId(), "BLOCKED"));
  }

  /** Atualiza somente a tarefa de Argos, sem tocar nos gates comerciais posteriores. */
  private void finishResearchTask(Long dossierId, String status) {
    taskRepository
        .findTopByAssignedAgentAgentKeyAndSourceReferenceOrderByUpdatedAtDescIdDesc(
            "market-radar", "opportunity-dossier:" + dossierId)
        .ifPresent(
            task -> {
              task.setStatus(status);
              task.setUpdatedAt(Instant.now());
              taskRepository.save(task);
            });
  }

  /** Preserva texto factual ou uma lacuna explícita, sem fabricar vantagem comercial. */
  private String textOrFallback(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value.trim();
  }

  /** Respeita o limite de título das tarefas sem alterar seu contexto detalhado. */
  private String limit(String value, int maxLength) {
    return value.length() <= maxLength ? value : value.substring(0, maxLength);
  }

  /** Agrupa o dossiê persistido com a candidata factual da mesma execução. */
  private record CandidateHandoff(
      OpportunityDossier dossier, ProductDiscoveryOpportunity opportunity) {}
}
