package com.marketinghub.agentmemory.service;

import com.marketinghub.agentmemory.PremiumAgentMemory;
import com.marketinghub.agentmemory.service.dashboard.AgentLearningDashboardResponse;
import com.marketinghub.agentmemory.service.dashboard.AgentLearningMemoryResponse;
import com.marketinghub.agentmemory.service.dashboard.AgentLearningSummaryResponse;
import com.marketinghub.repository.jpa.agentmemory.PremiumAgentMemoryRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Responsabilidade: consolidar evidências de aprendizado para supervisão administrativa. */
@Service
public class AgentLearningDashboardService {
  private static final Map<String, String> AGENT_NAMES =
      Map.of(
          "customer-agent", "Psique",
          "financial-agent", "Plutus",
          "growth-operator", "Operador de Crescimento",
          "experiment-strategist", "Estrategista de Experimentos",
          "communication-director", "Íris",
          "landing-generator", "Dédalo",
          "meta-ad-approver", "Têmis",
          "apollo", "Apolo");
  private final PremiumAgentMemoryRepository repository;

  /** Inicializa a consolidação usando a fonte canônica de memórias. */
  public AgentLearningDashboardService(PremiumAgentMemoryRepository repository) {
    this.repository = repository;
  }

  /** Monta o painel sem inferir impacto comercial onde não existe atribuição persistida. */
  @Transactional(readOnly = true)
  public AgentLearningDashboardResponse dashboard() {
    List<PremiumAgentMemory> memories = repository.findAllByOrderByUpdatedAtDesc();
    Map<String, List<PremiumAgentMemory>> byAgent = new LinkedHashMap<>();
    AGENT_NAMES.keySet().stream().sorted().forEach(key -> byAgent.put(key, List.of()));
    memories.stream()
        .collect(java.util.stream.Collectors.groupingBy(PremiumAgentMemory::getAgentKey))
        .forEach(byAgent::put);
    List<AgentLearningSummaryResponse> agents =
        byAgent.entrySet().stream()
            .map(entry -> summary(entry.getKey(), entry.getValue()))
            .toList();
    return new AgentLearningDashboardResponse(
        memories.size(),
        count(memories, "CANDIDATE"),
        count(memories, "CONFIRMED"),
        count(memories, "CONTRADICTED"),
        count(memories, "RETIRED"),
        memories.stream().mapToLong(PremiumAgentMemory::getRetrievalCount).sum(),
        agents,
        memories.stream().map(this::memory).toList());
  }

  /** Resume estados e reutilizações de um agente. */
  private AgentLearningSummaryResponse summary(String agentKey, List<PremiumAgentMemory> values) {
    return new AgentLearningSummaryResponse(
        agentKey,
        name(agentKey),
        values.size(),
        count(values, "CANDIDATE"),
        count(values, "CONFIRMED"),
        count(values, "CONTRADICTED"),
        count(values, "RETIRED"),
        values.stream().mapToLong(PremiumAgentMemory::getRetrievalCount).sum());
  }

  /** Conta memórias pelo estado canônico. */
  private long count(List<PremiumAgentMemory> values, String status) {
    return values.stream().filter(value -> status.equals(value.getStatus())).count();
  }

  /** Converte uma memória preservando segregação, origem e evidência. */
  private AgentLearningMemoryResponse memory(PremiumAgentMemory value) {
    return new AgentLearningMemoryResponse(
        value.getId(),
        value.getAgentKey(),
        name(value.getAgentKey()),
        value.getTenantKey(),
        value.getScopeType(),
        value.getScopeId(),
        value.getSpecialty(),
        value.getContent(),
        value.getEvidence(),
        value.getSourceReference(),
        value.getSourceExecutionId(),
        value.getStatus(),
        value.getConfidence(),
        value.getRetrievalCount(),
        value.getLastRetrievedAt(),
        value.getValidUntil(),
        value.getCreatedAt(),
        value.getUpdatedAt());
  }

  /** Resolve o nome humano sem ocultar agentes futuros ainda não catalogados. */
  private String name(String agentKey) {
    return AGENT_NAMES.getOrDefault(agentKey, agentKey);
  }
}
