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
import com.marketinghub.repository.jpa.productdiscovery.ProductDiscoveryOpportunityRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
    return new IndependentBusinessProcessFlowReportResponse(
        "PDE_OPPORTUNITY_TO_PRODUCT_V1",
        reportStatus(cycle, List.copyOf(latestTasks.values()), ready, products),
        firstText(
            cycle.getDecisionSummary(),
            "Argos ainda está reunindo evidências para formar candidatas factuais."),
        firstText(cycle.getAcquisitionChannel(), "Instagram"),
        opportunities.size(),
        ready,
        products,
        sourceCoverage(cycle, evidenceReport, opportunities),
        candidates);
  }

  /** Converte uma candidata na visão gerencial com seus cinco estágios. */
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
            "Priorização de mercado",
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
        coverage(
            "META",
            "Biblioteca Meta / Instagram",
            cycle,
            meta,
            meta > 0
                ? "Anúncios públicos observados; atividade não equivale a venda."
                : "Nenhum anúncio aderente foi comprovado ou a fonte exige observação supervisionada."),
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
      return "Abrir o produto planejado, construir o protótipo privado e validar o Momento de Compra antes da priorização comercial final.";
    }
    for (String activity : List.of("marketStrategy", "economics", "productArchitecture")) {
      AgentTask task = tasks.get(activity);
      if (task != null && "BLOCKED".equals(task.getStatus())) {
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
    if (sourceReference == null || !sourceReference.startsWith(SOURCE_PREFIX)) {
      throw new IllegalArgumentException("Execução independente não referencia um ciclo de Argos.");
    }
    Long cycleId = Long.valueOf(sourceReference.substring(SOURCE_PREFIX.length()));
    return cycleRepository
        .findById(cycleId)
        .orElseThrow(() -> new IllegalArgumentException("Ciclo de Argos não foi encontrado."));
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
