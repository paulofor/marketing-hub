package com.marketinghub.agent.web;

import com.marketinghub.agent.dto.AgentDto;
import com.marketinghub.agent.dto.AgentMaturityDto;
import com.marketinghub.agent.dto.SaveAgentRequest;
import com.marketinghub.agent.mapper.AgentMapper;
import com.marketinghub.agent.service.AgentMaturityService;
import com.marketinghub.agent.service.AgentService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Responsabilidade: expor o cadastro e a governanca dos agentes pela API administrativa. */
@RestController
@RequestMapping("/api/agents")
public class AgentController {

  private final AgentService service;
  private final AgentMapper mapper;
  private final AgentMaturityService maturityService;

  /** Configura o servico e o conversor usados pelos endpoints. */
  public AgentController(
      AgentService service, AgentMapper mapper, AgentMaturityService maturityService) {
    this.service = service;
    this.mapper = mapper;
    this.maturityService = maturityService;
  }

  /** Cria um agente e sua primeira versao de contrato. */
  @PostMapping
  public AgentDto create(@RequestBody SaveAgentRequest request) {
    return mapper.toDto(service.create(request));
  }

  /** Atualiza um agente criando uma nova versao auditavel. */
  @PutMapping("/{id}")
  public AgentDto update(@PathVariable Long id, @RequestBody SaveAgentRequest request) {
    return mapper.toDto(service.update(id, request));
  }

  /** Lista todos os agentes cadastrados. */
  @GetMapping
  public List<AgentDto> list() {
    return service.list().stream().map(mapper::toDto).toList();
  }

  /** Recupera um agente pelo identificador. */
  @GetMapping("/{id}")
  public AgentDto get(@PathVariable Long id) {
    return mapper.toDto(service.get(id));
  }

  /** Consolida maturidade, pendências e resultados comprovados dos agentes. */
  @GetMapping("/maturity")
  public List<AgentMaturityDto> maturity() {
    return maturityService.list();
  }
}
