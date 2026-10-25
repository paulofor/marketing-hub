package com.marketinghub.product.modeling.v1.service;

import com.fasterxml.jackson.databind.*;
import com.marketinghub.product.modeling.v1.ProductModeling;
import com.marketinghub.product.modeling.v1.service.get.*;
import com.marketinghub.product.modeling.v1.service.save.SaveModelingRequest;
import com.marketinghub.productdiscovery.v1.*;
import com.marketinghub.productdiscovery.v1.service.*;
import com.marketinghub.repository.jpa.product.ProductRepository;
import com.marketinghub.repository.jpa.productdiscovery.ProductDiscoveryCycleRepository;
import com.marketinghub.repository.jpa.productmodeling.ProductModelingRepository;
import java.net.URI;
import java.time.Instant;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Coordena a entrada por referências reutilizando a descoberta e a cadeia existentes. */
@Service
@RequiredArgsConstructor
@Slf4j
public class ModelingService {
  private final ProductModelingRepository modelings;
  private final ProductRepository products;
  private final ProductDiscoveryCycleRepository cycles;
  private final ProductDiscoveryService discovery;
  private final ObjectMapper json;
  private final ModelingHandoff handoff;
  private final ModelingAdoption adoption;
  private final com.marketinghub.repository.jpa.agenttask.AgentTaskRepository tasks;

  /** Lista a situação persistida sem disparar pesquisa ou alterar produtos. */
  @Transactional(readOnly = true)
  public List<ModelingView> list() {
    return modelings.findTop50ByOrderByIdDesc().stream().map(this::view).toList();
  }

  /** Consulta uma preparação e mantém ausências e bloqueios explícitos. */
  @Transactional(readOnly = true)
  public ModelingView get(Long id) {
    return view(required(id));
  }

  /** Salva intenção imutável e pesquisa em rascunho, sem novas chamadas ou autorização herdada. */
  @Transactional
  public ModelingView prepare(SaveModelingRequest request) {
    List<String> urls = validateUrls(request.referenceUrls());
    String outcome = request.desiredOutcome().trim();
    var product =
        request.productId() == null
            ? null
            : products
                .findLockedById(request.productId())
                .orElseThrow(() -> error("Produto não encontrado."));
    String name =
        product == null
            ? Objects.toString(request.newProductName(), "").trim()
            : Objects.toString(product.getInternalName(), product.getName());
    if (name.isBlank() || outcome.isBlank()) throw error("Informe produto e resultado desejado.");
    String encodedUrls = write(urls);
    var existing = modelings.findByRequestKey(request.requestKey().toString());
    if (existing.isPresent()) {
      var previous = existing.get();
      if (!Objects.equals(previous.getProductId(), request.productId())
          || !previous.getProductName().equals(name)
          || !previous.getDesiredOutcome().equals(outcome)
          || !previous.getReferencesJson().equals(encodedUrls))
        throw error("Esta chave já foi usada em outra preparação.");
      return view(previous);
    }
    ProductModeling modeling = new ProductModeling();
    modeling.setRequestKey(request.requestKey().toString());
    modeling.setProductId(request.productId());
    modeling.setProductName(name);
    modeling.setDesiredOutcome(outcome);
    modeling.setReferencesJson(encodedUrls);
    modeling.setBigIdeaSnapshot(
        product == null || product.getBigIdea() == null
            ? null
            : product.getBigIdea().getContentJson());
    modeling.setCreatedAt(Instant.now());
    modelings.saveAndFlush(modeling);
    var research =
        discovery.prepareCycle(
            new CreateProductDiscoveryCycleRequest(
                "Modelagem · " + name.substring(0, Math.min(178, name.length())),
                product == null ? null : product.getTargetAudience(),
                "BR",
                "pt-BR",
                "Instagram",
                "Preparação sem campanha, publicação ou autorização financeira herdada.",
                "Não copiar marcas, identidade, materiais ou código de terceiros.",
                "MODEL_REFERENCES_V1 #" + modeling.getId() + ": " + outcome,
                ProductDiscoveryResearchMode.VALIDATE_MARKET,
                ProductDiscoveryMarketType.UNSPECIFIED,
                String.join("\n", urls)));
    modeling.setDiscoveryCycleId(research.id());
    return view(modelings.save(modeling));
  }

  /** Abre a fila existente somente após comando explícito; replay não repete a pesquisa. */
  @Transactional
  public ModelingView start(Long id) {
    var modeling =
        modelings.findLockedById(id).orElseThrow(() -> error("Modelagem não encontrada."));
    if (modeling.getProductId() != null
        && !Boolean.TRUE.equals(
            products
                .findById(modeling.getProductId())
                .orElseThrow()
                .getAutomaticExecutionEnabled())) throw error("O produto está em STOP.");
    discovery.startPreparedCycle(modeling.getDiscoveryCycleId());
    return view(modeling);
  }

  /** Adota fontes na preparação exata sem disparar tarefas ou modificar orçamento. */
  @Transactional
  public ModelingView adopt(Long id, Long cycleId) {
    adoption.adopt(id, cycleId);
    return view(required(id));
  }

