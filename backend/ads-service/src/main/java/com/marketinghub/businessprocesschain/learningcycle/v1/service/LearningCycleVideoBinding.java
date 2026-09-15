package com.marketinghub.businessprocesschain.learningcycle.v1.service;

import static com.marketinghub.businessprocesschain.learningcycle.v1.service.LearningCycleRules.require;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.agenttask.AgentTask;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycleEvent;
import com.marketinghub.experiment.video.ExperimentVideoAsset;
import com.marketinghub.experiment.video.ExperimentVideoSlot;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleEventRepository;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Responsabilidade: vincular mídias aprovadas à experiência privada sem conceder gates comerciais.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LearningCycleVideoBinding {
  public static final String CONTRACT = "LEARNING_CYCLE_VIDEO_INTEGRATION_V1";
  public static final List<String> REVIEWS =
      List.of(
          "technicalHomologation",
          "psiqueAdherent",
          "psiqueRecovery",
          "psiqueSafety",
          "commercialIntegrityReview");
  private final LearningSalesCycleRepository cycles;
  private final LearningSalesCycleEventRepository events;
  private final LearningCycleVideoEvidence videos;
  private final LearningCyclePrototypeContext prototypes;
  private final ObjectMapper json;

  /** Reconhece o destino privado cujo frontend e harness implementam o contrato de integração. */
  public boolean supports(LearningSalesCycle cycle) {
    return !cycle.isBaseline()
        && prototypes
            .resolve(cycle)
            .map(
                target ->
                    target.path("privateAccessUrl").asText().matches("https://[^/]+/vega-private"))
            .orElse(false);
  }

  /** Identifica a única decisão humana necessária para as peças atuais. */
  public boolean awaitingApproval(LearningSalesCycle cycle) {
    return videos.awaitingApproval(cycle);
  }

  /** Monta somente vínculos comprovados e preserva o escopo privado para a homologação seguinte. */
  public ObjectNode prepare(LearningSalesCycle cycle) {
    require(
        supports(cycle),
        "O destino privado ainda não implementa o contrato de integração audiovisual.");
    var target =
        prototypes
            .resolve(cycle)
            .orElseThrow(
                () -> new IllegalStateException("O destino privado desta versão não foi aceito."));
    var ad = videos.approved(cycle, ExperimentVideoSlot.AD);
    var hero = videos.approved(cycle, ExperimentVideoSlot.LANDING_HERO);
    require(
        !Objects.equals(ad.getAssetUrl(), hero.getAssetUrl()),
        "Anúncio e demonstração precisam conservar suas peças distintas.");
    var proof = json.createObjectNode();
    proof.put("evidenceType", CONTRACT);
    proof.put("mode", "PRIVATE_PDE");
    proof.put("cycleId", cycle.getId());
    proof.put("productId", cycle.getProductId());
    proof.put("experimentId", cycle.getExperimentId());
    proof.put("chainDefinitionId", cycle.getChainDefinitionId());
    proof.put("productVersion", cycle.getProductVersion());
    proof.put("destinationUrl", target.path("privateAccessUrl").asText());
    proof.set("campaignVideo", media(ad));
    proof.set("heroVideo", media(hero));
    proof.put("paymentEnabled", false);
    proof.put("publicationAuthorized", false);
    proof.put("campaignAuthorized", false);
    proof.put("mediaSpendAuthorized", false);
    proof.put("technicalValidationRequired", true);
    proof.put("humanApprovalReused", true);
    proof.put("integrationFingerprint", hash(proof.toString()));
    return proof;
  }

  /** Localiza somente o recibo da versão vigente, preservando as conclusões históricas. */
  public Optional<LearningSalesCycleEvent> receipt(LearningSalesCycle cycle) {
    return events.findByCycleIdOrderByRevisionAsc(cycle.getId()).stream()
        .filter(e -> "VIDEO_APPROVAL".equals(e.getFromStage()) && "COMPLETE".equals(e.getAction()))
        .filter(e -> !e.getCreatedAt().isBefore(cycle.getVersionChangedAt()))
        .filter(e -> CONTRACT.equals(read(e.getEvidenceJson()).path("evidenceType").asText()))
        .reduce((a, b) -> b)
        .filter(
            e ->
                cycle
                    .getProductVersion()
                    .equals(read(e.getEvidenceJson()).path("productVersion").asText()));
  }

  /** Revalida aprovações e identidade antes de expor ou consumir um vínculo de vídeo. */
  public ObjectNode current(LearningSalesCycle cycle) {
    var stored =
        receipt(cycle)
            .orElseThrow(
                () -> new IllegalStateException("Integração audiovisual ainda não registrada."));
    var proof = prepare(cycle);
    require(
        read(proof.toString()).equals(read(stored.getEvidenceJson())),
        "A mídia, sua aprovação ou o destino mudou após a integração. A homologação anterior não pode ser reutilizada.");
    return proof;
  }

  /** Entrega o conjunto atual ao consumidor privado e ao harness por ciclo e versão explícitos. */
  public Optional<JsonNode> presentation(Long cycleId, String version) {
    var cycle = cycles.findById(cycleId).orElse(null);
    if (cycle == null
        || !Objects.equals(version, cycle.getProductVersion())
        || receipt(cycle).isEmpty()) return Optional.empty();
    return Optional.of(current(cycle));
  }

  /** Expõe o recibo ao contexto de tarefas da mesma referência sem procurar outro experimento. */
  public Optional<JsonNode> forReference(String reference) {
    if (reference == null || !reference.matches("experiment:[1-9][0-9]{0,17}"))
      return Optional.empty();
    return cycles
        .findByExperimentId(Long.parseLong(reference.substring(11)))
        .filter(c -> receipt(c).isPresent())
        .map(this::current);
  }

  /** Indica se a tarefa foi criada depois do conjunto integrado, sem aceitar prova retroativa. */
  public boolean currentTask(LearningSalesCycle cycle, AgentTask task) {
    return task != null
        && task.getCreatedAt() != null
        && receipt(cycle).map(e -> !task.getCreatedAt().isBefore(e.getCreatedAt())).orElse(false);
  }

  /** Exige no gate os testes do conjunto atual e todos os pareceres posteriores à integração. */
  public void validateReviews(String reference, List<AgentTask> reviewed) {
    if (reference == null || !reference.matches("experiment:[1-9][0-9]{0,17}")) return;
    var cycle = cycles.findByExperimentId(Long.parseLong(reference.substring(11))).orElse(null);
    if (cycle == null || receipt(cycle).isEmpty()) return;
    var proof = current(cycle);
    for (String code : REVIEWS) {
      var task =
          reviewed.stream()
              .filter(t -> code.equals(t.getProcessActivityId()))
              .findFirst()
              .orElse(null);
      require(
          currentTask(cycle, task),
          "A revisão " + code + " precisa avaliar o conjunto com os vídeos integrados.");
      if ("technicalHomologation".equals(code)) {
        var result = read(task.getResultJson());
        require(
            proof.path("integrationFingerprint").equals(result.path("videoIntegrationFingerprint")),
            "O harness precisa comprovar os mesmos vídeos e o destino integrados.");
        for (String check :
            List.of("videoIdentity", "videoPlayback", "videoOptional", "videoFailureRecovery"))
          require(
              result.path("checks").path(check).asBoolean(false),
              "A homologação audiovisual não comprovou " + check + ".");
      }
    }
  }

  /** Preserva somente os dados de mídia e a aprovação humana, sem prompts nem auditoria interna. */
  private ObjectNode media(ExperimentVideoAsset asset) {
    var metadata = read(asset.getResponseJson());
    String sha = metadata.path("hls_delivery").path("sourceSha256").asText();
    require(
        sha.matches("[0-9a-f]{64}"),
        "O vídeo #" + asset.getId() + " precisa do hash do arquivo final para integração segura.");
    require(
        asset.getReviewedAt() != null
            && asset.getReviewedBy() != null
            && !asset.getReviewedBy().isBlank(),
        "A aprovação do vídeo precisa preservar responsável e horário.");
    var result = json.createObjectNode();
    result.put("assetId", asset.getId());
    result.put("role", asset.getSlot().name());
    result.put("assetUrl", asset.getAssetUrl());
    result.put("hlsPlaybackUrl", asset.getHlsPlaybackUrl());
    result.put("thumbnailUrl", asset.getThumbnailUrl());
    result.put("durationSeconds", asset.getDurationSeconds());
    result.put("sha256", sha);
    result.put("captions", metadata.path("captions").path("text").asText());
    result.put("captionsBurnedIn", metadata.path("captions").path("burned_in").asBoolean());
    result.put("reviewStatus", asset.getReviewStatus().name());
    result.put("reviewedBy", asset.getReviewedBy());
    result.put("reviewedAt", asset.getReviewedAt().toString());
    if (asset.getSalesVideoJob() != null && asset.getSalesVideoJob().getVttAsset() != null)
      result.put("vttUrl", asset.getSalesVideoJob().getVttAsset().getUrl());
    require(
        result.path("captionsBurnedIn").asBoolean() && !result.path("captions").asText().isBlank(),
        "O vídeo precisa preservar suas legendas antes da integração.");
    return result;
  }

  /** Lê prova persistida sem converter falha de contrato em ausência silenciosa. */
  private JsonNode read(String text) {
    try {
      return json.readTree(text == null ? "{}" : text);
    } catch (Exception ex) {
      log.error("Falha ao ler evidência da integração audiovisual do ciclo", ex);
      throw new IllegalStateException("Evidência audiovisual ilegível.", ex);
    }
  }

  /** Identifica o conjunto sem alegar inspeção física dos bytes, tarefa do harness. */
  private String hash(String text) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));
    } catch (java.security.NoSuchAlgorithmException ex) {
      log.error("SHA-256 indisponível para integração audiovisual", ex);
      throw new IllegalStateException("Não foi possível identificar a integração.", ex);
    }
  }
}
