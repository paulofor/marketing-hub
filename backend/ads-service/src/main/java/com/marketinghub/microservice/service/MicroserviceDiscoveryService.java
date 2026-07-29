package com.marketinghub.microservice.service;

import com.marketinghub.microservice.dto.DeploymentWorkflowInventoryDto;
import com.marketinghub.microservice.dto.DiscoveredMicroserviceDto;
import com.marketinghub.microservice.dto.OperationalInventoryDto;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

/**
 * Responsabilidade: descobrir inventário operacional versionado a partir de compose e workflows.
 */
@Service
public class MicroserviceDiscoveryService {
  private static final String FALLBACK_DEPLOYMENTS_RESOURCE =
      "operational-inventory/project-vps-deployments.yaml";
  private static final Pattern SECRET_REFERENCE_PATTERN = Pattern.compile("secrets\\.([A-Z0-9_]+)");

  private final Path composePath;
  private final Path workflowsPath;
  private final String defaultHealthPath;

  /** Inicializa o serviço com os caminhos versionados usados como fonte do inventário. */
  public MicroserviceDiscoveryService(
      @Value("${microservice.discovery.compose-path:docker-compose.yml}") String composePath,
      @Value("${microservice.discovery.workflows-path:.github/workflows}") String workflowsPath,
      @Value("${microservice.discovery.health-path:/actuator/health}") String defaultHealthPath) {
    this.composePath = Paths.get(composePath);
    this.workflowsPath = Paths.get(workflowsPath);
    this.defaultHealthPath = defaultHealthPath;
  }

  /** Descobre os serviços publicados no docker-compose configurado. */
  public List<DiscoveredMicroserviceDto> discoverFromCompose() {
    if (!Files.exists(composePath)) {
      return List.of();
    }

    Yaml yaml = new Yaml();
    try (InputStream inputStream = Files.newInputStream(composePath)) {
      Object data = yaml.load(inputStream);
      if (!(data instanceof Map<?, ?> root)) {
        return List.of();
      }

      Object servicesNode = root.get("services");
      if (!(servicesNode instanceof Map<?, ?> services)) {
        return List.of();
      }

      List<DiscoveredMicroserviceDto> discovered = new ArrayList<>();
      for (Map.Entry<?, ?> entry : services.entrySet()) {
        DiscoveredMicroserviceDto dto = toDto(entry);
        if (dto != null) {
          discovered.add(dto);
        }
      }
      discovered.sort(Comparator.comparing(DiscoveredMicroserviceDto::serviceName));
      return discovered;
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to read docker-compose file at " + composePath, e);
    }
  }

  /** Consolida portas do compose e dados de deploy dos workflows em um único inventário. */
  public OperationalInventoryDto discoverOperationalInventory() {
    return new OperationalInventoryDto(discoverFromCompose(), discoverDeploymentsFromWorkflows());
  }

  /** Descobre os deploys declarados nos workflows versionados do repositório. */
  public List<DeploymentWorkflowInventoryDto> discoverDeploymentsFromWorkflows() {
    if (!Files.isDirectory(workflowsPath)) {
      return discoverDeploymentsFromFallbackResource();
    }

    try (var stream = Files.list(workflowsPath)) {
      List<DeploymentWorkflowInventoryDto> deployments = new ArrayList<>();
      stream
          .filter(Files::isRegularFile)
          .filter(this::isYamlFile)
          .sorted()
          .forEach(path -> deployments.addAll(discoverDeploymentsFromWorkflow(path)));
      deployments.sort(
          Comparator.comparing(DeploymentWorkflowInventoryDto::deployHost)
              .thenComparing(DeploymentWorkflowInventoryDto::workflowFile)
              .thenComparing(DeploymentWorkflowInventoryDto::jobName));
      if (deployments.isEmpty()) {
        return discoverDeploymentsFromFallbackResource();
      }
      return deployments;
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to read workflows at " + workflowsPath, e);
    }
  }

  /** Carrega o inventário VPS embarcado quando os workflows não estão disponíveis no runtime. */
  private List<DeploymentWorkflowInventoryDto> discoverDeploymentsFromFallbackResource() {
    try (InputStream inputStream =
        MicroserviceDiscoveryService.class
            .getClassLoader()
            .getResourceAsStream(FALLBACK_DEPLOYMENTS_RESOURCE)) {
      if (inputStream == null) {
        return List.of();
      }

      Yaml yaml = new Yaml();
      Object data = yaml.load(inputStream);
      if (!(data instanceof Map<?, ?> root)) {
        return List.of();
      }
      Object deploymentsNode = root.get("deployments");
      if (!(deploymentsNode instanceof Iterable<?> deployments)) {
        return List.of();
      }

      List<DeploymentWorkflowInventoryDto> discovered = new ArrayList<>();
      for (Object deploymentNode : deployments) {
        DeploymentWorkflowInventoryDto deployment = toFallbackDeploymentDto(deploymentNode);
        if (deployment != null) {
          discovered.add(deployment);
        }
      }
      discovered.sort(
          Comparator.comparing(DeploymentWorkflowInventoryDto::deployHost)
              .thenComparing(DeploymentWorkflowInventoryDto::workflowFile)
              .thenComparing(DeploymentWorkflowInventoryDto::jobName));
      return discovered;
    } catch (IOException e) {
      throw new UncheckedIOException(
          "Failed to read fallback deployment inventory at " + FALLBACK_DEPLOYMENTS_RESOURCE, e);
    }
  }

