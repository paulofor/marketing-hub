package com.marketinghub.agent.web;

import com.marketinghub.agent.Agent;
import com.marketinghub.agent.dto.AgentDto;
import com.marketinghub.agent.dto.AgentMaturityDto;
import com.marketinghub.agent.dto.SaveAgentRequest;
import com.marketinghub.agent.integration.AgentWorkflowFreshness;
import com.marketinghub.agent.integration.AgentWorkflowFreshnessService;
import com.marketinghub.agent.mapper.AgentMapper;
import com.marketinghub.agent.service.AgentMaturityService;
import com.marketinghub.agent.service.AgentService;
import com.marketinghub.agent.service.uploadportrait.AgentPortraitUploadResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Responsabilidade: expor o cadastro e a governanca dos agentes pela API administrativa. */
@RestController
@RequestMapping("/api/agents")
public class AgentController {

  private final AgentService service;
  private final AgentMapper mapper;
  private final AgentMaturityService maturityService;
  private final AgentWorkflowFreshnessService workflowFreshnessService;

  /** Configura o servico e o conversor usados pelos endpoints. */
  public AgentController(
      AgentService service,
      AgentMapper mapper,
      AgentMaturityService maturityService,
      AgentWorkflowFreshnessService workflowFreshnessService) {
    this.service = service;
    this.mapper = mapper;
    this.maturityService = maturityService;
    this.workflowFreshnessService = workflowFreshnessService;
  }

  /** Cria um agente e sua primeira versao de contrato. */
  @PostMapping
  public AgentDto create(@RequestBody SaveAgentRequest request) {
    return toDtoWithContractChange(service.create(request));
  }

  /** Atualiza um agente criando uma nova versao auditavel. */
  @PutMapping("/{id}")
  public AgentDto update(@PathVariable Long id, @RequestBody SaveAgentRequest request) {
    return toDtoWithContractChange(service.update(id, request));
  }

  /** Lista todos os agentes cadastrados. */
  @GetMapping
  public List<AgentDto> list() {
    List<Agent> agents = service.list();
    Map<Long, Instant> changes = service.currentVersionChanges(agents);
    Map<Long, AgentWorkflowFreshness> workflows =
        workflowFreshnessService.currentWorkflowRuns(agents);
    return agents.stream()
        .map(agent -> mapper.toDto(agent, changes.get(agent.getId()), workflows.get(agent.getId())))
        .toList();
  }

  /** Recupera um agente pelo identificador. */
  @GetMapping("/{id}")
  public AgentDto get(@PathVariable Long id) {
    return toDtoWithContractChange(service.get(id));
  }

  /** Combina o cadastro com a data imutavel da versao contratual atual. */
  private AgentDto toDtoWithContractChange(Agent agent) {
    Instant changedAt = service.currentVersionChanges(List.of(agent)).get(agent.getId());
    return mapper.toDto(agent, changedAt);
  }

  /** Recebe a imagem mitológica usada para identificar visualmente um agente. */
  @PostMapping(value = "/portrait", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public AgentPortraitUploadResponse uploadPortrait(@RequestParam("file") MultipartFile file)
      throws IOException {
    return service.uploadPortrait(file);
  }

  /** Consolida maturidade, pendências e resultados comprovados dos agentes. */
  @GetMapping("/maturity")
  public List<AgentMaturityDto> maturity() {
    return maturityService.list();
  }
}
