package com.marketinghub.product.bigidea.v1.service;

import com.fasterxml.jackson.databind.*;
import com.marketinghub.experimentstrategist.*;
import com.marketinghub.product.Product;
import com.marketinghub.product.bigidea.v1.service.research.*;
import com.marketinghub.product.bigidea.v1.service.save.BigIdeaContent;
import com.marketinghub.repository.jpa.experimentstrategist.ExperimentStrategistExecutionRepository;
import com.marketinghub.repository.jpa.planning.CommercialPlanRepository;
import jakarta.validation.Validator;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/** Adapta a fila existente de Atena à pesquisa isolada da primeira Big Idea de um produto. */
@Component
@RequiredArgsConstructor
@Slf4j
public class BigIdeaResearch {
  public static final String MODE = "PRODUCT_BIG_IDEA_RESEARCH";
  private final ExperimentStrategistExecutionRepository executions;
  private final CommercialPlanRepository plans;
  private final ObjectMapper json;
  private final Validator validator;

  /** Consulta a pesquisa mais recente sem alterar estratégia ou experimento. */
  public BigIdeaResearchView current(Product product) {
    var latest = executions.findLatestBigIdea(product.getId());
    return view(product, latest.orElse(null));
  }

  /** Reutiliza pedido existente e congela somente o produto solicitado, sob lock do chamador. */
  public BigIdeaResearchView start(Product product, StartBigIdeaResearchRequest request) {
    var existing = executions.findLatestBigIdea(product.getId());
    if (existing.isPresent()) return view(product, existing.get());
    if (product.getBigIdea() != null)
      throw conflict("O produto já possui Big Idea. Evolua a revisão existente com motivo.");
    if (!Boolean.TRUE.equals(product.getAutomaticExecutionEnabled()))
      throw conflict("O produto está em STOP. Preserve a pausa antes de solicitar Atena.");
    var plan =
        plans.findByProductId(product.getId()).stream()
            .findFirst()
            .orElseThrow(
                () -> conflict("Vincule o produto ao seu plano comercial antes da pesquisa."));
    var snapshot = new LinkedHashMap<String, Object>();
    snapshot.put("contractVersion", "PRODUCT_BIG_IDEA_RESEARCH_V1");
    snapshot.put("productId", product.getId());
    snapshot.put("name", product.getName());
    snapshot.put("internalName", product.getInternalName());
    snapshot.put("targetAudience", product.getTargetAudience());
    snapshot.put("hypothesis", product.getPrimaryHypothesis());
    snapshot.put("journey", product.getSevenDayJourney());
    snapshot.put("productType", product.getProductType());
    snapshot.put("priceReferenceBrl", product.getCurrentPriceBrl());
    snapshot.put("commercialStatus", product.getCommercialStatus());
    snapshot.put("languageStyle", product.getLanguageStyle());
    var execution = new ExperimentStrategistExecution();
    execution.setCommercialPlan(plan);
    execution.setAuthorityMode(MODE);
    execution.setStatus(ExperimentStrategistExecutionStatus.PENDING);
    execution.setResearchQuestion(
        request == null || request.brief() == null ? "" : request.brief().trim());
    try {
      execution.setEvidenceSnapshot(json.writeValueAsString(Map.of("bigIdeaResearch", snapshot)));
    } catch (Exception ex) {
      log.error("Falha ao preparar pesquisa Big Idea. productId={}", product.getId(), ex);
      throw new IllegalStateException("Não foi possível preparar o contexto de Atena.", ex);
    }
    executions.saveAndFlush(execution);
    executions.attachCurrentAgentVersion(execution.getId());
    return view(product, execution);
  }

  /**
   * Obtém proposta concluída da identidade exata; não transforma revisão em aprovação comercial.
   */
  public BigIdeaContent proposal(Product product, Long executionId) {
    var execution =
        executions.findById(executionId).orElseThrow(() -> conflict("Pesquisa não encontrada."));
    validateIdentity(execution, product.getId());
    if (execution.getStatus() != ExperimentStrategistExecutionStatus.COMPLETED)
      throw conflict("Atena ainda não concluiu esta pesquisa.");
    return validateResult(execution, execution.getRawModelResponse());
  }

