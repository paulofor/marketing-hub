package com.marketinghub.product.modeling.v1.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.product.modeling.v1.ProductModeling;
import com.marketinghub.product.modeling.v1.service.get.ModelingCycleOption;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository;
import com.marketinghub.repository.jpa.productdiscovery.ProductDiscoveryCycleRepository;
import com.marketinghub.repository.jpa.productmodeling.ProductModelingRepository;
import java.time.Instant;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

/** Vincula pesquisa a uma preparação ainda não executada, preservando a cadeia e seus gates. */
@Component
@RequiredArgsConstructor
public class ModelingAdoption {
  private static final java.util.regex.Pattern PUBLIC_HOST =
      java.util.regex.Pattern.compile(
          "^https?://(?:www\\.)?([a-z0-9.-]+)(?:[/?#].*)?$",
          java.util.regex.Pattern.CASE_INSENSITIVE);
  private final ProductModelingRepository models;
  private final LearningSalesCycleRepository cycles;
  private final ProductDiscoveryCycleRepository discovery;
  private final AgentTaskRepository tasks;
  private final ExperimentRepository experiments;
  private final ModelingHandoff handoff;
  private final com.marketinghub.repository.jpa.product.ProductRepository products;
  private final com.marketinghub.repository.jpa.processautomation.ProcessRunRepository runs;

  /** Lista ciclos próprios ainda sem execução quando as fontes permitem iniciar a estratégia. */
  public List<ModelingCycleOption> options(ProductModeling m) {
    if (m.getProductId() == null || m.getLearningCycleId() != null || !readyForStrategy(m))
      return List.of();
    return cycles.findByProductIdAndOpenSlot(m.getProductId(), 1).stream()
        .filter(c -> eligible(c) && models.findByLearningCycleId(c.getId()).isEmpty())
        .map(
            c ->
                new ModelingCycleOption(
                    c.getId(), c.getExperimentId(), c.getProductVersion(), url(c)))
        .toList();
  }

