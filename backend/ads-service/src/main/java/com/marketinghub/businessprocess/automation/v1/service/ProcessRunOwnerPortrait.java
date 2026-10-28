package com.marketinghub.businessprocess.automation.v1.service;

import com.marketinghub.repository.jpa.agent.AgentRepository;
import org.springframework.stereotype.Component;

/** Responsabilidade: resolver o retrato do responsável sem alterar a execução ou seu estado. */
@Component
public class ProcessRunOwnerPortrait {
  private final AgentRepository agents;

  /** Reutiliza o cadastro canônico de retratos dos agentes. */
  public ProcessRunOwnerPortrait(AgentRepository agents) {
    this.agents = agents;
  }

  /**
   * Retorna a imagem do apelido exato; responsáveis humanos ou sem retrato permanecem sem imagem.
   */
  public String resolve(String ownerName) {
    if (ownerName == null || ownerName.isBlank()) return null;
    return agents.findPortraitUrlByNickname(ownerName).filter(url -> !url.isBlank()).orElse(null);
  }
}
