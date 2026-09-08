package com.marketinghub.businessprocesschain.learningcycle.v1.service;

import static com.marketinghub.businessprocesschain.learningcycle.v1.service.LearningCycleRules.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.getCycles.LearningCycleResponse.ApprovalOption;
import com.marketinghub.creative.*;
import com.marketinghub.experiment.video.*;
import com.marketinghub.repository.jpa.creative.CreativeRepository;
import com.marketinghub.repository.jpa.experiment.video.ExperimentVideoAssetRepository;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleEventRepository;
import com.marketinghub.repository.jpa.pde.PdeProductionSlotRepository;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Responsabilidade: conferir a identidade e a elegibilidade dos dois vídeos e de sua integração.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class LearningCycleVideoEvidence {
  private final ExperimentVideoAssetRepository videos;
  private final CreativeRepository creatives;
  private final PdeProductionSlotRepository slots;
  private final LearningSalesCycleEventRepository events;
  private final LearningCycleJson json;

  /** Valida o briefing comercial sem convertê-lo em autorização financeira ou job. */
  public void brief(JsonNode data) {
    for (String field :
        List.of(
            "briefReference",
            "campaignGoal",
            "campaignCta",
            "campaignMetric",
            "pdeGoal",
            "pdeCta",
            "pdeMetric",
            "controlledVariables",
            "productionBudgetReference")) text(data, field);
  }

  /** Registra a identidade lida do ativo pronto; a aprovação independente será exigida no gate. */
  public void production(LearningSalesCycle cycle, JsonNode data, ExperimentVideoSlot role) {
    String field = role == ExperimentVideoSlot.AD ? "campaignVideoAssetId" : "pdeVideoAssetId";
    var video = video(cycle, id(data, field), role, false);
    text(data, "productionEvidence");
    ((ObjectNode) data).put("assetFingerprint", fingerprint(video));
  }

  /** Confere revisão, anúncio e contrato em rascunho antes da homologação do conjunto. */
  public void integration(LearningSalesCycle cycle, JsonNode data) {
    for (String field : List.of("technicalEvidence", "customerReviewEvidence")) text(data, field);
    for (String field :
        List.of(
            "captionsVerified", "mobileVerified", "optionalPlaybackVerified", "testDataExcluded"))
      require(
          data.path(field).isBoolean() && data.path(field).asBoolean(),
          "Confirme a validação audiovisual: " + field + ".");
    ((ObjectNode) data).put("integrationFingerprint", integrationFingerprint(cycle, data, false));
  }

  /**
   * Revalida mídia e integração atuais, inclusive contra o contrato publicado quando necessário.
   */
  public void current(LearningSalesCycle cycle, boolean published) {
    JsonNode proof = completed(cycle, "VIDEO_APPROVAL");
    require(
        proof
            .path("integrationFingerprint")
            .asText()
            .equals(integrationFingerprint(cycle, proof, published)),
        "Vídeo, criativo ou contrato mudou após a revisão. Devolva para correção e homologue a versão atual.");
  }

  /** Expõe a causa concreta do bloqueio sem executar rede, geração ou aprovação. */
  public String blocker(LearningSalesCycle cycle, boolean published) {
    try {
      current(cycle, published);
      return null;
    } catch (ResponseStatusException ex) {
      log.debug(
          "Ciclos: revisão audiovisual bloqueada cycleId={} experimentId={}",
          cycle.getId(),
          cycle.getExperimentId(),
          ex);
      return ex.getReason();
    }
  }

  /**
   * Oferece apenas registros do produto e experimento selecionados, sem inferência na interface.
   */
  public Map<String, List<ApprovalOption>> options(LearningSalesCycle cycle) {
    if (!Set.of("CAMPAIGN_VIDEO", "PDE_ENTRY_VIDEO", "VIDEO_APPROVAL").contains(cycle.getStage()))
      return Map.of();
    if (!"VIDEO_APPROVAL".equals(cycle.getStage())) {
      var role =
          "CAMPAIGN_VIDEO".equals(cycle.getStage())
              ? ExperimentVideoSlot.AD
              : ExperimentVideoSlot.LANDING_HERO;
      String field = role == ExperimentVideoSlot.AD ? "campaignVideoAssetId" : "pdeVideoAssetId";
      return Map.of(
          field,
          videos.findByExperimentIdOrderByCreatedAtDesc(cycle.getExperimentId()).stream()
              .filter(
                  v ->
                      scoped(v, cycle)
                          && v.getSlot() == role
                          && v.getStatus() == ExperimentVideoStatus.READY)
              .map(
                  v ->
                      new ApprovalOption(
                          v.getId(),
                          "Vídeo #" + v.getId() + " · " + role + " · " + v.getObjective()))
              .toList());
    }
    var ad =
        selected(cycle, "CAMPAIGN_VIDEO", "campaignVideoAssetId", ExperimentVideoSlot.AD, false);
    var product = ad.getExperiment().getProduct();
    return Map.of(
        "creativeId",
            creatives
                .findByExperimentIdAndVideoUrl(cycle.getExperimentId(), ad.getAssetUrl())
                .stream()
                .filter(
                    c ->
                        c.getStatus() == CreativeStatus.READY
                            && c.getAgentReviewStatus() == CreativeAgentReviewStatus.APPROVED)
                .map(
                    c ->
                        new ApprovalOption(
                            c.getId(), "Criativo #" + c.getId() + " · " + c.getHeadline()))
                .toList(),
        "pdeSlotId",
            slots.findByProductSlugOrderBySlotCodeAsc(product.getSlug()).stream()
                .filter(
                    s ->
                        cycle.getProductVersion().equals(s.getExperienceVersion())
                            && cycle.getExperimentId().equals(s.getSourceExperimentId()))
                .map(
                    s ->
                        new ApprovalOption(
                            s.getId(), s.getSlotCode() + " · " + s.getExperienceVersion()))
                .toList());
  }

  /** Confere ativos, revisões e correspondência exata entre anúncio, destino e versão PDE. */
  private String integrationFingerprint(
      LearningSalesCycle cycle, JsonNode data, boolean published) {
    var ad =
        selected(cycle, "CAMPAIGN_VIDEO", "campaignVideoAssetId", ExperimentVideoSlot.AD, true);
    var hero =
        selected(
            cycle, "PDE_ENTRY_VIDEO", "pdeVideoAssetId", ExperimentVideoSlot.LANDING_HERO, true);
    require(
        !ad.getId().equals(hero.getId()) && !Objects.equals(ad.getAssetUrl(), hero.getAssetUrl()),
        "Campanha e entrada exigem peças distintas com finalidade própria.");
    var creative = creatives.findById(id(data, "creativeId")).orElse(null);
    require(
        creative != null
            && creative.getExperiment() != null
            && cycle.getExperimentId().equals(creative.getExperiment().getId())
            && creative.getStatus() == CreativeStatus.READY
            && creative.getAgentReviewStatus() == CreativeAgentReviewStatus.APPROVED
            && Objects.equals(ad.getAssetUrl(), creative.getVideoUrl()),
        "Crie o anúncio a partir do AD e conclua a revisão independente e a aprovação do criativo deste experimento.");
    var slot = slots.findById(id(data, "pdeSlotId")).orElse(null);
    require(
        slot != null
            && Objects.equals(ad.getExperiment().getProduct().getSlug(), slot.getProductSlug())
            && cycle.getExperimentId().equals(slot.getSourceExperimentId())
            && cycle.getProductVersion().equals(slot.getExperienceVersion()),
        "Selecione uma versão PDE do mesmo produto, experimento e versão do ciclo.");
    require(
        https(slot.getPublicUrl())
            && Objects.equals(slot.getPublicUrl(), creative.getDestinationUrl())
            && Objects.equals(slot.getPublicUrl(), ad.getExperiment().getFollowUpActionUrl()),
        "O destino do criativo e do experimento deve ser a URL exata da versão PDE selecionada.");
    String contractText =
        published ? slot.getPublishedExperienceJson() : slot.getDraftExperienceJson();
    require(
        contractText != null && !contractText.isBlank(),
        "Prepare o contrato "
            + (published ? "publicado" : "em rascunho")
            + " da versão PDE selecionada.");
    JsonNode contract = effectiveContract(contractText, slot);
    boolean linked = false;
    for (JsonNode item : contract.path("heroVideos"))
      if (item.path("experimentVideoAssetId").asLong(-1) == hero.getId()
          && hero.getHlsPlaybackUrl().equals(item.path("hlsPlaybackUrl").asText())
          && "READY".equals(item.path("status").asText())
          && "APPROVED".equals(item.path("reviewStatus").asText())) linked = true;
    require(
        linked,
        published
            ? "O contrato publicado ainda não contém o vídeo de entrada revisado. Publique pelo fluxo oficial e confira novamente."
            : "Inclua o LANDING_HERO aprovado em heroVideos no rascunho da versão PDE antes de homologar.");
    return hash(
        json.write(
            List.of(
                fingerprint(ad),
                fingerprint(hero),
                creative.getId(),
                safe(creative.getHeadline()),
                safe(creative.getPrimaryText()),
                safe(creative.getDescription()),
                safe(creative.getCta()),
                safe(creative.getReviewedAt()),
                safe(creative.getAgentReviewedAt()),
                slot.getId(),
                slot.getPublicUrl(),
                slot.getExperienceVersion(),
                canonical(contract))));
  }

  /**
   * Compara a representação que o publicador canônico entrega, sem ocultar identidade divergente.
   */
  private JsonNode effectiveContract(String content, com.marketinghub.pde.PdeProductionSlot slot) {
    JsonNode parsed;
    try {
      parsed = json.read(content);
    } catch (IllegalStateException ex) {
      log.warn(
          "Ciclos: contrato PDE ilegível slotId={} experimentId={}",
          slot.getId(),
          slot.getSourceExperimentId(),
          ex);
      throw new ResponseStatusException(
          org.springframework.http.HttpStatus.CONFLICT,
          "Corrija o JSON do contrato na versão PDE selecionada antes de continuar.",
          ex);
    }
    require(
        parsed != null && parsed.isObject(), "O contrato PDE precisa ser um objeto estruturado.");
    ObjectNode contract = (ObjectNode) parsed;
    for (var entry :
        Map.of(
                "experienceVersion",
                safe(slot.getExperienceVersion()),
                "layoutKey",
                safe(slot.getLayoutKey()))
            .entrySet()) {
      if (entry.getValue().isBlank()) continue;
      require(
          !contract.has(entry.getKey())
              || entry.getValue().equals(contract.path(entry.getKey()).asText()),
          "A identidade "
              + entry.getKey()
              + " do contrato diverge da versão PDE selecionada. Corrija o rascunho.");
      contract.put(entry.getKey(), entry.getValue());
    }
    return contract;
  }

  /** Exige a mesma mídia registrada nesta revisão, sem reaproveitar seleção anterior à correção. */
  private ExperimentVideoAsset selected(
      LearningSalesCycle cycle,
      String stage,
      String field,
      ExperimentVideoSlot role,
      boolean approved) {
    JsonNode proof = completed(cycle, stage);
    var value = video(cycle, id(proof, field), role, approved);
    require(
        proof.path("assetFingerprint").asText().equals(fingerprint(value)),
        "A mídia selecionada mudou. Registre novamente os vídeos após devolver o ciclo para correção.");
    return value;
  }

  /** Valida a fonte oficial com finalidade, áudio, URLs e aprovação independente quando exigida. */
  private ExperimentVideoAsset video(
      LearningSalesCycle cycle, Long id, ExperimentVideoSlot role, boolean approved) {
    var value = videos.findById(id).orElse(null);
    require(
        value != null && scoped(value, cycle), "Vídeo não encontrado neste produto e experimento.");
    require(
        value.getSlot() == role
            && value.getStatus() == ExperimentVideoStatus.READY
            && Boolean.TRUE.equals(value.getHasAudio())
            && value.getDurationSeconds() != null
            && value.getDurationSeconds() > 0
            && https(value.getAssetUrl())
            && (role != ExperimentVideoSlot.LANDING_HERO || https(value.getHlsPlaybackUrl())),
        "Selecione um vídeo " + role + " pronto, com duração, áudio e URLs válidas.");
    require(
        !approved || value.getReviewStatus() == ExperimentVideoReviewStatus.APPROVED,
        "O vídeo #"
            + id
            + " aguarda aprovação independente ou foi reprovado. Corrija e revise pelo fluxo oficial.");
    return value;
  }

  /** Impede cruzamento de produto e experimento mesmo quando um identificador válido é enviado. */
  private boolean scoped(ExperimentVideoAsset video, LearningSalesCycle cycle) {
    return video.getExperiment() != null
        && cycle.getExperimentId().equals(video.getExperiment().getId())
        && video.getExperiment().getProduct() != null
        && cycle.getProductId().equals(video.getExperiment().getProduct().getId());
  }

  /** Busca somente a última conclusão da etapa posterior à revisão atual do produto. */
  private JsonNode completed(LearningSalesCycle cycle, String stage) {
    return events.findByCycleIdOrderByRevisionAsc(cycle.getId()).stream()
        .filter(
            e ->
                stage.equals(e.getFromStage())
                    && "COMPLETE".equals(e.getAction())
                    && !e.getCreatedAt().isBefore(cycle.getVersionChangedAt()))
        .reduce((a, b) -> b)
        .map(e -> json.read(e.getEvidenceJson()))
        .orElseThrow(
            () ->
                new ResponseStatusException(
                    org.springframework.http.HttpStatus.CONFLICT,
                    "Conclua a etapa " + label(stage) + " desta revisão antes de avançar."));
  }

  /** Compara a identidade de mídia sem confundir a aprovação posterior com alteração do arquivo. */
  private String fingerprint(ExperimentVideoAsset v) {
    return hash(
        json.write(
            List.of(
                v.getId(),
                v.getSlot(),
                safe(v.getAssetUrl()),
                safe(v.getHlsPlaybackUrl()),
                safe(v.getDurationSeconds()),
                safe(v.getHasAudio()),
                safe(v.getScript()),
                safe(v.getObjective()),
                safe(v.getPrimaryMetric()))));
  }

  /** Canonicaliza objetos JSON para preservar identidade quando a ordem das chaves mudar. */
  private Object canonical(JsonNode node) {
    if (node.isObject()) {
      Map<String, Object> sorted = new TreeMap<>();
      node.fields().forEachRemaining(e -> sorted.put(e.getKey(), canonical(e.getValue())));
      return sorted;
    }
    if (node.isArray()) {
      List<Object> values = new ArrayList<>();
      node.forEach(n -> values.add(canonical(n)));
      return values;
    }
    return node.toString();
  }

  /**
   * Calcula assinatura determinística do conteúdo conferido, sem afirmar inspeção física da mídia.
   */
  private String hash(String value) {
    try {
      return HexFormat.of()
          .formatHex(
              java.security.MessageDigest.getInstance("SHA-256")
                  .digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (java.security.NoSuchAlgorithmException ex) {
      log.error("Ciclos: algoritmo SHA-256 indisponível", ex);
      throw new IllegalStateException("SHA-256 indisponível", ex);
    }
  }

  /** Normaliza campo opcional antes de calcular a assinatura. */
  private String safe(Object value) {
    return value == null ? "" : value.toString();
  }

  /** Extrai somente identificadores positivos integrais, sem truncar decimais. */
  private Long id(JsonNode data, String field) {
    require(
        data.path(field).isIntegralNumber()
            && data.path(field).canConvertToLong()
            && data.path(field).asLong() > 0,
        "Selecione " + field + " entre as opções deste experimento.");
    return data.path(field).asLong();
  }

  /** Confere URL sem acessá-la nem aceitar credenciais embutidas. */
  private boolean https(String value) {
    if (value == null) return false;
    try {
      var uri = URI.create(value);
      return "https".equals(uri.getScheme()) && uri.getHost() != null && uri.getUserInfo() == null;
    } catch (IllegalArgumentException ex) {
      log.debug("Ciclos: URL inválida no contrato de mídia", ex);
      return false;
    }
  }
}
