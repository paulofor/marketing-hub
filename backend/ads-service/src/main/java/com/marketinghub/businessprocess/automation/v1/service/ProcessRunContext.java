package com.marketinghub.businessprocess.automation.v1.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agenttask.AgentTaskFunctionalSnapshot;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocess.automation.v1.ProcessRun;
import com.marketinghub.businessprocess.automation.v1.service.commands.ProcessRunCommand;
import com.marketinghub.businessprocess.execution.service.BusinessProcessActivityExecutionService;
import com.marketinghub.businessprocess.execution.service.productProcessExecutions.ProductProcessActivityExecutionHistoryResponse;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessDefinitionRepository;
import com.marketinghub.repository.jpa.businessprocesschain.BusinessProcessChainDefinitionRepository;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository;
import com.marketinghub.repository.jpa.product.ProductRepository;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: validar identidade e obter os contratos oficiais do contexto congelado. */
@Component
@RequiredArgsConstructor
@Slf4j
public class ProcessRunContext {
  private final BusinessProcessActivityExecutionService activities;
  private final BusinessProcessDefinitionRepository processes;
  private final BusinessProcessChainDefinitionRepository chains;
  private final LearningSalesCycleRepository cycles;
  private final ProductRepository products;
  private final ObjectMapper json;
  private final com.marketinghub.repository.jpa.experiment.ExperimentRepository experiments;
  private final AgentTaskRepository agentTasks;

  @org.springframework.beans.factory.annotation.Autowired(required = false)
  private com.marketinghub.product.executionprofile.v1.service.ExecutionProfileContext
      executionProfileContext;

  @org.springframework.beans.factory.annotation.Autowired(required = false)
  private com.marketinghub.repository.jdbc.catalogovivo.OpalaAdoptionRepository catalogAdoptions;

  @org.springframework.beans.factory.annotation.Autowired(required = false)
  private com.marketinghub.agenttask.CommunicationMaterializationContextProvider
      communicationInputs;

  /** Reconhece a versão publicada ou congelada previamente por esta execução do produto. */
  public boolean executableVersion(
      Long productId, Long processId, ProcessRunCommand command, String status) {
    if ("PUBLISHED".equals(status)) return true;
    return "RETIRED".equals(status)
        && executionProfileContext != null
        && executionProfileContext.bound(productId, command.sourceReference()).isPresent()
        && executionProfileContext.pins(command.sourceReference(), processId);
  }