  /** Congela fatos e pendências para a estratégia sem promover maturidade ou autorizar mercado. */
  @Transactional
  public void adopt(Long id, Long cycleId) {
    var identity = models.findById(id).orElseThrow();
    require(identity.getProductId() != null, "A pesquisa ainda não possui produto existente.");
    var product = products.findLockedById(identity.getProductId()).orElseThrow();
    var m = models.findLockedById(id).orElseThrow();
    if (Objects.equals(m.getLearningCycleId(), cycleId)) return;
    require(
        m.getLearningCycleId() == null && m.getProductId() != null,
        "Esta modelagem não pode ser vinculada a outro ciclo.");
    require(
        Objects.equals(
            m.getBigIdeaSnapshot(),
            product.getBigIdea() == null ? null : product.getBigIdea().getContentJson()),
        "A Big Idea evoluiu após a preparação. Prepare uma modelagem com a direção atual antes de adotar.");
    var c =
        cycles
            .findLocked(m.getProductId(), cycleId)
            .orElseThrow(() -> error("Ciclo de outro produto ou inexistente."));
    require(
        eligible(c),
        "Use uma preparação ainda não executada; o histórico não pode ser substituído.");
    require(
        models.findByLearningCycleId(cycleId).isEmpty(),
        "O ciclo já possui uma modelagem adotada.");
    var research = discovery.findByIdForUpdate(m.getDiscoveryCycleId()).orElseThrow();
    require(
        research.getStatus()
            == com.marketinghub.productdiscovery.v1.ProductDiscoveryCycleStatus.COMPLETED,
        "A pesquisa factual precisa estar concluída.");
    var snapshot = handoff.context(m.getDiscoveryCycleId());
    require(
        snapshot.path("factualSources").isArray() && !snapshot.path("factualSources").isEmpty(),
        "A pesquisa não possui fontes factuais persistidas.");
    String basis = planningBasis(m, snapshot);
    require(
        basis != null,
        "Argos ainda não produziu estudo factual apto ao planejamento; confira referências, contrapontos e limites.");
    ObjectNode frozen = (ObjectNode) snapshot.deepCopy();
    if ("REFERENCE_STUDY_FOR_STRATEGY".equals(basis)) {
      frozen.put("adoptionBasis", basis);
      frozen.put("commercialValidationRequired", true);
      frozen.put("opportunityReadinessPreserved", true);
      frozen.put(
          "guidance",
          frozen.path("guidance").asText()
              + " Estudo factual de referências apto somente à estratégia. As candidatas conservam maturidade e lacunas; preço brasileiro, diferenciação, economia e demanda seguem como hipóteses a trabalhar na cadeia. Não declara validação comercial nem dispensa revisões ou autorização de mercado.");
    }
    m.setLearningCycleId(cycleId);
    m.setExperimentId(c.getExperimentId());
    m.setAdoptedContextJson(frozen.toString());
    m.setAdoptedAt(Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS));
    models.save(m);
  }

  /** Consulta prontidão da pesquisa sem abrir tarefa, alterar parecer ou herdar orçamento. */
  public boolean readyForStrategy(ProductModeling m) {
    if (m.getDiscoveryCycleId() == null) return false;
    return discovery
        .findById(m.getDiscoveryCycleId())
        .filter(
            c ->
                c.getStatus()
                    == com.marketinghub.productdiscovery.v1.ProductDiscoveryCycleStatus.COMPLETED)
        .map(c -> planningBasis(m, handoff.context(m.getDiscoveryCycleId())) != null)
        .orElse(false);
  }

  /**
   * Distingue prontidão da oportunidade e estudo internacional suficiente para formular estratégia.
   */
  private String planningBasis(ProductModeling m, JsonNode snapshot) {
    if (!snapshot.path("factualSources").isArray() || snapshot.path("factualSources").isEmpty())
      return null;
    for (var candidate : snapshot.path("candidates"))
      if ("DOSSIER_READY".equals(candidate.path("maturity").asText()))
        return "OPPORTUNITY_DOSSIER_READY";
    if (!"INTERNATIONAL_TO_BR_V1".equals(m.getResearchScope())
        || m.getProductId() == null
        || snapshot.path("modelingId").asLong() != m.getId()
        || snapshot.path("productId").asLong() != m.getProductId()
        || snapshot.path("discoveryCycleId").asLong() != m.getDiscoveryCycleId()
        || !snapshot.path("referenceUrls").isArray()
        || snapshot.path("referenceUrls").isEmpty()
        || !snapshot.path("candidates").isArray()
        || snapshot.path("candidates").isEmpty()) return null;
    Set<String> references = new HashSet<>();
    for (var reference : snapshot.path("referenceUrls")) {
      String domain = sourceDomain(reference.asText());
      if (domain.isEmpty()) return null;
      references.add(domain);
    }
    for (var candidate : snapshot.path("candidates"))
      if (!Set.of("SIGNAL", "RESEARCHABLE").contains(candidate.path("maturity").asText())
          || !nonEmptyText(candidate.path("limitations"))
          || !nonEmptyText(candidate.path("mainPain"))) return null;
    Map<String, List<String>> counterpointClaims = counterpointClaims(snapshot);
    Set<String> covered = new HashSet<>();
    boolean counterpoint = false;
    for (var fact : snapshot.path("factualSources")) {
      if (fact.path("evidenceId").asLong() <= 0
          || fact.path("dossierId").asLong() <= 0
          || !nonEmptyText(fact.path("observation"))
          || !nonEmptyText(fact.path("recordedAt"))) continue;
      String domain = sourceDomain(fact.path("url").asText());
      if (domain.isEmpty()) continue;
      boolean primary = false;
      for (String reference : references)
        if (domain.equals(reference) || domain.endsWith("." + reference)) {
          covered.add(reference);
          primary = true;
        }
      counterpoint |=
          !primary
              && counterpointClaims.getOrDefault(fact.path("url").asText(), List.of()).stream()
                  .anyMatch(excerpt -> fact.path("observation").asText().contains(excerpt));
    }
    return counterpoint && covered.containsAll(references) ? "REFERENCE_STUDY_FOR_STRATEGY" : null;
  }

  /** Exige contraponto atribuído e trecho verificável na mesma fonte persistida. */
  private Map<String, List<String>> counterpointClaims(JsonNode snapshot) {
    Map<String, List<String>> claims = new HashMap<>();
    for (var candidate : snapshot.path("candidates"))
      for (var observation : candidate.path("publicEvidenceAssessment").path("observations")) {
        if (!Set.of("PUBLIC_CUSTOMER_REPORT", "EDITORIAL", "SCIENTIFIC")
                .contains(observation.path("sourceRole").asText())
            || !nonEmptyText(observation.path("url"))
            || !nonEmptyText(observation.path("supportingExcerpt"))
            || observation.path("supportingExcerpt").asText().length() < 15
            || !nonEmptyText(observation.path("limitation"))
            || !observation.path("sourceSha256").asText().matches("[a-fA-F0-9]{64}")) continue;
        claims
            .computeIfAbsent(observation.path("url").asText(), ignored -> new ArrayList<>())
            .add(observation.path("supportingExcerpt").asText());
      }
    return claims;
  }

  /** Rejeita null e tipos não textuais sem convertê-los em uma informação preenchida. */
  private boolean nonEmptyText(JsonNode value) {
    return value.isTextual() && StringUtils.hasText(value.textValue());
  }

  /** Compara domínio público com limite de subdomínio, ignorando URLs malformadas ou locais. */
  private String sourceDomain(String url) {
    var matcher = PUBLIC_HOST.matcher(url.trim());
    if (!matcher.matches()) return "";
    String host = matcher.group(1).toLowerCase(Locale.ROOT);
    return host.contains(".")
            && !host.matches("[0-9.]+")
            && !host.endsWith(".local")
            && !host.endsWith(".localhost")
        ? host
        : "";
  }

  /** Aponta à operação canônica; nenhuma tarefa ou campanha é criada pela vinculação. */
  public String url(ProductModeling m) {
    return m.getLearningCycleId() == null
        ? null
        : cycles.findById(m.getLearningCycleId()).map(this::url).orElse(null);
  }

  /** Mantém cadeia e ciclo nos links de atividade, sem selecionar a versão mais recente. */
  public String activityQuery(ProductModeling m) {
    return m.getLearningCycleId() == null
        ? ""
        : cycles
            .findById(m.getLearningCycleId())
            .map(c -> "&chainId=" + c.getChainDefinitionId() + "&learningCycleId=" + c.getId())
            .orElse("");
  }

  /** Recusa ciclo encerrado, exposto ou cujo planejamento já recebeu outro contexto. */
  private boolean eligible(
      com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle c) {
    if (!"OPEN".equals(c.getStatus())
        || c.isBaseline()
        || !Set.of("LEARNING", "PLANNING", "ADJUSTMENT").contains(c.getStage())) return false;
    var experiment = experiments.findById(c.getExperimentId()).orElse(null);
    return experiment != null
        && experiment.getStatus() == com.marketinghub.experiment.ExperimentStatus.PLANNED
        && !runs.existsByProductIdAndSourceReference(
            c.getProductId(), "experiment:" + c.getExperimentId())
        && tasks
            .findBySourceReferenceOrderByCreatedAtAscIdAsc("experiment:" + c.getExperimentId())
            .isEmpty();
  }

  /** Compõe a navegação com todas as identidades da ocorrência, sem selecionar outra versão. */
  private String url(com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle c) {
    return "/business-process-chains/learning-cycles?productId="
        + c.getProductId()
        + "&chainId="
        + c.getChainDefinitionId()
        + "&cycleId="
        + c.getId();
  }

  /** Converte pré-condição violada em bloqueio acionável sem efeito parcial. */
  private static void require(boolean value, String message) {
    if (!value) throw error(message);
  }

  /** Uniformiza erros funcionais da adoção. */
  private static ResponseStatusException error(String message) {
    return new ResponseStatusException(HttpStatus.CONFLICT, message);
  }
}
