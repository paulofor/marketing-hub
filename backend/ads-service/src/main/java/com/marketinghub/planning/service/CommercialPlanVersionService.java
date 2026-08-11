package com.marketinghub.planning.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.planning.CommercialPlan;
import com.marketinghub.planning.CommercialPlanVersion;
import com.marketinghub.planning.dto.CommercialPlanVersionDto;
import com.marketinghub.repository.jpa.planning.CommercialPlanVersionRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Responsabilidade: congelar e expor o contexto comercial usado nas decisões dos agentes. */
@Service
public class CommercialPlanVersionService {
  private final CommercialPlanVersionRepository repository;
  private final ObjectMapper objectMapper;
  private final Clock clock;

  /** Configura a persistência e a serialização determinística dos snapshots. */
  @Autowired
  public CommercialPlanVersionService(
      CommercialPlanVersionRepository repository, ObjectMapper objectMapper) {
    this(repository, objectMapper, Clock.systemUTC());
  }

  /** Permite testes determinísticos do instante de criação das versões. */
  CommercialPlanVersionService(
      CommercialPlanVersionRepository repository, ObjectMapper objectMapper, Clock clock) {
    this.repository = repository;
    this.objectMapper = objectMapper;
    this.clock = clock;
  }

  /** Cria uma nova versão imutável com os campos que orientam vendas, lucro e gates. */
  @Transactional
  public CommercialPlanVersionDto snapshot(CommercialPlan plan, String changedBy, String reason) {
    int next =
        repository
                .findTopByPlanIdOrderByVersionNumberDesc(plan.getId())
                .map(CommercialPlanVersion::getVersionNumber)
                .orElse(0)
            + 1;
    CommercialPlanVersion version = new CommercialPlanVersion();
    version.setPlan(plan);
    version.setVersionNumber(next);
    version.setSnapshotJson(serialize(plan));
    version.setChangedBy(changedBy);
    version.setChangeReason(reason);
    version.setCreatedAt(Instant.now(clock));
    return toDto(repository.save(version));
  }

  /** Lista todo o histórico de contexto de um plano sem recalcular versões anteriores. */
  @Transactional(readOnly = true)
  public List<CommercialPlanVersionDto> list(Long planId) {
    return repository.findByPlanIdOrderByVersionNumberDesc(planId).stream()
        .map(this::toDto)
        .toList();
  }

  /** Expõe a versão corrente para agentes anexarem às tarefas, gates e execuções. */
  @Transactional(readOnly = true)
  public CommercialPlanVersionDto current(Long planId) {
    return repository
        .findTopByPlanIdOrderByVersionNumberDesc(planId)
        .map(this::toDto)
        .orElseThrow(() -> new IllegalStateException("Plano comercial ainda não possui versão."));
  }

  /** Serializa somente o contexto funcional, sem entidades técnicas ou metadados internos. */
  private String serialize(CommercialPlan plan) {
    Map<String, Object> context = new LinkedHashMap<>();
    context.put("planId", plan.getId());
    context.put("name", plan.getName());
    context.put("status", plan.getStatus());
    context.put("commercialObjective", plan.getCommercialObjective());
    context.put("targetAudience", plan.getTargetAudience());
    context.put("mainPain", plan.getMainPain());
    context.put("mainOffer", plan.getMainOffer());
    context.put("mainLeadMagnet", plan.getMainLeadMagnet());
    context.put("mainChannel", plan.getMainChannel());
    context.put("mainMetric", plan.getMainMetric());
    context.put("successCriteria", plan.getSuccessCriteria());
    context.put("stopCriteria", plan.getStopCriteria());
    context.put("deadline", plan.getDeadline());
    context.put("maxBudgetBrl", plan.getMaxBudget());
    context.put("targetRevenueBrl", plan.getTargetRevenue());
    context.put("offerPriceBrl", plan.getOfferPriceBrl());
    context.put("variableCostPerSaleBrl", plan.getVariableCostPerSaleBrl());
    context.put("expectedMonthlyTraffic", plan.getExpectedMonthlyTraffic());
    context.put("expectedConversionRatePercent", plan.getExpectedConversionRatePercent());
    context.put("expectedCacBrl", plan.getExpectedCacBrl());
    context.put("expectedRefundRatePercent", plan.getExpectedRefundRatePercent());
    context.put("fixedOperationalCostBrl", plan.getFixedOperationalCostBrl());
    context.put("actualTotalCostBrl", plan.getActualTotalCost());
    context.put("actualRevenueBrl", plan.getActualRevenue());
    context.put("currentBlocker", plan.getCurrentBlocker());
    context.put("nextAction", plan.getNextAction());
    try {
      return objectMapper.writeValueAsString(context);
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException("Não foi possível versionar o contexto comercial.", ex);
    }
  }

  /** Converte a entidade persistida no contrato público. */
  private CommercialPlanVersionDto toDto(CommercialPlanVersion version) {
    return new CommercialPlanVersionDto(
        version.getId(),
        version.getPlan().getId(),
        version.getVersionNumber(),
        version.getSnapshotJson(),
        version.getChangedBy(),
        version.getChangeReason(),
        version.getCreatedAt());
  }
}
