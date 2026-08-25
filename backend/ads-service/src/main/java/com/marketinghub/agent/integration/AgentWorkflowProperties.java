package com.marketinghub.agent.integration;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Responsabilidade: concentrar o cadastro versionado dos workflows que atualizam cada agente. */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "agents.workflow")
public class AgentWorkflowProperties {
  private String repository = "paulofor/marketing-hub";
  private String branch = "main";
  private String githubToken;
  private Duration cacheTtl = Duration.ofMinutes(10);
  private Map<String, String> workflowByAgentKey = new LinkedHashMap<>();
}
