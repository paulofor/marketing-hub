package com.marketinghub.businessprocess.automation.v1.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocess.automation.v1.ProcessRun;
import com.marketinghub.businessprocess.automation.v1.service.commands.ProcessRunCommand;
import com.marketinghub.businessprocess.execution.service.BusinessProcessActivityExecutionService;
import com.marketinghub.businessprocess.execution.service.productProcessExecutions.ProductProcessActivityExecutionHistoryResponse;
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

  /** Valida identidade e BPM; permite consulta da navegação sem referência, mas nunca execução. */
  public ProductProcessActivityExecutionHistoryResponse read(
      Long productId, Long processId, ProcessRunCommand command, boolean execution) {
    if (command == null
        || command.chainId() == null
        || (execution
            && (command.sourceReference() == null || command.sourceReference().isBlank())))
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Informe cadeia e referência operacional do processo.");
    var process = process(processId);
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
    if (!belongs(process, memberIds, new HashSet<>()))
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
        activities.productProcessExecutions(
            processId, productId, command.learningCycleId(), command.chainId(), false);
    if (command.sourceReference() != null
        && !command.sourceReference().isBlank()
        && !Objects.equals(command.sourceReference(), result.currentExecutionReference()))
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "O contexto operacional mudou. Atualize a tela antes de iniciar outro processo.");
    graph(processId).ordered(result.activities());
    return result;
  }

  /** Reutiliza o contexto gravado, sem selecionar silenciosamente outro ciclo ou experimento. */
  public ProductProcessActivityExecutionHistoryResponse read(ProcessRun run, boolean execution) {
    return read(run.getProductId(), run.getProcessDefinitionId(), command(run), execution);
  }

  /** Bloqueia novos trabalhos em ciclo encerrado, sem impedir a leitura de resultados recebidos. */
  public String dispatchBlockReason(ProcessRun run) {
    if (run.getLearningCycleId() == null) return null;
    var cycle =
        cycles
            .findById(run.getLearningCycleId())
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ciclo não encontrado."));
    return "OPEN".equals(cycle.getStatus())
        ? null
        : "O ciclo está encerrado. Resultados preservados; nenhuma nova atividade será iniciada neste ciclo.";
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
   * Incorpora a versão do produto e a entrada validada à proteção contra repetição sem progresso.
   */
  public String inputVersion(ProcessRun run) {
    var product = products.findById(run.getProductId()).orElseThrow();
    String version =
        run.getLearningCycleId() == null
            ? ""
            : cycles.findById(run.getLearningCycleId()).orElseThrow().getProductVersion();
    return version + "|" + product.getValidationDefinitionJson();
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
  private boolean belongs(BusinessProcessDefinition process, Set<Long> members, Set<Long> visited) {
    if (members.contains(process.getId())) return true;
    if (process.getParentProcessCode() == null || !visited.add(process.getId())) return false;
    for (var parent :
        processes.findAllByProcessCodeOrderByVersionNumberDesc(process.getParentProcessCode())) {
      try {
        boolean called = false;
        for (var node : json.readTree(parent.getDiagramJson()).path("nodes"))
          if ("TASK".equals(node.path("type").asText())
              && process.getProcessCode().equals(node.path("subprocessCode").asText()))
            called = true;
        if (called && belongs(parent, members, visited)) return true;
      } catch (Exception ex) {
        log.error(
            "Falha ao validar chamada do subprocesso. processDefinitionId={} parentId={}",
            process.getId(),
            parent.getId(),
            ex);
        throw new IllegalStateException("A chamada do subprocesso possui contrato inválido.", ex);
      }
    }
    return false;
  }
}
