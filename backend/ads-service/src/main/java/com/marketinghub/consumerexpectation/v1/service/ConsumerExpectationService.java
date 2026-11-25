package com.marketinghub.consumerexpectation.v1.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.consumerexpectation.v1.service.learning.ExpectationLearningResponse;
import com.marketinghub.consumerexpectation.v1.service.plan.ExpectationPlanRequest;
import com.marketinghub.consumerexpectation.v1.service.plan.ExpectationPlanResponse;
import com.marketinghub.consumerexpectation.v1.service.protocol.ExpectationProtocol;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleEventRepository;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Prepara alternativas consultivas e lê aprendizagem comercial sem executar IA ou transições. */
@Service
@Transactional(readOnly = true)
public class ConsumerExpectationService {
  private static final Logger log = LoggerFactory.getLogger(ConsumerExpectationService.class);
  private final LearningSalesCycleRepository cycles;
  private final LearningSalesCycleEventRepository events;
  private final ObjectMapper mapper;

  /** Recebe fontes persistidas oficiais para impedir medições fabricadas pelo formulário. */
  public ConsumerExpectationService(
      LearningSalesCycleRepository cycles,
      LearningSalesCycleEventRepository events,
      ObjectMapper mapper) {
    this.cycles = cycles;
    this.events = events;
    this.mapper = mapper;
  }

  /** Expõe a mesma ciência, limites e contratos utilizados no contexto dos agentes. */
  public JsonNode protocol() {
    return ExpectationProtocol.catalog();
  }

  /**
   * Prepara três alternativas contextualizadas; referência informada não equivale a prova revisada.
   */
  public ExpectationPlanResponse preview(ExpectationPlanRequest input) {
    JsonNode protocol = protocol();
    List<String> gaps = new ArrayList<>();
    if (input.evidenceBasis() == ExpectationPlanRequest.EvidenceBasis.HYPOTHESIS)
      gaps.add(
          "Argos: a expectativa ainda é hipótese; buscar fonte pública e contraponto no mesmo contexto.");
    if (blank(input.evidenceReference()))
      gaps.add("Argos: falta referência rastreável da expectativa.");
    if (blank(input.deliveryProofReference()))
      gaps.add("Dédalo: falta prova funcional da entrega declarada.");
    gaps.add(
        "Psique e Têmis: revisar o artefato real; este desenho não comprova compreensão nem integridade.");
    JsonNode surface = protocol.path("surfaces").path(input.surface().name());
    List<ExpectationPlanResponse.Variant> variants = new ArrayList<>();
    for (JsonNode variant : protocol.path("variantDefinitions")) {
      String concept = surface.path("action").asText() + ": " + input.actualDelivery();
      variants.add(
          new ExpectationPlanResponse.Variant(
              variant.path("code").asText(),
              variant.path("label").asText(),
              surface.path("owner").asText(),
              concept,
              input.expectedExperience(),
              variant.path("change").asText(),
              surface.path("check").asText(),
              "Para "
                  + input.audience()
                  + ", em "
                  + input.context()
                  + ", testar se esta apresentação de "
                  + input.actualDelivery()
                  + " ajuda a alcançar "
                  + input.desiredOutcome()
                  + " com mais compras líquidas e contribuição, sem piorar esforço ou entrega.",
              variant.path("risk").asText()));
    }
    return new ExpectationPlanResponse(
        ExpectationProtocol.VERSION,
        fingerprint(input, protocol),
        "DRAFT_HYPOTHESIS",
        input,
        variants,
        List.copyOf(gaps),
        strings(protocol.path("validation")),
        strings(protocol.path("metrics")),
        "Prévia não salva. Vincular o desenho escolhido à tarefa e ao ciclo/experimento oficiais antes de executar; preservar versão e evidências.",
        false);
  }

