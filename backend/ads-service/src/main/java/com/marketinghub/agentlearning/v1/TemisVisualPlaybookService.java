package com.marketinghub.agentlearning.v1;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.planning.CommercialPlan;
import com.marketinghub.planning.CommercialPlanVisualAsset;
import com.marketinghub.planning.CommercialPlanVisualAssetStatus;
import com.marketinghub.planning.imagestudio.v1.service.TemisVisualPlaybookDto;
import com.marketinghub.planning.imagestudio.v1.service.TemisVisualPlaybookExampleDto;
import com.marketinghub.repository.jpa.planning.CommercialPlanVisualAssetRepository;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** Responsabilidade: resolver o playbook visual promovido e exemplos compatíveis para Têmis. */
@Service
public class TemisVisualPlaybookService {
  private static final Logger log = LoggerFactory.getLogger(TemisVisualPlaybookService.class);
  private static final String AGENT_KEY = "meta-ad-approver";
  private static final String SCOPE_TYPE = "VISUAL_CONTEXT";
  private static final String BASE_RESOURCE = "agent-learning/temis/visual-playbook-v1.json";
  private final GovernedAgentLearningService learningService;
  private final CommercialPlanVisualAssetRepository assetRepository;
  private final ObjectMapper objectMapper;
  private final BasePlaybook base;

  /** Inicializa o resolvedor com governança, biblioteca e contrato canônico versionado. */
  public TemisVisualPlaybookService(
      GovernedAgentLearningService learningService,
      CommercialPlanVisualAssetRepository assetRepository,
      ObjectMapper objectMapper) {
    this.learningService = learningService;
    this.assetRepository = assetRepository;
    this.objectMapper = objectMapper;
    this.base = loadBase();
  }

  /** Entrega somente baseline canônica ou estratégia promovida no mesmo contexto comercial. */
  @Transactional(readOnly = true)
  public TemisVisualPlaybookDto resolve(
      CommercialPlan plan, String label, List<String> purposes, String size) {
    String contextKey = contextKey(plan, label, purposes, size);
    List<LearningExperimentResponse> promoted =
        learningService.promoted(AGENT_KEY, SCOPE_TYPE, contextKey);
    List<String> rules = new ArrayList<>(base.rules());
    List<String> avoid = new ArrayList<>(base.avoid());
    String version = base.version();
    String status = "CANONICAL_BASELINE";
    if (!promoted.isEmpty()) {
      LearningExperimentResponse latest = promoted.getFirst();
      PromotedPlaybook learned = parsePromoted(latest.candidateResultJson());
      rules.addAll(learned.rules());
      avoid.addAll(learned.avoid());
      version = latest.candidateVersion();
      status = "PROMOTED";
    }
    return new TemisVisualPlaybookDto(
        version,
        contextKey,
        status,
        distinct(rules, 10),
        distinct(avoid, 10),
        approvedExamples(plan.getId(), label, purposes));
  }

  /** Gera a chave estável que impede misturar nicho, produto, finalidade, placement e formato. */
  public String contextKey(CommercialPlan plan, String label, List<String> purposes, String size) {
    String niche = plan.getNiche() == null ? "none" : String.valueOf(plan.getNiche().getId());
    String productType = "unknown";
    if (plan.getExperiment() != null && plan.getExperiment().getProduct() != null) {
      productType =
          firstText(
              plan.getExperiment().getProduct().getProductType(),
              plan.getExperiment().getProduct().getProductFormat(),
              plan.getExperiment().getProduct().getSlug(),
              "unknown");
    }
    String raw =
        "n="
            + normalize(niche)
            + "|p="
            + normalize(productType)
            + "|u="
            + purposes.stream()
                .map(this::normalize)
                .sorted()
                .reduce((a, b) -> a + "+" + b)
                .orElse("none")
            + "|pl="
            + placement(label, size)
            + "|f="
            + normalize(size);
    return raw.length() <= 120 ? raw : "visual-context-" + sha256(raw);
  }

  /** Seleciona dois exemplos premium, priorizando o formato pedido e diversidade post/story. */
  private List<TemisVisualPlaybookExampleDto> approvedExamples(
      Long planId, String label, List<String> purposes) {
    List<CommercialPlanVisualAsset> approved =
        assetRepository.findByCommercialPlanIdAndStatusOrderByCreatedAtAsc(
            planId, CommercialPlanVisualAssetStatus.APPROVED);
    String preferred = placement(label, "");
    LinkedHashSet<CommercialPlanVisualAsset> selected = new LinkedHashSet<>();
    approved.stream()
        .filter(asset -> preferred.equals(format(asset.getLabel())))
        .filter(asset -> compatiblePurpose(asset, purposes))
        .findFirst()
        .ifPresent(selected::add);
    approved.stream()
        .filter(asset -> !preferred.equals(format(asset.getLabel())))
        .filter(asset -> compatiblePurpose(asset, purposes))
        .findFirst()
        .ifPresent(selected::add);
    approved.stream()
        .filter(asset -> compatiblePurpose(asset, purposes))
        .filter(asset -> !selected.contains(asset))
        .limit(Math.max(0, 2 - selected.size()))
        .forEach(selected::add);
    return selected.stream().limit(2).map(this::example).toList();
  }