  /** Converte um item do inventário embarcado para DTO da tela de VPS. */
  private DeploymentWorkflowInventoryDto toFallbackDeploymentDto(Object deploymentNode) {
    Map<?, ?> deployment = asMap(deploymentNode);
    if (deployment.isEmpty()) {
      return null;
    }

    String deployHost = textOrDefault(deployment.get("deployHost"), null);
    if (!hasText(deployHost)) {
      return null;
    }

    return new DeploymentWorkflowInventoryDto(
        textOrDefault(deployment.get("workflowFile"), "inventario-versionado.yaml"),
        textOrDefault(deployment.get("workflowName"), "Inventario VPS versionado"),
        textOrDefault(deployment.get("jobName"), "deploy"),
        deployHost,
        textOrDefault(deployment.get("deployUser"), null),
        textOrDefault(deployment.get("remotePath"), null),
        toTextList(deployment.get("secretReferences")),
        textOrDefault(deployment.get("triggerMode"), "automatico"));
  }

  /** Converte uma entrada de serviço do docker-compose para DTO de descoberta. */
  private DiscoveredMicroserviceDto toDto(Map.Entry<?, ?> entry) {
    if (!(entry.getKey() instanceof String serviceName)) {
      return null;
    }

    Object value = entry.getValue();
    if (!(value instanceof Map<?, ?> serviceDefinition)) {
      return null;
    }

    PortMapping ports = extractPortMapping(serviceDefinition.get("ports"));
    String baseUrl = buildBaseUrl(serviceName, ports);
    String image = extractImage(serviceDefinition.get("image"));

    return new DiscoveredMicroserviceDto(
        serviceName, image, ports.hostPort(), ports.containerPort(), baseUrl, defaultHealthPath);
  }

  /** Extrai a imagem declarada no serviço do compose. */
  private String extractImage(Object imageNode) {
    if (imageNode instanceof String image) {
      return image;
    }
    return null;
  }

  /** Extrai o primeiro mapeamento de porta útil do serviço do compose. */
  private PortMapping extractPortMapping(Object portsNode) {
    if (!(portsNode instanceof Iterable<?> ports)) {
      return PortMapping.EMPTY;
    }

    for (Object port : ports) {
      PortMapping parsed = parsePort(port);
      if (parsed != null) {
        return parsed;
      }
    }

    return PortMapping.EMPTY;
  }

  /** Interpreta um item de porta do compose nos formatos numérico e texto. */
  private PortMapping parsePort(Object port) {
    if (port instanceof Number numberPort) {
      int value = numberPort.intValue();
      return new PortMapping(value, value, false);
    }

    if (!(port instanceof String portString)) {
      return null;
    }

    String sanitized = portString.split("/")[0];
    String[] parts = sanitized.split(":");

    if (parts.length == 2) {
      Integer hostPort = parsePortNumber(parts[0]);
      Integer containerPort = parsePortNumber(parts[1]);
      if (hostPort != null || containerPort != null) {
        return new PortMapping(hostPort, containerPort, true);
      }
    }

    Integer singlePort = parsePortNumber(parts[0]);
    if (singlePort != null) {
      return new PortMapping(singlePort, singlePort, false);
    }

    return null;
  }

  /** Converte texto de porta para número inteiro quando possível. */
  private Integer parsePortNumber(String port) {
    try {
      return Integer.parseInt(port);
    } catch (NumberFormatException ignored) {
      return null;
    }
  }

  /** Monta a Base URL operacional conforme a porta publicada ou a porta interna do serviço. */
  private String buildBaseUrl(String serviceName, PortMapping portMapping) {
    if (portMapping.hostBinding() && portMapping.hostPort() != null) {
      return "http://localhost:" + portMapping.hostPort();
    }

    if (portMapping.containerPort() != null) {
      return "http://" + serviceName + ":" + portMapping.containerPort();
    }

    return "http://" + serviceName;
  }

  /** Descobre os jobs de deploy de um workflow específico. */
  private List<DeploymentWorkflowInventoryDto> discoverDeploymentsFromWorkflow(Path workflowPath) {
    Yaml yaml = new Yaml();
    try (InputStream inputStream = Files.newInputStream(workflowPath)) {
      Object data = yaml.load(inputStream);
      if (!(data instanceof Map<?, ?> root)) {
        return List.of();
      }

      Object jobsNode = root.get("jobs");
      if (!(jobsNode instanceof Map<?, ?> jobs)) {
        return List.of();
      }

      List<DeploymentWorkflowInventoryDto> deployments = new ArrayList<>();
      String workflowText = Files.readString(workflowPath);
      Map<?, ?> rootEnv = asMap(root.get("env"));
      for (Map.Entry<?, ?> jobEntry : jobs.entrySet()) {
        DeploymentWorkflowInventoryDto deployment =
            toDeploymentDto(workflowPath, root, rootEnv, jobEntry, workflowText);
        if (deployment != null) {
          deployments.add(deployment);
        }
      }
      return deployments;
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to read workflow at " + workflowPath, e);
    }
  }

