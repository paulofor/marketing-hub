package com.marketinghub.businessprocess.automation.v1.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocess.automation.v1.ProcessRun;
import com.marketinghub.businessprocess.automation.v1.service.status.ProcessRunRelationResponse;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessDefinitionRepository;
import com.marketinghub.repository.jpa.businessprocesschain.BusinessProcessChainDefinitionRepository;
import com.marketinghub.repository.jpa.processautomation.ProcessRunRepository;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Responsabilidade: resolver links oficiais de chamadas sem perder o contexto da execução. */
@Component
@RequiredArgsConstructor
@Slf4j
public class ProcessRunNavigation {
  private final BusinessProcessDefinitionRepository processes;
  private final BusinessProcessChainDefinitionRepository chains;
  private final ProcessRunRepository runs;
  private final ObjectMapper json;

  /** Preserva a versão do pai gravado e, sem delegação, enumera chamadas da cadeia selecionada. */
  public List<ProcessRunRelationResponse> parents(ProcessRun run) {
    var selected = processes.findById(run.getProcessDefinitionId()).orElseThrow();
    if (run.getParentRunId() != null) {
      var parent = runs.findById(run.getParentRunId()).orElseThrow();
      requireSameContext(run, parent);
      return callsTo(
          run, processes.findById(parent.getProcessDefinitionId()).orElseThrow(), selected);
    }
    Map<Long, BusinessProcessDefinition> members = new LinkedHashMap<>();
    chains
        .findById(run.getChainDefinitionId())
        .orElseThrow()
        .getItems()
        .forEach(item -> collect(item.getProcessDefinition(), members));
    return members.values().stream()
        .flatMap(parent -> callsTo(run, parent, selected).stream())
        .toList();
  }

  /** Enumera cada atividade delegadora, inclusive após conclusão ou pausa do produto. */
  public List<ProcessRunRelationResponse> children(ProcessRun run) {
    var selected = processes.findById(run.getProcessDefinitionId()).orElseThrow();
    List<ProcessRunRelationResponse> result = new ArrayList<>();
    for (var node : nodes(selected)) {
      var child = recordedChild(run, node).or(() -> child(node));
      if (child.isEmpty()) continue;
      var target = child.get();
      result.add(relation(run, target, node, null));
    }
    return List.copyOf(result);
  }

  /** Preserva o filho já delegado quando uma publicação posterior alterar a versão do catálogo. */
  private Optional<BusinessProcessDefinition> recordedChild(ProcessRun run, JsonNode node) {
    if (run.getId() == null
        || !"TASK".equals(node.path("type").asText())
        || node.path("subprocessCode").asText().isBlank()) return Optional.empty();
    return runs.findAllByParentRunId(run.getId()).stream()
        .sorted(Comparator.comparing(ProcessRun::getId).reversed())
        .map(
            childRun -> {
              requireSameContext(run, childRun);
              return processes.findById(childRun.getProcessDefinitionId()).orElseThrow();
            })
        .filter(
            definition -> definition.getProcessCode().equals(node.path("subprocessCode").asText()))
        .findFirst();
  }

  /** Retorna o link de uma atividade somente dentro da identidade congelada do processo. */
  public String activityUrl(ProcessRun run, String activityId) {
    return url(run, run.getProcessDefinitionId(), activityId);
  }

  /** Percorre composições aninhadas, impedindo ciclos e duplicação na coleta do catálogo. */
  private void collect(
      BusinessProcessDefinition process, Map<Long, BusinessProcessDefinition> members) {
    if (members.putIfAbsent(process.getId(), process) != null) return;
    for (var node : nodes(process)) child(node).ifPresent(value -> collect(value, members));
  }

  /** Reconhece a relação pela chamada explícita do BPM e não apenas pelo campo de parentesco. */
  private List<ProcessRunRelationResponse> callsTo(
      ProcessRun run, BusinessProcessDefinition parent, BusinessProcessDefinition selected) {
    List<ProcessRunRelationResponse> result = new ArrayList<>();
    for (var node : nodes(parent)) {
      if ("TASK".equals(node.path("type").asText())
          && selected.getProcessCode().equals(node.path("subprocessCode").asText()))
        result.add(relation(run, parent, node, node.path("id").asText()));
    }
    return List.copyOf(result);
  }

  /** Converte a chamada em uma referência enxuta, sem expor auditoria ou segredos. */
  private ProcessRunRelationResponse relation(
      ProcessRun run, BusinessProcessDefinition process, JsonNode node, String anchor) {
    return new ProcessRunRelationResponse(
        process.getId(),
        process.getName(),
        process.getVersionNumber(),
        node.path("id").asText(),
        node.path("label").asText(node.path("id").asText()),
        url(run, process.getId(), anchor));
  }

  /** Resolve somente subprocessos explicitamente chamados e publicados. */
  private Optional<BusinessProcessDefinition> child(JsonNode node) {
    String code = node.path("subprocessCode").asText();
    return !"TASK".equals(node.path("type").asText()) || code.isBlank()
        ? Optional.empty()
        : processes.findFirstByProcessCodeAndStatusOrderByVersionNumberDesc(code, "PUBLISHED");
  }

  /** Lê a estrutura versionada e mantém falhas diagnosticáveis com o processo de origem. */
  private JsonNode nodes(BusinessProcessDefinition process) {
    try {
      return json.readTree(process.getDiagramJson()).path("nodes");
    } catch (Exception ex) {
      log.error(
          "Falha ao resolver navegação do processo. processDefinitionId={}", process.getId(), ex);
      throw new IllegalStateException("Composição do processo inválida.", ex);
    }
  }

  /** Impede que vínculo persistido incorreto exponha a navegação de outro produto ou ciclo. */
  private void requireSameContext(ProcessRun run, ProcessRun parent) {
    if (!Objects.equals(run.getProductId(), parent.getProductId())
        || !Objects.equals(run.getChainDefinitionId(), parent.getChainDefinitionId())
        || !Objects.equals(run.getLearningCycleId(), parent.getLearningCycleId())
        || !Objects.equals(run.getSourceReference(), parent.getSourceReference()))
      throw new IllegalStateException("Processo pai pertence a outro contexto de execução.");
  }

  /**
   * Monta caminho local com produto, cadeia, ciclo e atividade, sem aceitar redirecionamento
   * externo.
   */
  private String url(ProcessRun run, Long processId, String activityId) {
    return "/products/"
        + run.getProductId()
        + "/value-chain-history/processes/"
        + processId
        + "/activities?chainId="
        + run.getChainDefinitionId()
        + (run.getLearningCycleId() == null ? "" : "&learningCycleId=" + run.getLearningCycleId())
        + (activityId == null || activityId.isBlank()
            ? ""
            : "#activity-"
                + URLEncoder.encode(activityId, StandardCharsets.UTF_8).replace("+", "%20"));
  }
}