  /**
   * Rejeita resposta cruzada, conteúdo incompleto e demanda inventada antes de registrar sucesso.
   */
  public BigIdeaContent validateResult(ExperimentStrategistExecution execution, String raw) {
    try {
      JsonNode input = json.readTree(execution.getEvidenceSnapshot()).required("bigIdeaResearch");
      JsonNode result = json.readTree(raw);
      if (!result.path("productId").isIntegralNumber()
          || result.path("productId").asLong() != input.required("productId").asLong()
          || !"PRODUCT_BIG_IDEA_RESEARCH_V1".equals(result.path("contractVersion").asText())
          || !"HYPOTHESIS".equals(result.path("demandAssessment").path("status").asText())
          || !result.path("alternatives").isArray()
          || result.path("alternatives").size() != 3
          || !result.path("sources").isArray()
          || result.path("sources").size() < 2
          || result.path("sources").size() > 8)
        throw new IllegalArgumentException(
            "Identidade, alternativas ou limites de demanda inválidos.");
      requireText(result, "selectionReason", 1600);
      for (String field :
          List.of(
              "frequencyAndUrgency",
              "willingnessToPaySignals",
              "evidenceAgainst",
              "gaps",
              "validationPlan")) requireText(result.path("demandAssessment"), field, 1600);
      for (JsonNode option : result.path("alternatives"))
        for (String field : List.of("idea", "benefit", "risk", "effort", "fit"))
          requireText(option, field, 900);
      var classes = new HashSet<String>();
      for (JsonNode source : result.path("sources")) {
        for (String field : List.of("url", "title", "accessedAt", "evidenceClass", "learning"))
          requireText(source, field, 1000);
        if (!source.path("url").asText().matches("https?://[^\\s]+")
            || source.path("evidenceClass").asText().isBlank()
            || source.path("learning").asText().isBlank())
          throw new IllegalArgumentException("Fonte pública incompleta.");
        classes.add(source.path("evidenceClass").asText());
      }
      if (classes.size() < 2)
        throw new IllegalArgumentException("Use duas classes independentes de fontes.");
      for (String field :
          List.of(
              "centralIdea", "audience", "desire", "promise", "mechanism", "evidence", "limits"))
        requireText(result.path("bigIdea"), field, 3000);
      BigIdeaContent content = json.treeToValue(result.required("bigIdea"), BigIdeaContent.class);
      if (content == null || !validator.validate(content).isEmpty())
        throw new IllegalArgumentException("Big Idea incompleta ou acima dos limites.");
      return content;
    } catch (Exception ex) {
      log.error("Resposta Big Idea inválida. executionId={}", execution.getId(), ex);
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Resposta de Atena inválida: " + ex.getMessage(), ex);
    }
  }

  /** Confere produto congelado e modo específico antes de expor ou aplicar uma proposta. */
  private void validateIdentity(ExperimentStrategistExecution execution, Long productId) {
    try {
      if (!MODE.equals(execution.getAuthorityMode())
          || json.readTree(execution.getEvidenceSnapshot())
                  .path("bigIdeaResearch")
                  .path("productId")
                  .asLong()
              != productId) throw conflict("A pesquisa pertence a outro produto ou finalidade.");
    } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
      log.error(
          "Contexto Big Idea inválido. executionId={} productId={}",
          execution.getId(),
          productId,
          ex);
      throw conflict("Contexto da pesquisa inválido.");
    }
  }

  /** Deriva ações autorizadas do estado persistido para a interface. */
  private BigIdeaResearchView view(Product product, ExperimentStrategistExecution execution) {
    boolean missing = product.getBigIdea() == null;
    if (execution == null)
      return new BigIdeaResearchView(
          null,
          product.getId(),
          "NOT_REQUESTED",
          missing && Boolean.TRUE.equals(product.getAutomaticExecutionEnabled()),
          false,
          "Atena compara três ideias e pesquisa sinais de demanda. Não altera campanhas nem orçamentos.",
          null,
          null,
          null);
    validateIdentity(execution, product.getId());
    JsonNode proposal = null;
    if (execution.getRawModelResponse() != null
        && execution.getStatus() == ExperimentStrategistExecutionStatus.COMPLETED) {
      try {
        proposal = json.readTree(execution.getRawModelResponse());
      } catch (Exception ex) {
        log.error("Falha ao ler proposta Big Idea. executionId={}", execution.getId(), ex);
        throw new IllegalStateException("Proposta preservada inválida.", ex);
      }
    }
    String guidance =
        switch (execution.getStatus()) {
          case PENDING, RUNNING ->
              "Atena está preparando a proposta; o backend acompanha a execução.";
          case COMPLETED ->
              missing
                  ? "Pesquisa concluída. Confira e aplique a proposta para registrar a primeira versão."
                  : "A Big Idea já está registrada. A pesquisa e as fontes permanecem disponíveis.";
          case FAILED ->
              "A pesquisa falhou e não será repetida automaticamente. Resultado e consumo disponíveis foram preservados para diagnóstico.";
        };
    return new BigIdeaResearchView(
        execution.getId(),
        product.getId(),
        execution.getStatus().name(),
        false,
        missing && proposal != null,
        guidance,
        proposal,
        execution.getEstimatedCost(),
        execution.getModelName());
  }

  /** Expõe conflito acionável sem produzir mutações ou novas chamadas. */
  private ResponseStatusException conflict(String message) {
    return new ResponseStatusException(HttpStatus.CONFLICT, message);
  }

  /** Exige texto real e limitado, sem coerção de tipos ou campos vazios. */
  private void requireText(JsonNode object, String field, int max) {
    JsonNode value = object.path(field);
    if (!value.isTextual() || value.asText().isBlank() || value.asText().length() > max)
      throw new IllegalArgumentException("Campo da pesquisa incompleto: " + field);
  }
}