  /** Projeta a fila real; criação, fontes e proposta nunca são apresentadas como vendas. */
  private ModelingView view(ProductModeling m) {
    var cycle =
        m.getDiscoveryCycleId() == null
            ? null
            : cycles.findById(m.getDiscoveryCycleId()).orElse(null);
    String status = cycle == null ? "DRAFT" : cycle.getStatus().name();
    String guidance =
        switch (status) {
          case "DRAFT" ->
              "Preparação salva. Pesquisa ainda não iniciada; confira os limites antes de executar.";
          case "READY_FOR_RESEARCH", "RESEARCHING" ->
              "Argos investiga as referências. Fatos e limitações seguem para Atena pela cadeia existente.";
          case "COMPLETED" ->
              m.getLearningCycleId() != null
                  ? "Fontes incorporadas à preparação. A cadeia existente coordena planejamento, construção, revisões e teste conforme os aceites e limites."
                  : m.getProductId() != null
                      ? "Pesquisa pronta. Vincule a uma preparação ainda não executada para Atena receber as fontes. Experimentos históricos permanecem preservados."
                      : "Consulte o dossiê e as passagens para Atena, Plutus e Dédalo. Pesquisa concluída não comprova produto entregue nem demanda.";
          case "FAILED" ->
              "Pesquisa interrompida. Confira o diagnóstico preservado antes de retentar; não houve aprovação automática.";
          default -> "O dossiê possui pendências. Consulte a pesquisa antes de continuar a cadeia.";
        };
    return new ModelingView(
        m.getId(),
        m.getProductId(),
        m.getProductName(),
        m.getDesiredOutcome(),
        json.convertValue(
            read(m.getReferencesJson()),
            new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {}),
        m.getBigIdeaSnapshot() == null ? null : read(m.getBigIdeaSnapshot()),
        m.getDiscoveryCycleId(),
        status,
        guidance,
        m.getDiscoveryCycleId() == null
            ? null
            : "/product-discovery/cycles/" + m.getDiscoveryCycleId(),
        m.getProductId() == null ? null : "/products/" + m.getProductId() + "/big-idea",
        m.getCreatedAt(),
        stages(m),
        m.getLearningCycleId(),
        adoption.url(m),
        adoption.options(m));
  }

  /** Relaciona as tarefas reais da referência, sem confundir conclusão técnica com venda. */
  private List<ModelingStageView> stages(ProductModeling m) {
    if (m.getDiscoveryCycleId() == null) return List.of();
    var latest = new LinkedHashMap<String, com.marketinghub.agenttask.AgentTask>();
    for (var task :
        tasks.findBySourceReferenceOrderByCreatedAtAscIdAsc(
            handoff.sourceReference(m.getDiscoveryCycleId()))) {
      String key = task.getProcessActivityId() + ":" + task.getAssignedAgent().getAgentKey();
      latest.put(key, task);
    }
    return latest.values().stream()
        .map(
            task ->
                new ModelingStageView(
                    task.getId(),
                    task.getAssignedAgent().getAgentKey(),
                    task.getProcessActivityId(),
                    task.getStatus(),
                    task.getResultJson() == null
                        ? null
                        : read(task.getResultJson()).path("summary").asText(null),
                    m.getProductId() == null || task.getProcessDefinition() == null
                        ? null
                        : "/products/"
                            + m.getProductId()
                            + "/value-chain-history/processes/"
                            + task.getProcessDefinition().getId()
                            + "/activities?sourceReference="
                            + java.net.URLEncoder.encode(
                                task.getSourceReference(), java.nio.charset.StandardCharsets.UTF_8)
                            + adoption.activityQuery(m)
                            + "#activity-"
                            + task.getProcessActivityId()))
        .toList();
  }

  /** Aceita URLs públicas HTTPS sem credenciais; a coleta mantém sua proteção contra SSRF. */
  static List<String> validateUrls(List<String> values) {
    if (values == null || values.isEmpty() || values.size() > 5)
      throw error("Informe de uma a cinco referências.");
    var result = new LinkedHashSet<String>();
    for (String value : values) {
      try {
        URI uri = URI.create(value.trim());
        String host = uri.getHost();
        if (!"https".equalsIgnoreCase(uri.getScheme())
            || host == null
            || uri.getUserInfo() != null
            || uri.getPort() != -1
            || !host.contains(".")
            || host.matches("[0-9.]+")
            || host.contains(":")
            || host.endsWith(".local")
            || host.endsWith(".localhost")
            || uri.getRawQuery() != null
            || uri.getFragment() != null
            || value.length() > 1000)
          throw error("Use URLs públicas HTTPS sem parâmetros, credenciais ou portas.");
        result.add(uri.normalize().toString());
      } catch (IllegalArgumentException ex) {
        log.warn(
            "Referência de modelagem malformada; conteúdo omitido, tipo={}",
            ex.getClass().getSimpleName());
        throw error("Referência inválida. Use a página pública do produto.");
      }
    }
    return List.copyOf(result);
  }

  /** Obtém somente o registro solicitado. */
  private ProductModeling required(Long id) {
    return modelings.findById(id).orElseThrow(() -> error("Modelagem não encontrada."));
  }

  /** Serializa dados internos auditáveis sem perder a falha de contrato. */
  private String write(Object value) {
    try {
      return json.writeValueAsString(value);
    } catch (Exception ex) {
      log.error("Falha ao serializar modelagem.", ex);
      throw new IllegalStateException("Contrato inválido.", ex);
    }
  }

  /** Lê dados persistidos e preserva erro em vez de inventar conteúdo. */
  private JsonNode read(String value) {
    try {
      return json.readTree(value);
    } catch (Exception ex) {
      log.error("Falha ao ler modelagem.", ex);
      throw new IllegalStateException("Contrato persistido inválido.", ex);
    }
  }

  /** Expõe uma inconsistência acionável sem efeitos externos. */
  private static ResponseStatusException error(String text) {
    return new ResponseStatusException(HttpStatus.CONFLICT, text);
  }
}
