package com.marketinghub.experimentstrategist.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.experimentstrategist.ExperimentStrategistExecution;
import com.marketinghub.planning.dto.CommercialPlanVersionDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: comprovar a compatibilidade da proposta com a versão comercial persistida. */
public final class CommercialAssumptionVersionCompatibility {
  private static final Logger log =
      LoggerFactory.getLogger(CommercialAssumptionVersionCompatibility.class);
  private static final ObjectMapper json = new ObjectMapper();

  /** Impede instâncias do comparador sem estado. */
  private CommercialAssumptionVersionCompatibility() {}

  /** Prioriza a versão explícita; histórico antigo exige data posterior à versão vigente. */
  public static boolean matches(
      ExperimentStrategistExecution source, CommercialPlanVersionDto current) {
    if (source.getEvidenceSnapshot() == null || source.getEvidenceSnapshot().isBlank()) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Snapshot da proposta ausente.");
    }
    try {
      var snapshot = json.readTree(source.getEvidenceSnapshot());
      if (snapshot == null || !snapshot.isObject()) {
        throw new ResponseStatusException(HttpStatus.CONFLICT, "Snapshot da proposta inválido.");
      }
      var planId = snapshot.at("/commercialPlan/id");
      if (!planId.isMissingNode()
          && !planId.isNull()
          && (!planId.isIntegralNumber()
              || !planId.canConvertToLong()
              || planId.longValue() != current.commercialPlanId())) return false;
      var version = snapshot.at("/commercialPlan/version");
      if (version != null && !version.isMissingNode() && !version.isNull()) {
        if (!version.isIntegralNumber() || !version.canConvertToInt() || version.intValue() < 1)
          throw new ResponseStatusException(HttpStatus.CONFLICT, "Versão da proposta inválida.");
        return current.versionNumber() != null && version.intValue() == current.versionNumber();
      }
      return source.getCreatedAt() != null
          && current.createdAt() != null
          && !source.getCreatedAt().isBefore(current.createdAt());
    } catch (JsonProcessingException ex) {
      log.error(
          "Falha ao verificar versão da proposta; strategistExecutionId={} planId={}",
          source.getId(),
          current.commercialPlanId(),
          ex);
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "Snapshot da proposta inválido; preservar a resposta e corrigir a fonte.",
          ex);
    }
  }
}
