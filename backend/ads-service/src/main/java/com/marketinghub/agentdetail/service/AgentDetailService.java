package com.marketinghub.agentdetail.service;

import com.marketinghub.agent.Agent;
import com.marketinghub.agent.AgentInput;
import com.marketinghub.agent.AgentInternalFunction;
import com.marketinghub.agent.AgentOutput;
import com.marketinghub.agentdetail.service.getDetail.AgentDetailItemResponse;
import com.marketinghub.agentdetail.service.getDetail.AgentDetailResourceResponse;
import com.marketinghub.agentdetail.service.getDetail.AgentDetailResponse;
import com.marketinghub.businessprocessresource.BusinessProcessExecutionResource;
import com.marketinghub.repository.jpa.agent.AgentRepository;
import com.marketinghub.repository.jpa.agent.AgentVersionRepository;
import com.marketinghub.repository.jpa.businessprocessresource.BusinessProcessExecutionResourceRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: consolidar a configuração atual e os recursos específicos de um agente. */
@Service
public class AgentDetailService {
  private final AgentRepository agentRepository;
  private final AgentVersionRepository agentVersionRepository;
  private final BusinessProcessExecutionResourceRepository executionResourceRepository;
  private final AgentHarnessCatalog harnessCatalog;

  @org.springframework.beans.factory.annotation.Autowired(required = false)
  private com.marketinghub.catalogovivo.v1.service.CatalogoVivoService catalogoVivo;

  /** Configura as fontes de verdade do cadastro, versão, recursos e harness do agente. */
  public AgentDetailService(
      AgentRepository agentRepository,
      AgentVersionRepository agentVersionRepository,
      BusinessProcessExecutionResourceRepository executionResourceRepository,
      AgentHarnessCatalog harnessCatalog) {
    this.agentRepository = agentRepository;
    this.agentVersionRepository = agentVersionRepository;
    this.executionResourceRepository = executionResourceRepository;
    this.harnessCatalog = harnessCatalog;
  }