  /** Lê apenas a última tentativa de medição da mesma identidade, sem retentar coleta ou gasto. */
  public ExpectationLearningResponse learning(Long productId, Long cycleId) {
    var cycle =
        cycles
            .findById(cycleId)
            .filter(c -> Objects.equals(c.getProductId(), productId))
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Ciclo não encontrado neste produto."));
    var event =
        events
            .findFirstByCycleIdAndActionInOrderByRevisionDesc(
                cycleId, List.of("MEASURE", "MEASUREMENT_BLOCKED"))
            .orElse(null);
    String status = "MEASUREMENT_REQUIRED";
    String owner = "Hermes";
    String guidance =
        "Não há medição conciliada aplicável. Preparar instrumentação no ciclo; atenção e simulação não comprovam vendas.";
    var metrics = mapper.createObjectNode();
    String sourceFingerprint = null;
    if (event != null) {
      JsonNode evidence = readEvidence(event.getEvidenceJson(), cycleId, event.getId());
      boolean valid =
          "MEASURE".equals(event.getAction())
              && "LEARNING_CYCLE_AUTOMATIC_MEASUREMENT_V2"
                  .equals(evidence.path("contractVersion").asText())
              && evidence.path("automatic").asBoolean(false)
              && evidence.path("dataValid").asBoolean(false)
              && evidence.path("testDataExcluded").asBoolean(false)
              && evidence.path("experimentId").isIntegralNumber()
              && evidence.path("experimentId").longValue() == cycle.getExperimentId()
              && evidence.path("sourceFingerprint").isTextual()
              && !evidence.path("sourceFingerprint").asText().isBlank()
              && event.getCreatedAt() != null
              && (cycle.getVersionChangedAt() == null
                  || !event.getCreatedAt().isBefore(cycle.getVersionChangedAt()));
      if (valid) {
        for (String key :
            List.of(
                "humanVisitors",
                "netSales",
                "refunds",
                "spendBrl",
                "revenueBrl",
                "contributionBrl",
                "deliveryVerified",
                "useVerified",
                "periodStart",
                "periodEnd",
                "measurementLimitations")) {
          metrics.set(key, evidence.has(key) ? evidence.get(key) : mapper.nullNode());
        }
        sourceFingerprint = evidence.path("sourceFingerprint").asText();
        status = "INCONCLUSIVE";
        guidance =
            "Medição observacional disponível. Sem compras líquidas, custo completo e entrega útil não confirmar a hipótese. Preservar lacunas e comparar condições antes de novo ciclo.";
        boolean completeEconomics =
            List.of(
                    "humanVisitors",
                    "netSales",
                    "refunds",
                    "spendBrl",
                    "revenueBrl",
                    "contributionBrl")
                .stream()
                .allMatch(key -> metrics.path(key).isNumber());
        if (completeEconomics && metrics.path("netSales").asLong(0) > 0) {
          if (metrics.path("contributionBrl").decimalValue().signum() <= 0) {
            status = "REVIEW_ECONOMICS";
            owner = "Plutus";
            guidance =
                "Há compras, mas a contribuição conciliada não é positiva. Rever economia antes de escalar; surpresa ou atenção não compensam prejuízo.";
          } else if (metrics.path("useVerified").asBoolean(false)
              && metrics.path("deliveryVerified").asBoolean(false)) {
            status = "OBSERVED_NOT_CAUSAL";
            owner = "Atena / Hermes";
            guidance =
                "Compras, contribuição positiva e uso foram conciliados. Isso não isola efeito de surpresa: comparar hipótese, público, canal, exposição e janela; registrar confirmação, contradição ou inconclusão no ciclo existente.";
          }
        }
      } else {
        guidance =
            "A última medição está bloqueada, inválida ou pertence a outra versão. Corrigir a fonte no ciclo; não reutilizar um sucesso antigo nem transformar ausência em zero.";
      }
    }
    return new ExpectationLearningResponse(
        ExpectationProtocol.VERSION,
        productId,
        cycleId,
        cycle.getExperimentId(),
        cycle.getProductVersion(),
        cycle.getPreviousCycleId(),
        status,
        event == null ? null : event.getId(),
        event == null ? null : event.getCreatedAt(),
        metrics,
        sourceFingerprint,
        owner,
        guidance,
        "/business-process-chains/learning-cycles?productId="
            + productId
            + "&chainId="
            + cycle.getChainDefinitionId()
            + "&cycleId="
            + cycleId,
        false,
        false);
  }

  /**
   * Preserva falha de leitura como ausência de evidência e registra correlação para diagnóstico.
   */
  private JsonNode readEvidence(String json, Long cycleId, Long eventId) {
    try {
      JsonNode parsed = mapper.readTree(json == null || json.isBlank() ? "{}" : json);
      return parsed == null ? mapper.createObjectNode() : parsed;
    } catch (JsonProcessingException ex) {
      log.error("Expectativas: evidência inválida cycleId={} eventId={}", cycleId, eventId, ex);
      return mapper.createObjectNode();
    }
  }

  /** Identifica a entrada e a revisão do protocolo sem persistir dados do formulário. */
  private String fingerprint(ExpectationPlanRequest input, JsonNode protocol) {
    try {
      byte[] bytes = mapper.writeValueAsBytes(List.of(input, protocol));
      return java.util.HexFormat.of()
          .formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes));
    } catch (JsonProcessingException | java.security.NoSuchAlgorithmException ex) {
      log.error("Expectativas: falha ao identificar desenho surface={}", input.surface(), ex);
      throw new IllegalStateException("Não foi possível identificar o desenho", ex);
    }
  }

  /** Extrai instruções versionadas sem duplicar o catálogo na implementação. */
  private List<String> strings(JsonNode values) {
    List<String> result = new ArrayList<>();
    values.forEach(value -> result.add(value.asText()));
    return List.copyOf(result);
  }

  /** Distingue referência ausente de um texto informado sem validar a fonte remotamente. */
  private boolean blank(String value) {
    return value == null || value.isBlank();
  }
}
