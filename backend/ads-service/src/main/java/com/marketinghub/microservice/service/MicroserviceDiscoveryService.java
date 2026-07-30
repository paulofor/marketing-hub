package com.marketinghub.microservice.service;

import com.marketinghub.microservice.VpsHostInventory;
import com.marketinghub.microservice.dto.DeploymentWorkflowInventoryDto;
import com.marketinghub.microservice.dto.DiscoveredMicroserviceDto;
import com.marketinghub.microservice.dto.OperationalInventoryDto;
import com.marketinghub.microservice.dto.UpdateVpsHostInventoryRequest;
import com.marketinghub.microservice.dto.VpsHostInventoryDto;
import com.marketinghub.repository.jpa.microservice.VpsHostInventoryRepository;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
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
  private final VpsHostInventoryRepository hostInventoryRepository;

  /** Inicializa o serviço com os caminhos versionados usados como fonte do inventário. */
  public MicroserviceDiscoveryService(
      @Value("${microservice.discovery.compose-path:docker-compose.yml}") String composePath,
      @Value("${microservice.discovery.workflows-path:.github/workflows}") String workflowsPath,
      @Value("${microservice.discovery.health-path:/actuator/health}") String defaultHealthPath,
      VpsHostInventoryRepository hostInventoryRepository) {
    this.composePath = Paths.get(composePath);
    this.workflowsPath = Paths.get(workflowsPath);
    this.defaultHealthPath = defaultHealthPath;
    this.hostInventoryRepository = hostInventoryRepository;
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
  @Transactional(readOnly = true)
  public OperationalInventoryDto discoverOperationalInventory() {
    return new OperationalInventoryDto(
        discoverFromCompose(), discoverDeploymentsFromWorkflows(), discoverEditableHosts());
  }

  /** Busca um host VPS editável usando banco como verdade e YAML como cadastro inicial. */
  @Transactional(readOnly = true)
  public VpsHostInventoryDto getHostInventory(String host) {
    String normalizedHost = normalizeHost(host);
    return hostInventoryRepository
        .findByHost(normalizedHost)
        .map(this::toHostDto)
        .orElseGet(
            () ->
                fallbackHost(normalizedHost)
                    .orElseThrow(
                        () ->
                            new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Host VPS não encontrado no inventário: " + normalizedHost)));
  }

  /** Atualiza ou cria o cadastro editável de um host VPS. */
  @Transactional
  public VpsHostInventoryDto updateHostInventory(
      String host, UpdateVpsHostInventoryRequest request) {
    String normalizedHost = normalizeHost(host);
    VpsHostInventory entity =
        hostInventoryRepository
            .findByHost(normalizedHost)
            .orElseGet(
                () -> toHostEntity(fallbackHost(normalizedHost).orElse(null), normalizedHost));
    applyHostRequest(entity, request);
    return toHostDto(hostInventoryRepository.save(entity));
  }

  /** Junta hosts versionados com substituições persistidas pela tela administrativa. */
  private List<VpsHostInventoryDto> discoverEditableHosts() {
    Map<String, VpsHostInventoryDto> hostsByAddress = new LinkedHashMap<>();
    for (VpsHostInventoryDto host : discoverHostsFromFallbackResource()) {
      hostsByAddress.put(host.host(), host);
    }
    for (VpsHostInventory entity : hostInventoryRepository.findAllByOrderByHostAsc()) {
      hostsByAddress.put(entity.getHost(), toHostDto(entity));
    }
    return hostsByAddress.values().stream()
        .sorted(Comparator.comparing(VpsHostInventoryDto::host))
        .toList();
  }

  /** Localiza um host no inventário embarcado. */
  private java.util.Optional<VpsHostInventoryDto> fallbackHost(String host) {
    return discoverHostsFromFallbackResource().stream()
        .filter(candidate -> candidate.host().equals(host))
        .findFirst();
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

  /** Carrega o cadastro versionado de hosts VPS com provedor, capacidade e custo. */
  private List<VpsHostInventoryDto> discoverHostsFromFallbackResource() {
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
      Object hostsNode = root.get("hosts");
      if (!(hostsNode instanceof Iterable<?> hosts)) {
        return List.of();
      }

      List<VpsHostInventoryDto> discovered = new ArrayList<>();
      for (Object hostNode : hosts) {
        VpsHostInventoryDto host = toFallbackHostDto(hostNode);
        if (host != null) {
          discovered.add(host);
        }
      }
      discovered.sort(Comparator.comparing(VpsHostInventoryDto::host));
      return discovered;
    } catch (IOException e) {
      throw new UncheckedIOException(
          "Failed to read fallback host inventory at " + FALLBACK_DEPLOYMENTS_RESOURCE, e);
    }
  }

  /** Converte um item do cadastro de hosts para DTO seguro da tela. */
  private VpsHostInventoryDto toFallbackHostDto(Object hostNode) {
    Map<?, ?> host = asMap(hostNode);
    if (host.isEmpty()) {
      return null;
    }

    String hostAddress = textOrDefault(host.get("host"), null);
    if (!hasText(hostAddress)) {
      return null;
    }

    return new VpsHostInventoryDto(
        hostAddress,
        textOrDefault(host.get("providerName"), null),
        textOrDefault(host.get("providerEvidence"), null),
        textOrDefault(host.get("cpu"), null),
        integerOrNull(host.get("memoryGb")),
        integerOrNull(host.get("diskGb")),
        textOrDefault(host.get("operatingSystem"), null),
        decimalOrNull(host.get("monthlyCostBrl")),
        textOrDefault(host.get("billingCycle"), null),
        textOrDefault(host.get("costEvidence"), null),
        textOrDefault(host.get("physicalSpecsEvidence"), null),
        textOrDefault(host.get("notes"), null));
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

  /** Converte número YAML para inteiro quando o cadastro trouxe valor confirmado. */
  private Integer integerOrNull(Object value) {
    if (value instanceof Number number) {
      return number.intValue();
    }
    return null;
  }

  /** Converte número YAML para decimal quando há custo confirmado. */
  private BigDecimal decimalOrNull(Object value) {
    if (value instanceof BigDecimal decimal) {
      return decimal;
    }
    if (value instanceof Number number) {
      return BigDecimal.valueOf(number.doubleValue());
    }
    return null;
  }

  /** Cria entidade editável a partir do fallback ou apenas do endereço informado. */
  private VpsHostInventory toHostEntity(VpsHostInventoryDto fallback, String host) {
    VpsHostInventory entity = new VpsHostInventory();
    entity.setHost(host);
    if (fallback != null) {
      entity.setProviderName(fallback.providerName());
      entity.setProviderEvidence(fallback.providerEvidence());
      entity.setCpu(fallback.cpu());
      entity.setMemoryGb(fallback.memoryGb());
      entity.setDiskGb(fallback.diskGb());
      entity.setOperatingSystem(fallback.operatingSystem());
      entity.setMonthlyCostBrl(fallback.monthlyCostBrl());
      entity.setBillingCycle(fallback.billingCycle());
      entity.setCostEvidence(fallback.costEvidence());
      entity.setPhysicalSpecsEvidence(fallback.physicalSpecsEvidence());
      entity.setNotes(fallback.notes());
    }
    return entity;
  }

  /** Aplica campos editáveis normalizados no cadastro de VPS. */
  private void applyHostRequest(VpsHostInventory entity, UpdateVpsHostInventoryRequest request) {
    entity.setProviderName(normalizeOptional(request.providerName()));
    entity.setProviderEvidence(normalizeOptional(request.providerEvidence()));
    entity.setCpu(normalizeOptional(request.cpu()));
    entity.setMemoryGb(request.memoryGb());
    entity.setDiskGb(request.diskGb());
    entity.setOperatingSystem(normalizeOptional(request.operatingSystem()));
    entity.setMonthlyCostBrl(request.monthlyCostBrl());
    entity.setBillingCycle(normalizeOptional(request.billingCycle()));
    entity.setCostEvidence(normalizeOptional(request.costEvidence()));
    entity.setPhysicalSpecsEvidence(normalizeOptional(request.physicalSpecsEvidence()));
    entity.setNotes(normalizeOptional(request.notes()));
  }

  /** Converte entidade persistida para DTO de inventário operacional. */
  private VpsHostInventoryDto toHostDto(VpsHostInventory entity) {
    return new VpsHostInventoryDto(
        entity.getHost(),
        entity.getProviderName(),
        entity.getProviderEvidence(),
        entity.getCpu(),
        entity.getMemoryGb(),
        entity.getDiskGb(),
        entity.getOperatingSystem(),
        entity.getMonthlyCostBrl(),
        entity.getBillingCycle(),
        entity.getCostEvidence(),
        entity.getPhysicalSpecsEvidence(),
        entity.getNotes());
  }

  /** Normaliza e valida o endereço de host usado em rotas administrativas. */
  private String normalizeHost(String host) {
    if (!StringUtils.hasText(host)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Host VPS é obrigatório");
    }
    return host.trim();
  }

  /** Normaliza campos opcionais vazios para nulo antes de persistir. */
  private String normalizeOptional(String value) {
    return StringUtils.hasText(value) ? value.trim() : null;
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
