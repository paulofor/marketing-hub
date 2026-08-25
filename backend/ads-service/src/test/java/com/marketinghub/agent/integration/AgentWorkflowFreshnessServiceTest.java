package com.marketinghub.agent.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.marketinghub.agent.Agent;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Responsabilidade: validar a associação entre agentes e a recência dos workflows do executor. */
class AgentWorkflowFreshnessServiceTest {

  /** Seleciona a execução mais recente da branch operacional para cada agente. */
  @Test
  void resolvesLatestCompletedWorkflowByAgent() {
    AgentWorkflowProperties properties = properties();
    AgentWorkflowClient client =
        (repository, branch) ->
            List.of(
                run(
                    ".github/workflows/landing-generator-agent-worker-ci.yml",
                    "2026-08-20T10:00:00Z",
                    "success"),
                run(
                    ".github/workflows/landing-generator-agent-worker-ci.yml",
                    "2026-08-25T10:00:00Z",
                    "success"),
                run(
                    ".github/workflows/landing-generator-agent-worker-ci.yml",
                    "2026-08-24T10:00:00Z",
                    "failure"),
                run(
                    ".github/workflows/customer-agent-worker-ci.yml",
                    "2026-08-25T11:00:00Z",
                    "success"));
    Agent landingGenerator = agent(7L, "landing-generator");
    Agent customerAgent = agent(2L, "customer-agent");

    Map<Long, AgentWorkflowFreshness> result =
        new AgentWorkflowFreshnessService(client, properties)
            .currentWorkflowRuns(List.of(landingGenerator, customerAgent));

    assertThat(result.get(7L).lastWorkflowRunAt()).isEqualTo(Instant.parse("2026-08-25T10:00:00Z"));
    assertThat(result.get(7L).workflowConclusion()).isEqualTo("success");
    assertThat(result.get(2L).workflowFile())
        .isEqualTo(".github/workflows/customer-agent-worker-ci.yml");
  }

  /** Mantém a tela funcional quando o provedor externo estiver indisponível. */
  @Test
  void preservesContractDataWhenGithubFails() {
    AgentWorkflowClient client =
        (repository, branch) -> {
          throw new IOException("GitHub indisponível");
        };

    Map<Long, AgentWorkflowFreshness> result =
        new AgentWorkflowFreshnessService(client, properties())
            .currentWorkflowRuns(List.of(agent(7L, "landing-generator")));

    assertThat(result).isEmpty();
  }

  /** Cria as regras versionadas usadas para relacionar agentes e workflows. */
  private AgentWorkflowProperties properties() {
    AgentWorkflowProperties properties = new AgentWorkflowProperties();
    properties.setBranch("main");
    properties.setCacheTtl(java.time.Duration.ZERO);
    properties.setWorkflowByAgentKey(
        Map.of(
            "landing-generator", ".github/workflows/landing-generator-agent-worker-ci.yml",
            "customer-agent", ".github/workflows/customer-agent-worker-ci.yml"));
    return properties;
  }

  /** Cria um agente mínimo para o cenário de recência do executor. */
  private Agent agent(Long id, String agentKey) {
    Agent agent = new Agent();
    agent.setId(id);
    agent.setAgentKey(agentKey);
    return agent;
  }

  /** Cria uma execução concluída do workflow para o teste. */
  private AgentWorkflowClient.WorkflowRun run(
      String workflowFile, String completedAt, String conclusion) {
    return new AgentWorkflowClient.WorkflowRun(
        workflowFile,
        workflowFile,
        "main",
        "completed",
        conclusion,
        Instant.parse(completedAt),
        "https://github.com/example/actions/runs/1");
  }
}
