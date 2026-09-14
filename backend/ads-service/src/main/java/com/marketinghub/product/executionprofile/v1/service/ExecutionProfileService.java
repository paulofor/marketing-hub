package com.marketinghub.product.executionprofile.v1.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.financialagent.FinancialAgentExecutionStatus;
import com.marketinghub.financialagent.service.*;
import com.marketinghub.product.executionprofile.v1.*;
import com.marketinghub.product.executionprofile.v1.service.bind.BindProfileRequest;
import com.marketinghub.product.executionprofile.v1.service.getprofile.ProfileView;
import com.marketinghub.product.executionprofile.v1.service.getprofile.ProfileView.*;
import com.marketinghub.product.executionprofile.v1.service.review.ReviewProfileRequest;
import com.marketinghub.product.executionprofile.v1.service.saveprofile.SaveProfileRequest;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.businessprocess.*;
import com.marketinghub.repository.jpa.businessprocesschain.BusinessProcessChainDefinitionRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.financialagent.FinancialAgentExecutionRepository;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository;
import com.marketinghub.repository.jpa.planning.*;
import com.marketinghub.repository.jpa.processautomation.ProcessRunRepository;
import com.marketinghub.repository.jpa.product.ProductRepository;
import com.marketinghub.repository.jpa.productexecution.*;
import jakarta.validation.Validator;
import java.time.Instant;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Responsabilidade: governar revisão, vínculo e decisão financeira da ficha de execução do produto.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExecutionProfileService {
  private final ExecutionProfileRepository profiles;
  private final ExecutionProfileBindingRepository bindings;
  private final ExecutionProfileReviewRepository reviews;
  private final ExecutionProfileConsumptionRepository consumption;
  private final ProductRepository products;
  private final CommercialPlanRepository plans;
  private final CommercialPlanVersionRepository planVersions;
  private final BusinessProcessChainDefinitionRepository chains;
  private final BusinessProcessDefinitionRepository processes;
  private final ExperimentRepository experiments;
  private final LearningSalesCycleRepository cycles;
  private final AgentTaskRepository tasks;
  private final ProcessRunRepository runs;
  private final FinancialAgentExecutionRepository financialExecutions;
  private final FinancialAgentService plutus;
  private final ExecutionProfileContext context;
  private final ExecutionProfileOmissions omissions;
  private final ExecutionProfileBudget budget;
  private final ObjectMapper json;
  private final Validator validator;

  /** Lista somente planos do produto e cadeias comuns aptas à criação de novas fichas. */
  @Transactional(readOnly = true)
  public com.marketinghub.product.executionprofile.v1.service.getprofile.ProfileCatalog catalog(
      Long productId) {
    requireProduct(productId);
    var options =
        new ArrayList<
            com.marketinghub.product.executionprofile.v1.service.getprofile.ProfileCatalog
                .Option>();
    for (var chain : chains.findAllByStatusOrderByNameAscVersionNumberDesc("PUBLISHED")) {
      var codes =
          chain.getItems().stream().map(i -> i.getProcessDefinition().getProcessCode()).toList();
      if ("PUBLISHED".equals(chain.getStatus())
          && codes.size() == 6
          && new HashSet<>(codes).equals(new HashSet<>(ExecutionProfileRules.PHASES)))
        options.add(
            new com.marketinghub.product.executionprofile.v1.service.getprofile.ProfileCatalog
                .Option(
                String.valueOf(chain.getId()),
                chain.getName() + " · v" + chain.getVersionNumber()));
    }
    return new com.marketinghub.product.executionprofile.v1.service.getprofile.ProfileCatalog(
        List.of(
            new com.marketinghub.product.executionprofile.v1.service.getprofile.ProfileCatalog
                .Option("PERSONALIZED_IMAGES", "Imagens personalizadas"),
            new com.marketinghub.product.executionprofile.v1.service.getprofile.ProfileCatalog
                .Option("GUIDED_EXPERIENCE", "Experiência guiada"),
            new com.marketinghub.product.executionprofile.v1.service.getprofile.ProfileCatalog
                .Option("AI_TOOL", "Ferramenta com IA"),
            new com.marketinghub.product.executionprofile.v1.service.getprofile.ProfileCatalog
                .Option("DIGITAL_PACKAGE", "Pacote digital")),
        options,
        plans.findByProductId(productId).stream()
            .map(
                p ->
                    new com.marketinghub.product.executionprofile.v1.service.getprofile
                        .ProfileCatalog.Option(String.valueOf(p.getId()), p.getName()))
            .toList(),
        com.marketinghub.imagegeneration.OpenAiImageGenerationPolicy.CANONICAL_MODEL,
        List.of(
            new com.marketinghub.product.executionprofile.v1.service.getprofile.ProfileCatalog
                .Option("OFFER", "Oferta"),
            new com.marketinghub.product.executionprofile.v1.service.getprofile.ProfileCatalog
                .Option("DELIVERY_DESIGN", "Desenho da entrega"),
            new com.marketinghub.product.executionprofile.v1.service.getprofile.ProfileCatalog
                .Option("HOMOLOGATION", "Homologação"),
            new com.marketinghub.product.executionprofile.v1.service.getprofile.ProfileCatalog
                .Option("OPERATION", "Operação")));
  }

  /** Lista fichas e históricos somente do produto solicitado. */
  @Transactional(readOnly = true)
  public List<ProfileView> list(Long productId) {
    requireProduct(productId);
    return profiles.findByProductIdOrderByRevisionNumberDesc(productId).stream()
        .map(this::view)
        .toList();
  }

  /** Retorna a ficha exata, sem trocar pela revisão mais recente. */
  @Transactional(readOnly = true)
  public ProfileView get(Long productId, Long profileId) {
    return view(required(productId, profileId));
  }

  /** Cria revisão imutável e congela os subprocessos que pertencem à cadeia selecionada. */
  @Transactional
  public ProfileView create(Long productId, SaveProfileRequest request) {
    validate(request);
    var product = products.findLockedById(productId).orElseThrow(() -> notFound("Produto"));
    if (!plans.findIdsByProductId(productId).contains(request.commercialPlanId()))
      throw conflict("O plano comercial não pertence ao produto.");
    var version =
        planVersions
            .findTopByPlanIdOrderByVersionNumberDesc(request.commercialPlanId())
            .orElseThrow(() -> conflict("O plano comercial ainda não possui versão."));
    var chain = chains.findById(request.chainId()).orElseThrow(() -> notFound("Cadeia"));
    if (!"PUBLISHED".equals(chain.getStatus()))
      throw conflict("Escolha uma versão publicada da cadeia.");
    var chainCodes =
        chain.getItems().stream().map(i -> i.getProcessDefinition().getProcessCode()).toList();
    if (chainCodes.size() != 6
        || !new HashSet<>(chainCodes).equals(new HashSet<>(ExecutionProfileRules.PHASES)))
      throw conflict("A ficha exige a cadeia comum de seis processos PDE.");
    LinkedHashMap<String, ProcessReference> composition = new LinkedHashMap<>();
    chain
        .getItems()
        .forEach(item -> collect(item.getProcessDefinition(), composition, new HashSet<>()));
    var profile = new ExecutionProfile();
    profile.setProductId(productId);
    var previous = profiles.findByProductIdOrderByRevisionNumberDesc(productId);
    profile.setRevisionNumber(previous.isEmpty() ? 1 : previous.getFirst().getRevisionNumber() + 1);
    profile.setChainId(request.chainId());
    profile.setCommercialPlanId(request.commercialPlanId());
    profile.setCommercialPlanVersion(version.getVersionNumber());
    profile.setProductType(
        product.getProductTypeDefinition() == null
            ? product.getProductType()
            : product.getProductTypeDefinition().getCode());
    profile.setProductFormat(product.getProductFormat());
    profile.setContractJson(write(request.contract()));
    profile.setCompositionJson(write(composition.values()));
    profile.setCreatedBy(request.createdBy().trim());
    profile.setCreatedAt(Instant.now());
    return view(profiles.saveAndFlush(profile));
  }

  /** Vincula uma única revisão antes do trabalho e valida produto, experimento, cadeia e ciclo. */
  @Transactional
  public ProfileView bind(Long productId, Long profileId, BindProfileRequest request) {
    validate(request);
    products.findLockedById(productId).orElseThrow(() -> notFound("Produto"));
    var profile = required(productId, profileId);
    String reference = request.sourceReference().trim();
    var existing = bindings.findByProductIdAndSourceReference(productId, reference);
    if (existing.isPresent()) {
      if (!existing.get().getProfileId().equals(profileId)
          || !Objects.equals(existing.get().getLearningCycleId(), request.learningCycleId()))
        throw conflict(
            "Esta execução já tem ficha e contexto congelados. O histórico não pode ser reescrito.");
      return view(profile);
    }
    validateReference(profile, request, reference);
    if (!tasks.findBySourceReferenceOrderByCreatedAtAscIdAsc(reference).isEmpty()
        || runs.existsByProductIdAndSourceReference(productId, reference))
      throw conflict(
          "A referência já possui execução. Adote a ficha em uma nova execução antes de iniciar atividades.");
    var binding = new ExecutionProfileBinding();
    binding.setProductId(productId);
    binding.setProfileId(profileId);
    binding.setSourceReference(reference);
    binding.setLearningCycleId(request.learningCycleId());
    binding.setCreatedBy(request.actor().trim());
    binding.setCreatedAt(Instant.now());
    omissions.record(profile, bindings.saveAndFlush(binding));
    return view(profile);
  }

  /**
   * Solicita uma análise de Plutus por revisão e reaproveita seu resultado nos quatro checkpoints.
   */
  @Transactional
  public ProfileView requestAnalysis(Long productId, Long profileId) {
    products.findLockedById(productId).orElseThrow(() -> notFound("Produto"));
    var profile = required(productId, profileId);
    var prior =
        reviews.findByProfileIdOrderByIdAsc(profileId).stream()
            .filter(r -> "ANALYSIS".equals(r.getCheckpoint()))
            .reduce((a, b) -> b);
    if (prior.isPresent()) {
      var execution =
          financialExecutions.findById(prior.get().getFinancialExecutionId()).orElseThrow();
      if (execution.getStatus() != FinancialAgentExecutionStatus.FAILED) return view(profile);
      throw conflict(
          "A análise falhou. Corrija a causa e use uma nova revisão; não repetir uma chamada paga com a mesma entrada.");
    }
    if (planVersions
        .findTopByPlanIdOrderByVersionNumberDesc(profile.getCommercialPlanId())
        .map(v -> v.getVersionNumber() != profile.getCommercialPlanVersion())
        .orElse(true))
      throw conflict("O plano mudou. Atualize a ficha antes de solicitar uma nova análise paga.");
    var contract = context.contract(profile);
    var brief = new LinkedHashMap<String, Object>();
    brief.put("executionProfileId", profileId);
    brief.put("revision", profile.getRevisionNumber());
    brief.put("productId", productId);
    brief.put("productVersion", contract.productVersion());
    brief.put("capability", contract.capability());
    brief.put("includedUnits", contract.includedUnits());
    brief.put("maximumAttempts", contract.maximumAttempts());
    brief.put("costModel", contract.costModel());
    brief.put("pricingRevision", contract.pricingRevision());
    brief.put("economics", ExecutionProfileRules.economics(contract));
    brief.put("costComponents", contract.scenarios());
    brief.put("usdBrl", contract.usdBrl());
    brief.put("maximumAttemptCostBrl", contract.maximumAttemptCostBrl());
    brief.put("purchasedOutcome", contract.purchasedOutcome());
    brief.put("maximumDeliveryCostBrl", contract.maximumDeliveryCostBrl());
    brief.put("productionBudget", contract.productionBudget());
    brief.put("minimumContributionBrl", contract.minimumContributionBrl());
    brief.put("checkpoints", List.of("OFFER", "DELIVERY_DESIGN", "HOMOLOGATION", "OPERATION"));
    var result =
        plutus.startRevenueProjection(
            profile.getCommercialPlanId(), new StartRevenueProjectionRequest(write(brief)));
    var audit = new ExecutionProfileReview();
    audit.setProfileId(profileId);
    audit.setCheckpoint("ANALYSIS");
    audit.setFinancialExecutionId(result.id());
    audit.setApproved(false);
    audit.setReviewedBy("Solicitação administrativa");
    audit.setRationale(
        "Análise da revisão completa. Reutilizar o parecer sem novas chamadas por imagem.");
    audit.setCreatedAt(Instant.now());
    reviews.saveAndFlush(audit);
    return view(profile);
  }

  /** Registra decisão humana somente com parecer completo, contextual e economicamente coerente. */
  @Transactional
  public ProfileView review(Long productId, Long profileId, ReviewProfileRequest request) {
    validate(request);
    products.findLockedById(productId).orElseThrow(() -> notFound("Produto"));
    var profile = required(productId, profileId);
    if (request.approved()
        && planVersions
            .findTopByPlanIdOrderByVersionNumberDesc(profile.getCommercialPlanId())
            .map(v -> v.getVersionNumber() != profile.getCommercialPlanVersion())
            .orElse(true))
      throw conflict("O plano mudou. A decisão exige ficha e parecer da versão atual.");
    var execution =
        financialExecutions
            .findById(request.financialExecutionId())
            .orElseThrow(() -> notFound("Parecer"));
    boolean linked =
        reviews.findByProfileIdOrderByIdAsc(profileId).stream()
            .anyMatch(
                r ->
                    "ANALYSIS".equals(r.getCheckpoint())
                        && r.getFinancialExecutionId().equals(execution.getId()));
    if (!linked
        || execution.getStatus() != FinancialAgentExecutionStatus.COMPLETED
        || !profile.getCommercialPlanId().equals(execution.getCommercialPlan().getId())
        || !Objects.equals(
            profile.getCommercialPlanVersion(), execution.getCommercialPlanVersion()))
      throw conflict("O parecer não está concluído para esta ficha e versão do plano.");
    if (request.approved()) {
      var issues = ExecutionProfileRules.blockers(context.contract(profile));
      if (!issues.isEmpty()) throw conflict(String.join(" ", issues));
      validatePlutusScenarios(profile, execution.getReconciliationJson());
    }
    var review = new ExecutionProfileReview();
    review.setProfileId(profileId);
    review.setCheckpoint(request.checkpoint().name());
    review.setFinancialExecutionId(execution.getId());
    review.setApproved(request.approved());
    review.setReviewedBy(request.reviewedBy().trim());
    review.setRationale(request.rationale().trim());
    review.setCreatedAt(Instant.now());
    reviews.saveAndFlush(review);
    return view(profile);
  }

  /** Concilia custo pendente com comprovante identificado, conservando a evidência anterior. */
  @Transactional
  public ProfileView reconcile(
      Long productId,
      Long profileId,
      com.marketinghub.product.executionprofile.v1.service.reconcile.ReconcileConsumptionRequest
          request) {
    validate(request);
    products.findLockedById(productId).orElseThrow(() -> notFound("Produto"));
    var profile = required(productId, profileId);
    var entry =
        consumption.findById(request.consumptionId()).orElseThrow(() -> notFound("Consumo"));
    var binding = bindings.findById(entry.getBindingId()).orElseThrow(() -> notFound("Vínculo"));
    if (!binding.getProductId().equals(productId) || !binding.getProfileId().equals(profileId))
      throw conflict("O consumo não pertence a esta ficha.");
    if (!"COST_PENDING".equals(entry.getStatus()))
      throw conflict("Somente custo pendente pode ser conciliado; aguarde o retorno da tentativa.");
    if (!request.providerReceipt().matches("(?:https://|internal://)[^\\s]+"))
      throw conflict(
          "Informe o link do comprovante oficial do provedor; estimativa não comprova custo realizado.");
    var evidence = new LinkedHashMap<String, Object>();
    evidence.put("evidenceType", "HUMAN_RECONCILED_PROVIDER_RECEIPT_V1");
    evidence.put("priorEvidence", entry.getEvidence());
    evidence.put("providerReceipt", request.providerReceipt());
    evidence.put("reviewedBy", request.reviewedBy());
    evidence.put("rationale", request.rationale());
    evidence.put("recordedAt", Instant.now());
    budget.settle(productId, entry.getId(), request.actualBrl(), write(evidence), request.failed());
    return view(profile);
  }

  /** Monta o relatório persistido sem transformar projeções em vendas ou objetivos concluídos. */
  private ProfileView view(ExecutionProfile profile) {
    var c = context.contract(profile);
    var bound = bindings.findByProfileIdOrderByIdAsc(profile.getId());
    var decisions =
        reviews.findByProfileIdOrderByIdAsc(profile.getId()).stream()
            .map(
                r ->
                    new Review(
                        r.getCheckpoint(),
                        r.getFinancialExecutionId(),
                        financialExecutions
                            .findById(r.getFinancialExecutionId())
                            .map(e -> e.getStatus().name())
                            .orElse("MISSING"),
                        r.isApproved(),
                        "ANALYSIS".equals(r.getCheckpoint())
                            ? "Análise solicitada"
                            : r.isApproved() ? "Aprovado" : "Reprovado",
                        r.getReviewedBy(),
                        r.getRationale(),
                        r.getCreatedAt()))
            .toList();
    var costs =
        bound.stream()
            .flatMap(b -> consumption.findByBindingIdOrderByIdAsc(b.getId()).stream())
            .map(
                e ->
                    new Consumption(
                        e.getId(),
                        e.getBindingId(),
                        e.getOperationKey(),
                        e.getUsageKey(),
                        e.getUnits(),
                        e.getReservedBrl(),
                        e.getActualBrl(),
                        e.getStatus(),
                        e.getEvidence(),
                        e.isTestData(),
                        e.getCreatedAt()))
            .toList();
    var blockers = new ArrayList<>(ExecutionProfileRules.blockers(c));
    for (var checkpoint : ReviewProfileRequest.Checkpoint.values()) {
      String blocker = context.financialBlocker(profile, checkpoint.name());
      if (blocker != null && !blockers.contains(blocker)) blockers.add(blocker);
    }
    return new ProfileView(
        profile.getId(),
        profile.getProductId(),
        profile.getRevisionNumber(),
        profile.getChainId(),
        profile.getCommercialPlanId(),
        profile.getCommercialPlanVersion(),
        profile.getProductType(),
        profile.getProductFormat(),
        c,
        ExecutionProfileRules.routeName(c),
        ExecutionProfileRules.work(c),
        context.composition(profile),
        ExecutionProfileRules.economics(c),
        blockers,
        decisions,
        bound.stream()
            .map(
                b ->
                    new Binding(
                        b.getId(),
                        b.getSourceReference(),
                        b.getLearningCycleId(),
                        b.getCreatedBy()))
            .toList(),
        costs,
        profile.getCreatedAt(),
        profile.getCreatedBy());
  }

  /** Exige evidência financeira estruturada e contribuição por pacote acima do mínimo. */
  private void validatePlutusScenarios(ExecutionProfile profile, String report) {
    try {
      var rows = json.readTree(report).path("scenarios");
      if (rows.size() != 3) throw conflict("Plutus não apresentou os três cenários completos.");
      var contract = context.contract(profile);
      Set<String> names = new HashSet<>();
      for (var row : rows) {
        String code = row.path("name").asText();
        if (!Set.of("CONSERVATIVE", "BASE", "OPTIMISTIC").contains(code)
            || !names.add(code)
            || !row.path("averagePriceBrl").isNumber()
            || !row.path("contributionMarginPercent").isNumber())
          throw conflict("Plutus não comprovou preço e margem nos três cenários.");
        String expected = "OPTIMISTIC".equals(code) ? "FAVORABLE" : code;
        var scenario =
            contract.scenarios().stream()
                .filter(s -> s.code().name().equals(expected))
                .findFirst()
                .orElseThrow();
        var price = row.path("averagePriceBrl").decimalValue();
        var margin = row.path("contributionMarginPercent").decimalValue();
        if (price.compareTo(scenario.priceBrl()) != 0
            || margin.signum() <= 0
            || margin.compareTo(java.math.BigDecimal.valueOf(100)) > 0
            || price.multiply(margin).movePointLeft(2).compareTo(contract.minimumContributionBrl())
                < 0)
          throw conflict(
              "O parecer de Plutus diverge do preço ou não comprova a contribuição mínima do pacote.");
      }
    } catch (ResponseStatusException ex) {
      log.warn("Parecer financeiro bloqueou ficha. profileId={}", profile.getId(), ex);
      throw ex;
    } catch (Exception ex) {
      log.error("Parecer financeiro inválido. profileId={}", profile.getId(), ex);
      throw conflict("O parecer financeiro não possui evidência estruturada válida.");
    }
  }

  /** Confere a propriedade da referência e o contexto opcional do ciclo, sem inferir por nome. */
  private void validateReference(ExecutionProfile profile, BindProfileRequest request, String ref) {
    if (ref.matches("experiment:[0-9]{1,18}")) {
      var experiment =
          experiments
              .findById(Long.valueOf(ref.substring(11)))
              .orElseThrow(() -> notFound("Experimento"));
      if (experiment.getProduct() == null
          || !profile.getProductId().equals(experiment.getProduct().getId()))
        throw conflict("O experimento não pertence ao produto.");
    } else if (!ref.matches("product:" + profile.getProductId() + "@[A-Za-z0-9._-]+")
        && !ref.matches(
            "commercial-plan:"
                + profile.getCommercialPlanId()
                + "@v"
                + profile.getCommercialPlanVersion()
                + "(?:[:A-Za-z0-9._-]*)")) {
      throw conflict("Use uma referência canônica deste produto, experimento ou plano.");
    }
    if (request.learningCycleId() != null) {
      var cycle = cycles.findById(request.learningCycleId()).orElseThrow(() -> notFound("Ciclo"));
      if (!profile.getProductId().equals(cycle.getProductId())
          || !profile.getChainId().equals(cycle.getChainDefinitionId())
          || !ref.equals("experiment:" + cycle.getExperimentId())
          || !"OPEN".equals(cycle.getStatus())
          || !context.contract(profile).productVersion().equals(cycle.getProductVersion()))
        throw conflict(
            "Produto, versão, cadeia, ciclo e experimento devem corresponder exatamente.");
    }
  }

  /** Percorre chamadas explícitas e congela a versão publicada de cada subprocesso alcançável. */
  private void collect(
      BusinessProcessDefinition process,
      Map<String, ProcessReference> result,
      Set<String> visiting) {
    if (!"PUBLISHED".equals(process.getStatus()))
      throw conflict("A cadeia contém uma definição que não está publicada.");
    if (!visiting.add(process.getProcessCode()))
      throw conflict("A composição contém ciclo de subprocessos.");
    if (result.containsKey(process.getProcessCode())) {
      visiting.remove(process.getProcessCode());
      return;
    }
    result.put(
        process.getProcessCode(),
        new ProcessReference(
            process.getId(),
            process.getProcessCode(),
            process.getVersionNumber(),
            process.getName(),
            process.getParentProcessCode()));
    try {
      for (var node : json.readTree(process.getDiagramJson()).path("nodes")) {
        String childCode = node.path("subprocessCode").asText("");
        if (!childCode.isBlank())
          collect(
              processes
                  .findFirstByProcessCodeAndStatusOrderByVersionNumberDesc(childCode, "PUBLISHED")
                  .orElseThrow(() -> conflict("Subprocesso sem versão publicada: " + childCode)),
              result,
              visiting);
      }
    } catch (ResponseStatusException ex) {
      log.warn("Composição bloqueou ficha. processId={}", process.getId(), ex);
      throw ex;
    } catch (Exception ex) {
      log.error("Composição inválida da ficha. processId={}", process.getId(), ex);
      throw conflict("BPM sem composição válida.");
    }
    visiting.remove(process.getProcessCode());
  }

  /** Rejeita entrada inválida também em chamadas internas do serviço. */
  private void validate(Object request) {
    if (request == null || !validator.validate(request).isEmpty())
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Preencha todos os campos e custos obrigatórios da ficha.");
  }

  /** Resolve a ficha no escopo do produto. */
  private ExecutionProfile required(Long productId, Long id) {
    return profiles.findByIdAndProductId(id, productId).orElseThrow(() -> notFound("Ficha"));
  }

  /** Confere existência do produto sem adquirir lock na consulta. */
  private void requireProduct(Long productId) {
    if (!products.existsById(productId)) throw notFound("Produto");
  }

  /** Serializa snapshots com diagnóstico contextual em caso de falha. */
  private String write(Object value) {
    try {
      return json.writeValueAsString(value);
    } catch (Exception ex) {
      log.error("Falha ao serializar ficha de execução.", ex);
      throw new IllegalStateException("Snapshot inválido.", ex);
    }
  }

  /** Produz o erro funcional de conflito de contrato. */
  private ResponseStatusException conflict(String reason) {
    return new ResponseStatusException(HttpStatus.CONFLICT, reason);
  }

  /** Produz o erro de recurso ausente sem vazar dados de outro produto. */
  private ResponseStatusException notFound(String resource) {
    return new ResponseStatusException(HttpStatus.NOT_FOUND, resource + " não encontrado.");
  }
}