  /** Recupera o contrato atual completo do agente sem inferências no frontend. */
  @Transactional(readOnly = true)
  public AgentDetailResponse getDetail(Long agentId) {
    Agent agent =
        agentRepository
            .findDetailedById(agentId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Agente não encontrado."));
    Instant lastContractChangeAt = currentContractChangeAt(agentId);
    List<AgentDetailResourceResponse> resources = executionResources(agent);

    return new AgentDetailResponse(
        agent.getId(),
        agent.getName(),
        agent.getNickname(),
        agent.getPortraitAsset() == null ? null : agent.getPortraitAsset().getId(),
        agent.getPortraitAsset() == null ? null : agent.getPortraitAsset().getUrl(),
        agent.getAgentKey(),
        agent.getStatus(),
        agent.getCurrentVersion(),
        agent.getTheme() == null ? null : agent.getTheme().getId(),
        agent.getTheme() == null ? null : agent.getTheme().getName(),
        agent.getOwnerName(),
        agent.getDescription(),
        agent.getBusinessObjective(),
        agent.getSuccessMetrics(),
        agent.getModelName(),
        agent.getExecutionMode(),
        agent.getAutomaticExecutionEnabled(),
        agent.getAutomaticExecutionChangedAt(),
        agent.getAutomaticExecutionChangedBy(),
        agent.getTriggerPolicy(),
        agent.getResponsibilityContract(),
        agent.getOrchestratorPolicy(),
        agent.getAnalysisPolicy(),
        agent.getOfferingPolicy(),
        agent.getAuthorityPolicy(),
        agent.getPromptContractPath(),
        agent.getSchemaContractPath(),
        mapInputs(agent.getInputs()),
        mapOutputs(agent.getOutputs()),
        mapInternalFunctions(agent.getInternalFunctions()),
        resources,
        harness(agent.getAgentKey()),
        agent.getCreatedAt(),
        agent.getUpdatedAt(),
        lastContractChangeAt);
  }

  /** Acrescenta textos e hashes do banco ao harness preservando os recursos ainda em arquivo. */
  private com.marketinghub.agentdetail.service.getDetail.AgentHarnessResponse harness(
      String agentKey) {
    var base = harnessCatalog.getByAgentKey(agentKey);
    if (catalogoVivo == null) return base;
    var files = new java.util.ArrayList<>(base.behaviorFiles());
    var artifacts = new java.util.ArrayList<>(base.artifacts());
    for (var item : catalogoVivo.harness(agentKey)) {
      if (!agentKey.equals(item.binding().agentKey())) continue;
      var version =
          item.versions().stream()
              .filter(v -> java.util.Objects.equals(v.id(), item.binding().activeVersionId()))
              .findFirst();
      if (version.isEmpty()) continue;
      var v = version.get();
      String path = "/catalogo-vivo/opala#binding-" + item.binding().id();
      String description =
          "Origem: banco de dados. Texto da atividade "
              + item.binding().activityName()
              + ". Versão fixada por tarefa; revisão e ativação auditáveis.";
      files.add(
          new com.marketinghub.agentdetail.service.getDetail.AgentBehaviorFileResponse(
              "PROMPT",
              item.binding().activityName(),
              "v" + v.versionNumber(),
              path,
              description,
              "text/markdown",
              v.sha256(),
              v.text()));
      artifacts.add(
          new com.marketinghub.agentdetail.service.getDetail.AgentHarnessArtifactResponse(
              "PROMPT", item.binding().activityName(), "v" + v.versionNumber(), path, description));
    }
    return new com.marketinghub.agentdetail.service.getDetail.AgentHarnessResponse(
        base.status(),
        base.contractVersion(),
        base.sourceReference(),
        base.sensitiveValuesPolicy(),
        base.sections(),
        artifacts,
        files);
  }

  /** Recupera a data imutável da versão que governa o contrato atual. */
  private Instant currentContractChangeAt(Long agentId) {
    return agentVersionRepository.findCurrentVersionChanges(List.of(agentId)).stream()
        .filter(change -> agentId.equals(change.getAgentId()))
        .map(AgentVersionRepository.CurrentVersionChange::getChangedAt)
        .findFirst()
        .orElse(null);
  }

  /** Consulta no banco somente os recursos ativos que pertencem ao agente. */
  private List<AgentDetailResourceResponse> executionResources(Agent agent) {
    if (agent.getAgentKey() == null || agent.getAgentKey().isBlank()) {
      return List.of();
    }
    return executionResourceRepository
        .findAllByResponsibleAgentKeyAndActiveTrueOrderByNameAsc(agent.getAgentKey())
        .stream()
        .map(this::mapResource)
        .toList();
  }

  /** Converte uma entrada persistida para o contrato de detalhe. */
  private List<AgentDetailItemResponse> mapInputs(List<AgentInput> inputs) {
    return inputs.stream()
        .map(
            item ->
                new AgentDetailItemResponse(
                    item.getId(),
                    item.getName(),
                    item.getType(),
                    item.getDescription(),
                    item.getOrderIndex()))
        .toList();
  }

  /** Converte uma saída persistida para o contrato de detalhe. */
  private List<AgentDetailItemResponse> mapOutputs(List<AgentOutput> outputs) {
    return outputs.stream()
        .map(
            item ->
                new AgentDetailItemResponse(
                    item.getId(),
                    item.getName(),
                    item.getType(),
                    item.getDescription(),
                    item.getOrderIndex()))
        .toList();
  }

  /** Converte uma função interna persistida para o contrato de detalhe. */
  private List<AgentDetailItemResponse> mapInternalFunctions(
      List<AgentInternalFunction> internalFunctions) {
    return internalFunctions.stream()
        .map(
            item ->
                new AgentDetailItemResponse(
                    item.getId(),
                    item.getName(),
                    item.getType(),
                    item.getDescription(),
                    item.getOrderIndex()))
        .toList();
  }

  /** Converte um recurso executável persistido para o contrato de detalhe. */
  private AgentDetailResourceResponse mapResource(BusinessProcessExecutionResource resource) {
    return new AgentDetailResourceResponse(
        resource.getId(),
        resource.getResourceCode(),
        resource.getName(),
        resource.getDescription(),
        resource.getResourceType(),
        resource.getExecutorReference(),
        resource.getUsageInstructions());
  }
}