  /** Converte um job de workflow em item de inventário quando ele declara host de deploy. */
  private DeploymentWorkflowInventoryDto toDeploymentDto(
      Path workflowPath,
      Map<?, ?> root,
      Map<?, ?> rootEnv,
      Map.Entry<?, ?> jobEntry,
      String workflowText) {
    if (!(jobEntry.getKey() instanceof String jobName)) {
      return null;
    }
    Map<?, ?> job = asMap(jobEntry.getValue());
    if (job.isEmpty()) {
      return null;
    }

    Map<?, ?> env = mergeEnv(rootEnv, asMap(job.get("env")));
    String deployHost = firstText(env, "DEPLOY_HOST", "MCP_VPS_IP", "VPS_HOST", "HOST");
    if (!hasText(deployHost)) {
      return null;
    }

    String workflowFile = workflowsPath.relativize(workflowPath).toString();
    String workflowName = textOrDefault(root.get("name"), workflowFile);
    String deployUser = firstText(env, "DEPLOY_USER", "VPS_USER", "SSH_USER");
    String remotePath = firstText(env, "REMOTE_PATH", "DEPLOY_DIR", "APP_DIR");
    List<String> secretReferences = extractSecretReferences(workflowText);
    String triggerMode = resolveTriggerMode(job.get("if"), workflowText);

    return new DeploymentWorkflowInventoryDto(
        workflowFile,
        workflowName,
        jobName,
        deployHost,
        deployUser,
        remotePath,
        secretReferences,
        triggerMode);
  }

  /** Junta variáveis de ambiente do workflow e do job, preservando precedência do job. */
  private Map<?, ?> mergeEnv(Map<?, ?> rootEnv, Map<?, ?> jobEnv) {
    if (rootEnv.isEmpty()) {
      return jobEnv;
    }
    if (jobEnv.isEmpty()) {
      return rootEnv;
    }
    java.util.LinkedHashMap<Object, Object> merged = new java.util.LinkedHashMap<>(rootEnv);
    merged.putAll(jobEnv);
    return merged;
  }

  /** Resolve se o deploy é manual ou automático conforme gatilhos e condição do job. */
  private String resolveTriggerMode(Object jobCondition, String workflowText) {
    String condition = String.valueOf(jobCondition == null ? "" : jobCondition);
    if (condition.contains("workflow_dispatch")) {
      return "manual";
    }
    if (workflowText.contains("workflow_dispatch") && !workflowText.contains("push:")) {
      return "manual";
    }
    return "automatico";
  }

  /** Extrai nomes de secrets referenciados no workflow, sem expor valores. */
  private List<String> extractSecretReferences(String workflowText) {
    Matcher matcher = SECRET_REFERENCE_PATTERN.matcher(workflowText);
    Set<String> references = new LinkedHashSet<>();
    while (matcher.find()) {
      references.add(matcher.group(1));
    }
    return List.copyOf(references);
  }

  /** Indica se o caminho aponta para arquivo YAML. */
  private boolean isYamlFile(Path path) {
    String fileName = path.getFileName().toString();
    return fileName.endsWith(".yml") || fileName.endsWith(".yaml");
  }

  /** Converte um nó YAML para mapa quando possível. */
  private Map<?, ?> asMap(Object node) {
    if (node instanceof Map<?, ?> map) {
      return map;
    }
    return Map.of();
  }

  /** Localiza o primeiro valor textual presente no mapa para as chaves candidatas. */
  private String firstText(Map<?, ?> values, String... keys) {
    for (String key : keys) {
      String value = textOrDefault(values.get(key), null);
      if (hasText(value)) {
        return value;
      }
    }
    return null;
  }

  /** Converte valor simples para texto ou devolve fallback. */
  private String textOrDefault(Object value, String fallback) {
    if (value instanceof String text && hasText(text)) {
      return text;
    }
    return fallback;
  }

  /** Converte uma lista YAML simples em lista textual para exposição segura na tela. */
  private List<String> toTextList(Object value) {
    if (!(value instanceof Iterable<?> iterable)) {
      return List.of();
    }

    List<String> result = new ArrayList<>();
    for (Object item : iterable) {
      String text = textOrDefault(item, null);
      if (hasText(text)) {
        result.add(text);
      }
    }
    return List.copyOf(result);
  }

  /** Verifica se o texto tem conteúdo útil. */
  private boolean hasText(String value) {
    return value != null && !value.isBlank();
  }

  /** Representa um mapeamento de porta encontrado no compose. */
  private record PortMapping(Integer hostPort, Integer containerPort, boolean hostBinding) {
    private static final PortMapping EMPTY = new PortMapping(null, null, false);
  }
}
