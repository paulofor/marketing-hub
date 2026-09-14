package com.marketinghub.product.executionprofile.v1.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agenttask.BusinessProcessActivityInstance;
import com.marketinghub.product.executionprofile.v1.*;
import com.marketinghub.repository.jpa.agenttask.BusinessProcessActivityInstanceRepository;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessActivityDefinitionRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: registrar a única dispensa permitida pela ficha sem concluir seu objetivo. */
@Component
@RequiredArgsConstructor
@Slf4j
public class ExecutionProfileOmissions {
  private final ExecutionProfileContext context;
  private final BusinessProcessActivityDefinitionRepository definitions;
  private final BusinessProcessActivityInstanceRepository instances;
  private final ObjectMapper json;

  /**
   * Materializa a dispensa de audiovisual na construção para UI e consumidores compartilharem
   * prova.
   */
  public void record(ExecutionProfile profile, ExecutionProfileBinding binding) {
    if (context.contract(profile).audiovisualRequired()) return;
    var process =
        context.composition(profile).stream()
            .filter(p -> p.code().equals("pde-construction-approval"))
            .findFirst()
            .orElseThrow();
    var definition =
        definitions.findByProcessDefinitionIdAndActivityId(process.id(), "audiovisual");
    if (definition.isEmpty()) return;
    try {
      var node = json.readTree(definition.get().getDefinitionJson());
      if (!"Apolo".equals(definition.get().getOwnerName())
          || node.path("responsibleAgentKeys").size() != 1
          || !"videomaker".equals(node.path("responsibleAgentKeys").path(0).asText()))
        throw new ResponseStatusException(
            HttpStatus.CONFLICT,
            "A atividade audiovisual não pode ser dispensada pela ficha: preserve a responsabilidade e os gates publicados.");
      if (instances
          .findTopByActivityDefinitionIdAndSourceReferenceOrderByOccurrenceNumberDesc(
              definition.get().getId(), binding.getSourceReference())
          .isPresent())
        throw new ResponseStatusException(
            HttpStatus.CONFLICT,
            "A atividade já possui histórico e não pode receber dispensa retroativa.");
      Instant now = Instant.now();
      var instance = new BusinessProcessActivityInstance();
      instance.setActivityDefinition(definition.get());
      instance.setSourceReference(binding.getSourceReference());
      instance.setOccurrenceNumber(1);
      instance.setStatus("NOT_APPLICABLE");
      instance.setObjectiveAchieved(false);
      instance.setEnteredAt(now);
      instance.setExitedAt(now);
      instance.setCreatedAt(now);
      instance.setUpdatedAt(now);
      instance.setKnownCostUsd(BigDecimal.ZERO);
      instance.setCostCoverage("COMPLETE");
      instance.setEvidenceQuality("DIRECT");
      instance.setObjectiveEvidenceJson(
          json.writeValueAsString(
              Map.of(
                  "evidenceType",
                  "EXECUTION_PROFILE_OPTIONAL_ACTIVITY_V1",
                  "profileId",
                  profile.getId(),
                  "profileRevision",
                  profile.getRevisionNumber(),
                  "bindingId",
                  binding.getId(),
                  "activityId",
                  "audiovisual",
                  "sourceReference",
                  binding.getSourceReference(),
                  "audiovisualRequired",
                  false,
                  "reason",
                  "A entrega contratada não inclui audiovisual.")));
      instances.saveAndFlush(instance);
    } catch (RuntimeException ex) {
      log.error("Falha ao registrar dispensa da ficha. profileId={}", profile.getId(), ex);
      throw ex;
    } catch (Exception ex) {
      log.error("Contrato inválido da dispensa. profileId={}", profile.getId(), ex);
      throw new IllegalStateException(ex);
    }
  }
}
