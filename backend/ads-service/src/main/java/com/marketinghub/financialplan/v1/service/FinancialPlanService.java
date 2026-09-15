package com.marketinghub.financialplan.v1.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.financialagent.service.FinancialAgentService;
import com.marketinghub.financialagent.service.StartRevenueProjectionRequest;
import com.marketinghub.financialplan.v1.FinancialPlanRevision;
import com.marketinghub.financialplan.v1.FinancialPlanRevision.Environment;
import com.marketinghub.financialplan.v1.service.catalog.PlanCatalog;
import com.marketinghub.financialplan.v1.service.getplan.*;
import com.marketinghub.financialplan.v1.service.saveplan.*;
import com.marketinghub.repository.jpa.financialagent.FinancialAgentExecutionRepository;
import com.marketinghub.repository.jpa.financialplan.FinancialPlanRevisionRepository;
import com.marketinghub.repository.jpa.planning.CommercialPlanRepository;
import com.marketinghub.repository.jpa.planning.CommercialPlanVersionRepository;
import com.marketinghub.repository.jpa.product.ProductRepository;
import com.marketinghub.repository.jpa.producttype.ProductTypeDefinitionRepository;
import jakarta.validation.Validator;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Responsabilidade: governar revisões de planos/modelos financeiros e solicitar parecer contextual.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FinancialPlanService {
  private final FinancialPlanRevisionRepository revisions;
  private final ProductRepository products;
  private final ProductTypeDefinitionRepository types;
  private final CommercialPlanRepository plans;
  private final CommercialPlanVersionRepository versions;
  private final FinancialAgentExecutionRepository executions;
  private final FinancialAgentService plutus;
  private final ObjectMapper json;
  private final Validator validator;

  /** Entrega referências canônicas para cadastro, incluindo o tipo real de cada produto. */
  @Transactional(readOnly = true)
  public PlanCatalog catalog(Long productId) {
    if (productId != null) requireOwner("PRODUCT", productId, false);
    return new PlanCatalog(
        products.findAll().stream()
            .map(
                p ->
                    new PlanCatalog.ProductOption(
                        p.getId(),
                        p.getInternalName() == null ? p.getName() : p.getInternalName(),
                        p.getProductTypeDefinition() == null
                            ? null
                            : p.getProductTypeDefinition().getId()))
            .toList(),
        types.findAllByOrderByNameAsc().stream()
            .map(t -> new PlanCatalog.Option(t.getId(), t.getInternalName() + " · " + t.getName()))
            .toList(),
        productId == null
            ? List.of()
            : plans.findByProductId(productId).stream()
                .map(p -> new PlanCatalog.Option(p.getId(), p.getName()))
                .toList());
  }

  /** Lista apenas o histórico do produto ou tipo e ambiente escolhidos. */
  @Transactional(readOnly = true)
  public List<PlanView> list(String scope, Long scopeId, Environment environment) {
    requireOwner(scope, scopeId, false);
    return revisions
        .findByScopeKindAndScopeIdAndEnvironmentOrderByRevisionNumberDesc(
            scope, scopeId, environment)
        .stream()
        .map(this::view)
        .toList();
  }

  /** Retorna revisão exata, sem substituir referências explícitas pela revisão atual. */
  @Transactional(readOnly = true)
  public PlanView get(String scope, Long scopeId, Environment environment, Long id) {
    return view(required(scope, scopeId, environment, id, false));
  }

  /** Salva revisão imutável com versão comercial congelada e controle de concorrência. */
  @Transactional
  public PlanView create(
      String scope, Long scopeId, Environment environment, SavePlanRequest request) {
    var violations = validator.validate(request);
    if (!violations.isEmpty())
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Revise os campos do plano: " + violations.iterator().next().getPropertyPath());
    Long typeId = requireOwner(scope, scopeId, true);
    var history =
        revisions.findByScopeKindAndScopeIdAndEnvironmentOrderByRevisionNumberDesc(
            scope, scopeId, environment);
    int current = history.isEmpty() ? 0 : history.getFirst().getRevisionNumber();
    if (current != request.expectedRevision())
      throw conflict("Outra revisão foi salva. Recarregue o histórico antes de gravar.");
    var p = new FinancialPlanRevision();
    p.setScopeKind(scope);
    p.setScopeId(scopeId);
    p.setProductId("PRODUCT".equals(scope) ? scopeId : null);
    p.setProductTypeId(typeId);
    p.setEnvironment(environment);
    p.setRevisionNumber(current + 1);
    p.setName(request.name().trim());
    p.setCreatedBy(request.createdBy().trim());
    p.setCreatedAt(Instant.now());
    if ("PRODUCT".equals(scope)) {
      if (request.assumptions().productVersion() == null
          || request.assumptions().productVersion().isBlank())
        throw conflict("Informe a versão específica do produto.");
      if (request.commercialPlanId() == null
          || !plans.findIdsByProductId(scopeId).contains(request.commercialPlanId()))
        throw conflict("Selecione um plano comercial deste produto.");
      p.setCommercialPlanId(request.commercialPlanId());
      p.setCommercialPlanVersion(currentVersion(request.commercialPlanId()));
      if (request.templateId() != null) {
        var template =
            revisions.findById(request.templateId()).orElseThrow(() -> missing("Modelo"));
        if (!"TYPE".equals(template.getScopeKind())
            || !Objects.equals(template.getScopeId(), typeId)
            || template.getEnvironment() != environment)
          throw conflict("O modelo precisa pertencer ao tipo e ambiente deste produto.");
        if (read(template.getAssumptionsJson(), PlanAssumptions.class, template.getId())
            .validUntil()
            .isBefore(today()))
          throw conflict("O modelo venceu. Revise suas premissas antes de adotá-lo.");
        p.setTemplateId(template.getId());
      }
    } else if (request.commercialPlanId() != null || request.templateId() != null) {
      throw conflict("Modelo por tipo não recebe plano comercial nem parecer de outro produto.");
    }
    if (request.assumptions().ai().pricingCheckedOn() != null
        && request.assumptions().ai().pricingCheckedOn().isAfter(today()))
      throw conflict("A conferência da tarifa não pode ter data futura.");
    p.setAssumptionsJson(write(request.assumptions(), scope, scopeId));
    for (var prior : history) {
      if (Objects.equals(prior.getCommercialPlanId(), p.getCommercialPlanId())
          && Objects.equals(prior.getCommercialPlanVersion(), p.getCommercialPlanVersion())
          && Objects.equals(prior.getTemplateId(), p.getTemplateId())
          && Objects.equals(prior.getProductTypeId(), p.getProductTypeId())
          && prior.getAssumptionsJson().equals(p.getAssumptionsJson())) return view(prior);
    }
    p.setEvaluationJson(
        write(FinancialPlanCalculator.evaluate(request.assumptions()), scope, scopeId));
    var saved = revisions.saveAndFlush(p);
    log.info(
        "Plano financeiro salvo scope={} scopeId={} revision={} environment={} financialPlanId={}",
        scope,
        scopeId,
        p.getRevisionNumber(),
        environment,
        p.getId());
    return view(saved);
  }

  /** Enfileira no máximo um parecer por revisão e reutiliza a mesma execução após retentativa. */
  @Transactional
  public PlanView requestAnalysis(Long productId, Environment environment, Long id) {
    var p = required("PRODUCT", productId, environment, id, true);
    if (p.getFinancialExecutionId() != null) return view(p);
    var current = view(p);
    if (!current.canRequestAnalysis())
      throw conflict(
          "Resolva as pendências antes de solicitar Plutus: "
              + String.join(" ", current.pendingActions()));
    var context = new LinkedHashMap<String, Object>();
    context.put("financialPlanId", p.getId());
    context.put("financialPlanRevision", p.getRevisionNumber());
    context.put("productId", productId);
    context.put("commercialPlanVersion", p.getCommercialPlanVersion());
    context.put("environment", environment.name());
    context.put("assumptions", current.assumptions());
    context.put("deterministicEvaluation", current.evaluation());
    var request = new StartRevenueProjectionRequest(write(context, "PRODUCT", productId));
    if (!validator.validate(request).isEmpty())
      throw conflict(
          "O contexto excede o contrato de Plutus; reduza as fontes sem omitir custos essenciais.");
    var result = plutus.startRevenueProjection(p.getCommercialPlanId(), request);
    if (!Objects.equals(result.commercialPlanVersion(), p.getCommercialPlanVersion()))
      throw conflict(
          "O plano comercial mudou durante a solicitação; atualize a revisão financeira.");
    p.setFinancialExecutionId(result.id());
    revisions.saveAndFlush(p);
    log.info(
        "Parecer financeiro enfileirado productId={} financialPlanId={} revision={} executionId={}",
        productId,
        id,
        p.getRevisionNumber(),
        result.id());
    return view(p);
  }

  /** Apresenta projeção persistida e status atual, sem aprovar gastos ou misturar realizado. */
  private PlanView view(FinancialPlanRevision p) {
    var assumptions = read(p.getAssumptionsJson(), PlanAssumptions.class, p.getId());
    var evaluation = read(p.getEvaluationJson(), PlanEvaluation.class, p.getId());
    List<String> pending = new ArrayList<>();
    boolean stale = assumptions.validUntil().isBefore(today());
    if (stale) pending.add("Plutus: validade encerrada; crie uma revisão com fontes conferidas.");
    if (p.getCommercialPlanId() != null
        && !Objects.equals(p.getCommercialPlanVersion(), currentVersion(p.getCommercialPlanId()))) {
      stale = true;
      pending.add("Atena / Plutus: o plano comercial mudou; revise as premissas e os limites.");
    }
    if ("PRODUCT".equals(p.getScopeKind())) {
      var product = products.findById(p.getScopeId()).orElseThrow(() -> missing("Produto"));
      Long currentType =
          product.getProductTypeDefinition() == null
              ? null
              : product.getProductTypeDefinition().getId();
      if (!Objects.equals(p.getProductTypeId(), currentType)) {
        stale = true;
        pending.add("Responsável pelo produto: o tipo mudou; revise a aplicabilidade do modelo.");
      }
    }
    if ("MISSING_INPUTS".equals(evaluation.status())) pending.addAll(evaluation.blockers());
    if (p.getEnvironment() == Environment.TEST)
      pending.add("Homologação: parecer pago indisponível para dados TEST.");
    if ("TYPE".equals(p.getScopeKind()))
      pending.add("Adote este modelo em um produto e versão antes de solicitar parecer.");
    PlanView.Analysis analysis = null;
    if (p.getFinancialExecutionId() != null) {
      var e =
          executions
              .findById(p.getFinancialExecutionId())
              .orElseThrow(() -> missing("Parecer financeiro"));
      analysis =
          new PlanView.Analysis(
              e.getId(),
              e.getStatus().name(),
              e.getDailyReport(),
              e.getReconciliationJson() == null
                  ? null
                  : read(
                      e.getReconciliationJson(),
                      com.fasterxml.jackson.databind.JsonNode.class,
                      p.getId()),
              e.getErrorMessage(),
              e.getModel(),
              e.getEstimatedCost(),
              e.getEstimatedCost() == null ? "NOT_REPORTED" : "REPORTED",
              e.getFinishedAt());
    }
    return new PlanView(
        p.getId(),
        p.getScopeKind(),
        p.getScopeId(),
        p.getEnvironment(),
        p.getName(),
        p.getRevisionNumber(),
        p.getTemplateId(),
        p.getCommercialPlanId(),
        p.getCommercialPlanVersion(),
        p.getCreatedBy(),
        p.getCreatedAt(),
        assumptions,
        evaluation,
        stale,
        List.copyOf(pending),
        p.getFinancialExecutionId() == null && pending.isEmpty(),
        analysis);
  }

  /** Confirma propriedade e ambiente antes de ler ou bloquear uma revisão. */
  private FinancialPlanRevision required(
      String scope, Long owner, Environment environment, Long id, boolean lock) {
    var p =
        (lock ? revisions.findLockedById(id) : revisions.findById(id))
            .orElseThrow(() -> missing("Plano financeiro"));
    if (!scope.equals(p.getScopeKind())
        || !owner.equals(p.getScopeId())
        || environment != p.getEnvironment()) throw missing("Plano financeiro neste escopo");
    return p;
  }

  /** Confirma proprietário real e serializa revisões no registro de produto ou tipo. */
  private Long requireOwner(String scope, Long id, boolean lock) {
    if ("PRODUCT".equals(scope)) {
      var p =
          (lock ? products.findLockedById(id) : products.findById(id))
              .orElseThrow(() -> missing("Produto"));
      return p.getProductTypeDefinition() == null ? null : p.getProductTypeDefinition().getId();
    }
    if ("TYPE".equals(scope)) {
      var type =
          (lock ? types.findLockedById(id) : types.findById(id)).orElseThrow(() -> missing("Tipo"));
      return type.getId();
    }
    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Escopo inválido.");
  }

  /** Recupera a versão comercial persistida sem criar ou inferir uma versão inexistente. */
  private Integer currentVersion(Long planId) {
    return versions
        .findTopByPlanIdOrderByVersionNumberDesc(planId)
        .orElseThrow(() -> conflict("O plano comercial precisa ter uma versão registrada."))
        .getVersionNumber();
  }

  /** Usa a mesma referência UTC do backend para validade das premissas. */
  private static LocalDate today() {
    return LocalDate.now(ZoneOffset.UTC);
  }

  /** Serializa contratos de persistência/integração com identificação do proprietário em falhas. */
  private String write(Object value, String scope, Long scopeId) {
    try {
      return json.writeValueAsString(value);
    } catch (JsonProcessingException ex) {
      log.error(
          "Plano financeiro: falha ao serializar contrato type={} scope={} scopeId={}",
          value.getClass().getSimpleName(),
          scope,
          scopeId,
          ex);
      throw new IllegalStateException("Não foi possível registrar o plano financeiro.", ex);
    }
  }

  /** Lê contrato persistido preservando a revisão e a falha completa para diagnóstico. */
  private <T> T read(String value, Class<T> type, Long financialPlanId) {
    try {
      return json.readValue(value, type);
    } catch (JsonProcessingException ex) {
      log.error(
          "Plano financeiro: falha ao ler contrato type={} financialPlanId={}",
          type.getSimpleName(),
          financialPlanId,
          ex);
      throw new IllegalStateException("Não foi possível ler o plano financeiro.", ex);
    }
  }

  /** Converte referência ausente em resposta explícita de consulta. */
  private static ResponseStatusException missing(String label) {
    return new ResponseStatusException(HttpStatus.NOT_FOUND, label + " não encontrado.");
  }

  /** Explica impedimento de negócio sem alterar decisões humanas ou histórico. */
  private static ResponseStatusException conflict(String message) {
    return new ResponseStatusException(HttpStatus.CONFLICT, message);
  }
}
