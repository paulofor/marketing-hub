package com.marketinghub.agent.service;

import com.marketinghub.agent.Agent;
import com.marketinghub.agent.dto.AgentMaturityDto;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/** Responsabilidade: consolidar resultados auditáveis dos agentes sem duplicar suas filas. */
@Service
public class AgentMaturityService {
  private final AgentService agentService;
  private final JdbcTemplate jdbc;

  /** Configura as fontes canônicas usadas na consolidação. */
  public AgentMaturityService(AgentService agentService, JdbcTemplate jdbc) {
    this.agentService = agentService;
    this.jdbc = jdbc;
  }

  /** Consolida maturidade dos agentes atualmente governados pelo painel. */
  public List<AgentMaturityDto> list() {
    return agentService.list().stream().map(this::summarize).toList();
  }

  /** Resolve a fonte operacional pelo contrato canônico do agente. */
  private AgentMaturityDto summarize(Agent agent) {
    return switch (String.valueOf(agent.getAgentKey())) {
      case "growth-operator" -> growth(agent);
      case "financial-agent" -> financial(agent);
      case "customer-agent" -> customer(agent);
      default -> empty(agent);
    };
  }

  /** Consolida execuções e pendências comprováveis do Operador. */
  private AgentMaturityDto growth(Agent agent) {
    Map<String, Object> execution = aggregate("growth_operator_execution");
    Map<String, Object> tasks =
        jdbc.queryForMap(
            "SELECT COUNT(*) total, SUM(status = 'OPEN') open_count, SUM(status = 'COMPLETED') resolved_count FROM growth_operator_task");
    return dto(
        agent,
        execution,
        number(tasks, "open_count"),
        number(tasks, "resolved_count"),
        number(tasks, "resolved_count"));
  }

  /** Consolida conciliações do Financeiro. */
  private AgentMaturityDto financial(Agent agent) {
    return dto(agent, aggregate("financial_agent_execution"), 0, 0, 0);
  }

  /** Consolida avaliações simuladas e resultados humanos do Agente Cliente. */
  private AgentMaturityDto customer(Agent agent) {
    Map<String, Object> execution = aggregate("customer_agent_evaluation");
    long confirmed =
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM customer_agent_evaluation WHERE human_result_json IS NOT NULL AND TRIM(human_result_json) <> ''",
            Long.class);
    long open =
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM customer_agent_evaluation WHERE status IN ('PENDING','RUNNING')",
            Long.class);
    return dto(agent, execution, open, confirmed, confirmed);
  }

  /** Calcula os indicadores comuns sem promover hipótese a resultado confirmado. */
  private AgentMaturityDto dto(
      Agent agent, Map<String, Object> row, long open, long resolved, long confirmed) {
    long total = number(row, "total");
    long completed = number(row, "completed");
    long failed = number(row, "failed");
    BigDecimal completion = rate(completed, total);
    BigDecimal resolution = rate(resolved, open + resolved);
    String level =
        confirmed >= 10 && completion.compareTo(BigDecimal.valueOf(90)) >= 0
            ? "MADURO"
            : completed > 0 ? "EM_VALIDACAO" : "INICIAL";
    String next =
        open > 0
            ? "Concluir pendências abertas com evidência posterior."
            : confirmed < 10
                ? "Acumular dez resultados humanos ou operacionais confirmados."
                : "Avaliar ampliação controlada de autonomia.";
    Object last = row.get("last_at");
    return new AgentMaturityDto(
        agent.getId(),
        agent.getAgentKey(),
        agent.getName(),
        total,
        completed,
        failed,
        open,
        resolved,
        confirmed,
        decimal(row, "cost"),
        completion,
        resolution,
        last instanceof Timestamp timestamp
            ? timestamp.toInstant()
            : last instanceof Instant instant ? instant : null,
        level,
        next);
  }

  /** Agrega estados comuns das tabelas de execução. */
  private Map<String, Object> aggregate(String table) {
    String cost =
        table.equals("customer_agent_evaluation") ? "0" : "COALESCE(SUM(estimated_cost), 0)";
    return jdbc.queryForMap(
        "SELECT COUNT(*) total, SUM(status = 'COMPLETED') completed, SUM(status = 'FAILED') failed, "
            + cost
            + " cost, MAX(COALESCE(finished_at, created_at)) last_at FROM "
            + table);
  }

  /** Retorna painel vazio para agentes ainda sem executor integrado. */
  private AgentMaturityDto empty(Agent agent) {
    return new AgentMaturityDto(
        agent.getId(),
        agent.getAgentKey(),
        agent.getName(),
        0,
        0,
        0,
        0,
        0,
        0,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        null,
        "NAO_INTEGRADO",
        "Integrar execuções e resultados ao ciclo compartilhado.");
  }

  /** Converte um contador SQL anulável. */
  private long number(Map<String, Object> row, String key) {
    Object value = row.get(key);
    return value instanceof Number number ? number.longValue() : 0;
  }

  /** Converte um valor monetário SQL anulável. */
  private BigDecimal decimal(Map<String, Object> row, String key) {
    Object value = row.get(key);
    return value instanceof BigDecimal decimal
        ? decimal
        : value instanceof Number number
            ? BigDecimal.valueOf(number.doubleValue())
            : BigDecimal.ZERO;
  }

  /** Calcula percentual sem divisão por zero. */
  private BigDecimal rate(long numerator, long denominator) {
    return denominator == 0
        ? BigDecimal.ZERO
        : BigDecimal.valueOf(numerator * 100.0 / denominator).setScale(1, RoundingMode.HALF_UP);
  }
}
