package com.marketinghub.config;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.info.GitProperties;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Responsabilidade: expor identidade rastreável do build do backend principal para diagnóstico. */
@RestController
public class RuntimeBuildInfoController {

  private final BuildProperties buildProperties;
  private final GitProperties gitProperties;
  private final Environment environment;

  /** Inicializa a leitura opcional de metadados de build, git e ambiente do container. */
  public RuntimeBuildInfoController(
      java.util.Optional<BuildProperties> buildProperties,
      java.util.Optional<GitProperties> gitProperties,
      Environment environment) {
    this.buildProperties = buildProperties.orElse(null);
    this.gitProperties = gitProperties.orElse(null);
    this.environment = environment;
  }

  /** Retorna a identidade de build em rota estável usada pelo MCP Server. */
  @GetMapping("/actuator/info")
  public Map<String, Object> actuatorInfo() {
    return buildInfo();
  }

  /** Monta o payload compatível com os formatos lidos pela tool MCP runtime_build_info. */
  private Map<String, Object> buildInfo() {
    Map<String, Object> response = new LinkedHashMap<>();
    Map<String, Object> build = new LinkedHashMap<>();
    Map<String, Object> git = new LinkedHashMap<>();

    putIfPresent(build, "version", valueFromBuild("version", "0.0.1-SNAPSHOT"));
    putIfPresent(build, "time", valueFromBuild("time", null));
    putIfPresent(
        git,
        "branch",
        firstText(gitValue("branch"), env("BACKEND_BUILD_BRANCH"), env("GITHUB_REF_NAME")));
    putIfPresent(
        git,
        "commit.id",
        firstText(gitValue("commit.id"), env("BACKEND_BUILD_COMMIT"), env("GITHUB_SHA")));
    putIfPresent(git, "commit.id.abbrev", abbreviate((String) git.get("commit.id")));

    response.put("app", Map.of("name", "marketinghub-backend"));
    response.put("build", build);
    response.put("git", git);
    response.put("queriedAt", Instant.now().toString());
    return response;
  }

  /** Recupera um campo do build-info gerado pelo Maven com fallback explícito. */
  private String valueFromBuild(String key, String fallback) {
    if (buildProperties == null) {
      return fallback;
    }
    String value =
        switch (key) {
          case "version" -> buildProperties.getVersion();
          case "time" ->
              buildProperties.getTime() != null ? buildProperties.getTime().toString() : null;
          default -> buildProperties.get(key);
        };
    return firstText(value, fallback);
  }

  /** Recupera um campo do git.properties quando disponível. */
  private String gitValue(String key) {
    return gitProperties == null ? null : gitProperties.get(key);
  }

  /** Recupera variável de ambiente ou propriedade equivalente exposta ao runtime. */
  private String env(String key) {
    return environment.getProperty(key);
  }

  /** Adiciona um campo ao payload somente quando há valor útil. */
  private void putIfPresent(Map<String, Object> target, String key, String value) {
    if (StringUtils.hasText(value)) {
      target.put(key, value);
    }
  }

  /** Retorna o primeiro texto preenchido entre os candidatos. */
  private String firstText(String... candidates) {
    for (String candidate : candidates) {
      if (StringUtils.hasText(candidate) && !"unknown".equalsIgnoreCase(candidate.trim())) {
        return candidate.trim();
      }
    }
    return null;
  }

  /** Gera a versão curta do commit quando houver SHA completo. */
  private String abbreviate(String commitId) {
    if (!StringUtils.hasText(commitId)) {
      return null;
    }
    return commitId.length() <= 12 ? commitId : commitId.substring(0, 12);
  }
}
