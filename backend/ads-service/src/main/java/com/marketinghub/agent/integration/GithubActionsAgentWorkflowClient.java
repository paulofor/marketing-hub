package com.marketinghub.agent.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** Responsabilidade: consultar a API pública do GitHub Actions sem expor credenciais. */
@Component
public class GithubActionsAgentWorkflowClient implements AgentWorkflowClient {
  private static final String API_BASE_URL = "https://api.github.com/repos/";

  private final HttpClient httpClient;
  private final ObjectMapper objectMapper;
  private final AgentWorkflowProperties properties;

  /** Configura o cliente HTTP, o conversor JSON e as propriedades do repositório. */
  public GithubActionsAgentWorkflowClient(
      HttpClient httpClient, ObjectMapper objectMapper, AgentWorkflowProperties properties) {
    this.httpClient = httpClient;
    this.objectMapper = objectMapper;
    this.properties = properties;
  }

  /** Consulta até cem execuções concluídas da branch operacional do repositório. */
  @Override
  public List<WorkflowRun> listCompletedRuns(String repository, String branch)
      throws IOException, InterruptedException {
    String encodedBranch = java.net.URLEncoder.encode(branch, StandardCharsets.UTF_8);
    URI uri =
        URI.create(
            API_BASE_URL + repository + "/actions/runs?branch=" + encodedBranch + "&per_page=100");
    HttpRequest.Builder requestBuilder =
        HttpRequest.newBuilder(uri)
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .GET();
    if (StringUtils.hasText(properties.getGithubToken())) {
      requestBuilder.header("Authorization", "Bearer " + properties.getGithubToken());
    }

    HttpResponse<String> response =
        httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
    if (response.statusCode() < 200 || response.statusCode() >= 300) {
      throw new IOException("GitHub Actions respondeu HTTP " + response.statusCode());
    }
    return parseRuns(response.body());
  }

  /** Converte a resposta do GitHub em registros mínimos sem transportar payload bruto. */
  private List<WorkflowRun> parseRuns(String body) throws IOException {
    JsonNode root = objectMapper.readTree(body);
    List<WorkflowRun> runs = new ArrayList<>();
    for (JsonNode node : root.path("workflow_runs")) {
      String status = node.path("status").asText(null);
      if (!"completed".equals(status)) {
        continue;
      }
      runs.add(
          new WorkflowRun(
              node.path("path").asText(null),
              node.path("name").asText(null),
              node.path("head_branch").asText(null),
              status,
              node.path("conclusion").asText(null),
              parseInstant(node.path("updated_at").asText(null)),
              node.path("html_url").asText(null)));
    }
    return List.copyOf(runs);
  }

  /** Converte uma data ISO do GitHub e mantém ausência de valor como nulo. */
  private Instant parseInstant(String value) {
    return StringUtils.hasText(value) ? Instant.parse(value) : null;
  }
}