  /** Converte um asset aprovado em exemplo leve e rastreável. */
  private TemisVisualPlaybookExampleDto example(CommercialPlanVisualAsset asset) {
    return new TemisVisualPlaybookExampleDto(
        asset.getId(),
        asset.getLabel(),
        asset.getAssetUrl(),
        format(asset.getLabel()),
        readPurposes(asset));
  }

  /** Confirma compatibilidade de finalidade sem aceitar ativo de outro uso por inferência. */
  private boolean compatiblePurpose(CommercialPlanVisualAsset asset, List<String> requested) {
    List<String> available = readPurposes(asset);
    return requested.stream()
        .anyMatch(value -> available.stream().anyMatch(value::equalsIgnoreCase));
  }

  /** Lê finalidades estruturadas com compatibilidade para o campo singular legado. */
  private List<String> readPurposes(CommercialPlanVisualAsset asset) {
    try {
      if (StringUtils.hasText(asset.getPurposesJson())) {
        return objectMapper.readValue(
            asset.getPurposesJson(),
            objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
      }
      return StringUtils.hasText(asset.getPurpose()) ? List.of(asset.getPurpose()) : List.of();
    } catch (IOException ex) {
      log.error("Falha ao ler finalidades do exemplo visual. assetId={}", asset.getId(), ex);
      return List.of();
    }
  }

  /** Extrai regras de um resultado já promovido e ignora conteúdo estruturalmente inválido. */
  private PromotedPlaybook parsePromoted(String json) {
    if (!StringUtils.hasText(json)) return new PromotedPlaybook(List.of(), List.of());
    try {
      JsonNode root = objectMapper.readTree(json).path("playbook");
      return new PromotedPlaybook(strings(root.path("rules")), strings(root.path("avoid")));
    } catch (IOException ex) {
      log.error("Estratégia promovida de Têmis contém JSON inválido", ex);
      throw new IllegalStateException("Playbook promovido de Têmis é inválido", ex);
    }
  }

  /** Lê uma lista textual de um nó JSON sem propagar itens vazios. */
  private List<String> strings(JsonNode node) {
    if (!node.isArray()) return List.of();
    List<String> values = new ArrayList<>();
    node.forEach(
        item -> {
          if (StringUtils.hasText(item.asText())) values.add(item.asText().trim());
        });
    return values;
  }

  /** Carrega integralmente o playbook base versionado fora do código Java. */
  private BasePlaybook loadBase() {
    try (var input = new ClassPathResource(BASE_RESOURCE).getInputStream()) {
      return objectMapper.readValue(input, BasePlaybook.class);
    } catch (IOException ex) {
      log.error("Falha ao carregar playbook visual canônico. resource={}", BASE_RESOURCE, ex);
      throw new IllegalStateException("Playbook visual canônico indisponível", ex);
    }
  }

  /** Deduplica regras preservando a ordem e limitando o contexto operacional. */
  private List<String> distinct(List<String> values, int limit) {
    return values.stream()
        .filter(StringUtils::hasText)
        .map(String::trim)
        .distinct()
        .limit(limit)
        .toList();
  }

  /** Classifica o placement pelo rótulo e dimensão declarados. */
  private String placement(String label, String size) {
    String normalized = normalize(Objects.toString(label, "") + " " + Objects.toString(size, ""));
    if (normalized.contains("story") || normalized.contains("1152x2048")) return "STORY";
    if (normalized.contains("post")
        || normalized.contains("1024x1024")
        || normalized.contains("2048x2048")) return "FEED";
    return "GENERAL";
  }

  /** Classifica um exemplo sem baixar novamente seu arquivo. */
  private String format(String label) {
    return placement(label, "");
  }

  /** Normaliza texto para uma chave contextual estável. */
  private String normalize(String value) {
    String normalized = Objects.toString(value, "").trim().toLowerCase(Locale.ROOT);
    normalized =
        java.text.Normalizer.normalize(normalized, java.text.Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "");
    return normalized.replaceAll("[^a-z0-9_-]+", "-").replaceAll("(^-|-$)", "");
  }

  /** Escolhe o primeiro atributo de produto preenchido. */
  private String firstText(String... values) {
    for (String value : values) if (StringUtils.hasText(value)) return value;
    return "unknown";
  }

  /** Calcula uma chave curta quando o contexto textual excede o contrato persistido. */
  private String sha256(String value) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("SHA-256 indisponível", ex);
    }
  }

  /** Contrato do playbook base versionado. */
  private record BasePlaybook(String version, List<String> rules, List<String> avoid) {}

  /** Parte operacional permitida de uma estratégia promovida. */
  private record PromotedPlaybook(List<String> rules, List<String> avoid) {}
}