  /**
   * Valida identidade, ficha, BPM e adesão explícita, preservando a referência congelada em toda
   * leitura; navegação sem referência e homologação encerrada não permitem execução.
   */
  public ProductProcessActivityExecutionHistoryResponse read(
      Long productId, Long processId, ProcessRunCommand command, boolean execution) {
    if (command == null
        || command.chainId() == null
        || (execution
            && (command.sourceReference() == null || command.sourceReference().isBlank())))
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Informe cadeia e referência operacional do processo.");
    var process = process(processId);
    if (executionProfileContext != null && command.sourceReference() != null)
      executionProfileContext.requireScope(
          productId, command.sourceReference(), command.chainId(), command.learningCycleId());
    if (!"PRODUCT".equals(process.getExecutionScope()))
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Este controle exige um processo vinculado a produto.");
    var chain =
        chains
            .findById(command.chainId())
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cadeia não encontrada."));
    Set<Long> memberIds = new HashSet<>();
    chain.getItems().forEach(item -> memberIds.add(item.getProcessDefinition().getId()));
    boolean explicitlyAdopted =
        catalogAdoptions != null
            && command.learningCycleId() != null
            && catalogAdoptions.permits(
                command.learningCycleId(),
                productId,
                command.chainId(),
                processId,
                command.sourceReference());
    if (!explicitlyAdopted && !belongs(process, memberIds, productTypeCode(productId)))
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "A versão do processo não pertence à cadeia informada.");
    if (command.learningCycleId() != null) {
      var cycle =
          cycles
              .findById(command.learningCycleId())
              .orElseThrow(
                  () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ciclo não encontrado."));
      if (!productId.equals(cycle.getProductId())
          || !command.chainId().equals(cycle.getChainDefinitionId()))
        throw new ResponseStatusException(
            HttpStatus.CONFLICT, "Produto, cadeia e ciclo não correspondem.");
      if (execution && !"OPEN".equals(cycle.getStatus()))
        throw new ResponseStatusException(
            HttpStatus.CONFLICT, "O ciclo está encerrado; o histórico foi preservado.");
    }
    var result =
        command.sourceReference() != null && !command.sourceReference().isBlank()
            ? activities.productProcessExecutions(
                processId,
                productId,
                command.learningCycleId(),
                command.chainId(),
                false,
                command.sourceReference())
            : activities.productProcessExecutions(
                processId, productId, command.learningCycleId(), command.chainId(), false);
    if (command.sourceReference() != null
        && !command.sourceReference().isBlank()
        && !Objects.equals(command.sourceReference(), result.currentExecutionReference()))
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "O contexto operacional mudou. Atualize a tela antes de iniciar outro processo.");
    if (execution) {
      String blocker = experimentBlockReason(process, productId, command.sourceReference());
      if (blocker != null) throw new ResponseStatusException(HttpStatus.CONFLICT, blocker);
    }
    graph(processId).ordered(result.activities());
    return result;
  }

  /** Reutiliza o contexto gravado, sem selecionar silenciosamente outro ciclo ou experimento. */
  public ProductProcessActivityExecutionHistoryResponse read(ProcessRun run, boolean execution) {
    return read(run.getProductId(), run.getProcessDefinitionId(), command(run), execution);
  }

  /** Lê resultados funcionais da referência congelada sem carregar prompts ou auditoria. */
  public List<AgentTaskFunctionalSnapshot> functionalTasks(ProcessRun run) {
    return agentTasks.findFunctionalSnapshotsByProcessSince(
        run.getProcessDefinitionId(), run.getSourceReference(), null);
  }

  /**
   * Bloqueia homologação encerrada, ciclo fechado ou versão sem autorização, preservando callbacks
   * existentes e versões retiradas que foram fixadas pela ficha da mesma referência.
   */
  public String dispatchBlockReason(ProcessRun run) {
    if (run.getLearningCycleId() != null) {
      var cycle =
          cycles
              .findById(run.getLearningCycleId())
              .orElseThrow(
                  () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ciclo não encontrado."));
      if (!"OPEN".equals(cycle.getStatus()))
        return "O ciclo está encerrado. Resultados preservados; nenhuma nova atividade será iniciada neste ciclo.";
    }
    var process = process(run.getProcessDefinitionId());
    String experimentBlocker =
        experimentBlockReason(process, run.getProductId(), run.getSourceReference());
    if (experimentBlocker != null) return experimentBlocker;
    if (executableVersion(
        run.getProductId(), run.getProcessDefinitionId(), command(run), process.getStatus()))
      return null;
    return "A versão v"
        + process.getVersionNumber()
        + " do processo #"
        + process.getId()
        + " não está publicada nem fixada por uma ficha desta referência. Resultados preservados; "
        + "inicie a versão autorizada no contexto correto.";
  }

  /** Protege preparações Safira/Quartzo e homologação mesmo quando a navegação omite o ciclo. */
  private String experimentBlockReason(
      BusinessProcessDefinition process, Long productId, String reference) {
    if (!"experiment-homologation-activation".equals(process.getProcessCode())
        && !"pde-commercial-homologation-activation".equals(process.getProcessCode())
        && !"safira-commercial-preparation-v1".equals(process.getProcessCode())
        && !"quartzo-commercial-preparation-v1".equals(process.getProcessCode())) return null;
    if (reference == null || !reference.matches("experiment:[1-9][0-9]*")) return null;
    Long id = Long.valueOf(reference.substring("experiment:".length()));
    var experiment =
        experiments
            .findById(id)
            .filter(
                value -> value.getProduct() != null && productId.equals(value.getProduct().getId()))
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "O experimento não pertence ao produto desta homologação."));
    String reason =
        com.marketinghub.experiment.run.service.ExperimentHomologationLifecycle.blockReason(
            experiment);
    if (reason != null) return reason;
    var cycle = cycles.findByExperimentId(id).orElse(null);
    if (cycle != null && !productId.equals(cycle.getProductId()))
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "O ciclo do experimento não pertence ao produto desta preparação.");
    if (cycle != null && !"OPEN".equals(cycle.getStatus()))
      return "O ciclo #"
          + cycle.getId()
          + " está encerrado. Resultados preservados; nenhuma nova atividade será iniciada neste ciclo.";
    return null;
  }

  /** Reconhece dependências preparatórias da mesma ocorrência sem liberar trabalho em curso. */
  public boolean permitsCommercialContinuation(ProcessRun waiting, ProcessRun candidate) {
    if (permitsInitialPlanning(waiting, candidate)) return true;
    if (waiting.getLearningCycleId() == null
        || !"learningCycle".equals(waiting.getCurrentActivityId())
        || waiting.getFailureCount() > 0
        || !Set.of("WAITING_ACTIVITY", "WAITING_HUMAN", "WAITING_INPUT")
            .contains(waiting.getStatus())
        || !Objects.equals(waiting.getProductId(), candidate.getProductId())
        || !Objects.equals(waiting.getChainDefinitionId(), candidate.getChainDefinitionId())
        || !Objects.equals(waiting.getLearningCycleId(), candidate.getLearningCycleId())
        || !Objects.equals(waiting.getSourceReference(), candidate.getSourceReference()))
      return false;
    var cycle = cycles.findById(waiting.getLearningCycleId()).orElseThrow();
    return "OPEN".equals(cycle.getStatus())
        && Set.of("AUTHORIZATION", "PUBLICATION").contains(cycle.getStage())
        && Objects.equals(cycle.getProductId(), waiting.getProductId())
        && Objects.equals(cycle.getChainDefinitionId(), waiting.getChainDefinitionId())
        && Objects.equals("experiment:" + cycle.getExperimentId(), waiting.getSourceReference())
        && "pde-sales-delivery-learning"
            .equals(process(waiting.getProcessDefinitionId()).getProcessCode())
        && Set.of(
                "pde-commercial-homologation-activation",
                "opala-commercial-preparation-v1",
                "quartzo-commercial-preparation-v1",
                "safira-commercial-preparation-v1")
            .contains(process(candidate.getProcessDefinitionId()).getProcessCode());
  }

  /** Permite preparar o primeiro experimento enquanto sua comunicação aguarda os predecessores. */
  private boolean permitsInitialPlanning(ProcessRun waiting, ProcessRun candidate) {
    if (!"WAITING_INPUT".equals(waiting.getStatus())
        || waiting.getFailureCount() > 0
        || !"communicationContract".equals(waiting.getCurrentActivityId())
        || waiting.getLearningCycleId() != null
        || candidate.getLearningCycleId() != null
        || !Objects.equals(waiting.getProductId(), candidate.getProductId())
        || !Objects.equals(waiting.getChainDefinitionId(), candidate.getChainDefinitionId())
        || !Objects.equals(waiting.getSourceReference(), candidate.getSourceReference())
        || waiting.getSourceReference() == null
        || !waiting.getSourceReference().matches("experiment:[1-9][0-9]{0,17}")) return false;
    if (!"pde-communication-sales-journey"
            .equals(process(waiting.getProcessDefinitionId()).getProcessCode())
        || !"pde-commercial-plan-offer"
            .equals(process(candidate.getProcessDefinitionId()).getProcessCode())) return false;
    Long experimentId = Long.valueOf(waiting.getSourceReference().substring(11));
    var experiment = experiments.findById(experimentId).orElse(null);
    return experiment != null
        && experiment.getProduct() != null
        && Objects.equals(waiting.getProductId(), experiment.getProduct().getId())
        && experiment.getStatus() == com.marketinghub.experiment.ExperimentStatus.PLANNED
        && experiment.getSourceExperiment() == null
        && cycles.findByExperimentId(experimentId).isEmpty();
  }

  /** Reconstrói somente os identificadores imutáveis da solicitação original. */
  public ProcessRunCommand command(ProcessRun run) {
    return new ProcessRunCommand(
        run.getChainDefinitionId(), run.getLearningCycleId(), run.getSourceReference());
  }

  /** Consulta a definição exata; publicação de outra versão não altera a execução aberta. */
  public BusinessProcessDefinition process(Long id) {
    return processes
        .findById(id)
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Processo não encontrado."));
  }

  /**
   * Incorpora versão, definição validada e revisão funcional do contrato criativo à proteção contra
   * repetição sem progresso, preservando o contrato anterior dos demais processos.
   */
  public String inputVersion(ProcessRun run) {
    var product = products.findById(run.getProductId()).orElseThrow();
    String version =
        run.getLearningCycleId() == null
            ? ""
            : cycles.findById(run.getLearningCycleId()).orElseThrow().getProductVersion();
    String base = version + "|" + product.getValidationDefinitionJson();
    return communicationInputs == null
        ? base
        : communicationInputs
            .executionInputVersion(
                process(run.getProcessDefinitionId()).getProcessCode(), run.getSourceReference())
            .map(revision -> base + "|" + revision)
            .orElse(base);
  }

  /** Lê o grafo para que a decisão respeite as dependências reais da versão selecionada. */
  public ProcessExecutionGraph graph(Long processId) {
    try {
      return new ProcessExecutionGraph(json.readTree(process(processId).getDiagramJson()));
    } catch (Exception ex) {
      log.error("Falha no BPM da execução automática. processDefinitionId={}", processId, ex);
      throw new IllegalStateException("O BPM não possui um contrato de sequência válido.", ex);
    }
  }

  /** Confere vínculo por chamada explícita, sem aceitar apenas parentesco por nome. */
  private boolean belongs(
      BusinessProcessDefinition process, Set<Long> members, String productTypeCode) {
    if (members.contains(process.getId())) return true;
    for (Long memberId : members)
      if (calls(
          processes.findById(memberId).orElseThrow(), process, productTypeCode, new HashSet<>()))
        return true;
    return false;
  }

  /** Percorre o grafo real da cadeia e respeita versões exatas declaradas nas rotas por tipo. */
  private boolean calls(
      BusinessProcessDefinition parent,
      BusinessProcessDefinition target,
      String productTypeCode,
      Set<Long> visited) {
    if (!visited.add(parent.getId())) return false;
    try {
      for (var node : json.readTree(parent.getDiagramJson()).path("nodes")) {
        if (!"TASK".equals(node.path("type").asText())) continue;
        String directCode = node.path("subprocessCode").asText();
        if (!directCode.isBlank()) {
          if (directCode.equals(target.getProcessCode())) return true;
          for (var child : processes.findAllByProcessCodeOrderByVersionNumberDesc(directCode))
            if (calls(child, target, productTypeCode, visited)) return true;
        }
        for (JsonNode route : node.path("subprocessRoutes")) {
          if (!Objects.equals(productTypeCode, route.path("productTypeCode").asText())) continue;
          String code = route.path("subprocessCode").asText();
          int version = route.path("subprocessVersion").asInt(-1);
          if (code.isBlank() || version < 1)
            throw new IllegalStateException("A rota do tipo não possui processo e versão exatos.");
          if (code.equals(target.getProcessCode()) && version == target.getVersionNumber())
            return true;
          var child = processes.findByProcessCodeAndVersionNumber(code, version).orElse(null);
          if (child != null && calls(child, target, productTypeCode, visited)) return true;
        }
      }
      return false;
    } catch (Exception ex) {
      log.error(
          "Falha ao validar chamada do subprocesso. processDefinitionId={} parentId={}",
          target.getId(),
          parent.getId(),
          ex);
      throw new IllegalStateException("A chamada do subprocesso possui contrato inválido.", ex);
    }
  }

  /** Obtém o tipo cadastrado usado para impedir que outra família consuma a rota Opala. */
  private String productTypeCode(Long productId) {
    return products
        .findById(productId)
        .map(product -> product.getProductTypeDefinition())
        .map(type -> type.getCode())
        .orElse(null);
  }
}
