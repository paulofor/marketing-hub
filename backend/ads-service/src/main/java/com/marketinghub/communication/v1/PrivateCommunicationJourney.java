package com.marketinghub.communication.v1;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.agenttask.BusinessProcessActivityInstance;
import com.marketinghub.businessprocess.BusinessProcessActivityDefinition;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocess.execution.service.backendactivity.BackendProductProcessActivityExecutionResult;
import com.marketinghub.businessprocess.execution.service.backendactivity.BackendProductProcessActivityReadiness;
import com.marketinghub.businessprocess.execution.service.predecessor.ProductProcessActivityPredecessorService;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.agenttask.BusinessProcessActivityInstanceRepository;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Responsabilidade: comprovar e registrar a jornada privada na referência do produto ou ciclo. */
@Service
@RequiredArgsConstructor
@Slf4j
public class PrivateCommunicationJourney {
  private static final String PROCESS = "pde-communication-sales-journey";
  private static final Set<String> TECHNICAL_CHECKS =
      Set.of(
          "sameVersion",
          "desktopAndMobile",
          "happyResultWithinTenMinutes",
          "recoveryPreserved",
          "safetyBlocked",
          "accessibilityBasic",
          "responsiveLayout",
          "privacyPreserved",
          "internalTrafficSegregated",
          "paymentDisabled",
          "publicationDisabled",
          "campaignDisabled",
          "zeroMediaSpend");
  private final IrisLearningCycleContext cycles;
  private final com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository
      cycleRepository;
  private final ProductProcessActivityPredecessorService predecessors;
  private final BusinessProcessActivityInstanceRepository instances;
  private final AgentTaskRepository tasks;
  private final PrivateCommunicationCreativeProof creativeProof;
  private final ObjectMapper json;

  @org.springframework.beans.factory.annotation.Autowired(required = false)
  private IrisPrivateProductContext privateProducts;

  /** Reconhece produtos e ciclos privados mesmo bloqueados, sem fallback para plano histórico. */
  @Transactional(readOnly = true)
  public boolean applies(String reference) {
    return IrisPrivateProductContext.supports(reference) || privateCycle(reference).isPresent();
  }

  /** Expõe o destino e seus requisitos antes de criar uma tentativa ou consumir modelo. */
  @Transactional(readOnly = true)
  public BackendProductProcessActivityReadiness readiness(
      BusinessProcessDefinition process,
      BusinessProcessActivityDefinition activity,
      Product product,
      String reference) {
    try {
      var completed =
          latest(activity, reference)
              .filter(i -> "COMPLETED".equals(i.getStatus()) && i.isObjectiveAchieved());
      if (preparationArchived(reference) && completed.isPresent()) {
        var historical = archivedEvidence(completed.get(), activity, product, reference);
        return new BackendProductProcessActivityReadiness(
            false,
            "A preparação deste ciclo já foi concluída. O histórico permanece disponível.",
            "Consultar destino registrado",
            "Destino registrado na conclusão: "
                + historical.path("prototypeVersion").asText()
                + "; checkout simulado.",
            null,
            null,
            List.of(),
            null,
            historical.path("destinationUrl").asText());
      }
      var evidence = proof(process, activity, product, reference);
      return new BackendProductProcessActivityReadiness(
          true,
          "O destino aprovado é a experiência privada "
              + evidence.path("prototypeVersion").asText()
              + "; acesso, retomada, eventos e checkout simulado possuem provas do próprio contexto.",
          "integration".equals(activity.getActivityId())
              ? "Confirmar integração privada"
              : "Confirmar destino aprovado",
          "integration".equals(activity.getActivityId())
              ? "Registra destino, acesso, retomada e eventos no contexto, com checkout simulado."
              : "Reutiliza a experiência homologada e registra suas provas no contexto.",
          null,
          null,
          List.of(),
          null,
          evidence.path("destinationUrl").asText());
    } catch (RuntimeException ex) {
      log.warn(
          "Jornada privada aguarda provas. productId={} processId={} activityId={} sourceReference={}",
          product.getId(),
          process.getId(),
          activity.getActivityId(),
          reference,
          ex);
      return new BackendProductProcessActivityReadiness(false, ex.getMessage());
    }
  }

  /**
   * Reserva a ocorrência e persiste a decisão idempotente, preservando as tentativas anteriores.
   */
  @Transactional
  public BackendProductProcessActivityExecutionResult complete(
      BusinessProcessDefinition process,
      BusinessProcessActivityDefinition activity,
      Product product,
      String reference) {
    var proof = proof(process, activity, product, reference);
    String serialized = proof.toString();
    var latest =
        instances.findTopByActivityDefinitionIdAndSourceReferenceOrderByOccurrenceNumberDesc(
            activity.getId(), reference);
    if (latest
        .filter(
            i ->
                "COMPLETED".equals(i.getStatus())
                    && i.isObjectiveAchieved()
                    && serialized.equals(i.getObjectiveEvidenceJson()))
        .isEmpty()) {
      require(
          latest.stream().noneMatch(i -> Set.of("PENDING", "IN_PROGRESS").contains(i.getStatus())),
          "Aguarde a tentativa em andamento antes de registrar outra integração.");
      var instance = new BusinessProcessActivityInstance();
      Instant now = Instant.now();
      instance.setActivityDefinition(activity);
      instance.setSourceReference(reference);
      instance.setOccurrenceNumber(latest.map(i -> i.getOccurrenceNumber() + 1).orElse(1));
      instance.setStatus("COMPLETED");
      instance.setObjectiveAchieved(true);
      instance.setObjectiveEvidenceJson(serialized);
      instance.setKnownCostUsd(BigDecimal.ZERO);
      instance.setCostCoverage("COMPLETE");
      instance.setEvidenceQuality("DIRECT");
      instance.setCreatedAt(now);
      instance.setUpdatedAt(now);
      instance.setEnteredAt(now);
      instance.setExitedAt(now);
      instances.saveAndFlush(instance);
    }
    return new BackendProductProcessActivityExecutionResult(
        reference,
        "COMPLETED",
        true,
        "integration".equals(activity.getActivityId())
            ? "Jornada privada integrada com destino, acesso, retomada, eventos e checkout simulado comprovados."
            : "Destino aprovado reutilizado; nenhuma landing adicional é necessária nesta preparação privada.");
  }

  /** Invalida uma conclusão quando sua prova deixa de corresponder à versão e aos predecessores. */
  @Transactional(readOnly = true)
  public boolean stale(
      BusinessProcessDefinition process,
      BusinessProcessActivityDefinition activity,
      Product product,
      String reference) {
    var latest = latest(activity, reference);
    if (latest.filter(i -> "COMPLETED".equals(i.getStatus()) && i.isObjectiveAchieved()).isEmpty())
      return false;
    try {
      if (preparationArchived(reference)) {
        archivedEvidence(latest.orElseThrow(), activity, product, reference);
        return false;
      }
      return !proof(process, activity, product, reference)
          .toString()
          .equals(latest.orElseThrow().getObjectiveEvidenceJson());
    } catch (RuntimeException ex) {
      log.warn(
          "Prova da jornada privada superada. productId={} activityId={} sourceReference={}",
          product.getId(),
          activity.getActivityId(),
          reference,
          ex);
      return true;
    }
  }

  /** Distingue o ciclo privado sem exigir autorização operacional para consultar seu histórico. */
  private Optional<com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle>
      privateCycle(String reference) {
    if (reference == null || !reference.matches("experiment:[1-9][0-9]*")) return Optional.empty();
    return cycleRepository
        .findByExperimentId(Long.parseLong(reference.substring(11)))
        .filter(c -> !c.isBaseline());
  }

  /**
   * Preserva conclusões ao sair da preparação; reabrir essa fase volta a exigir provas vigentes.
   */
  private boolean preparationArchived(String reference) {
    return privateCycle(reference)
        .filter(
            c ->
                (c.getStatus() != null && !"OPEN".equals(c.getStatus()))
                    || (c.getStage() != null
                        && !Set.of("ADJUSTMENT", "VALIDATION").contains(c.getStage())))
        .isPresent();
  }

  /**
   * Confere a identidade da prova arquivada sem transformar falta de nova autorização em falha
   * passada.
   */
  private JsonNode archivedEvidence(
      BusinessProcessActivityInstance instance,
      BusinessProcessActivityDefinition activity,
      Product product,
      String reference) {
    try {
      var evidence = json.readTree(instance.getObjectiveEvidenceJson());
      String expected =
          "integration".equals(activity.getActivityId())
              ? "PDE_COMMUNICATION_PRIVATE_INTEGRATION_V1"
              : "PDE_COMMUNICATION_PRIVATE_DESTINATION_V1";
      require(
          evidence != null
              && expected.equals(evidence.path("evidenceType").asText())
              && product.getId().equals(evidence.path("productId").asLong())
              && reference.equals(evidence.path("sourceReference").asText())
              && !evidence.path("prototypeVersion").asText().isBlank()
              && evidence.path("destinationUrl").asText().startsWith("https://"),
          "A prova arquivada não corresponde à atividade e ao produto deste ciclo.");
      return evidence;
    } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
      log.warn(
          "Prova arquivada da jornada inválida. instanceId={} sourceReference={}",
          instance.getId(),
          reference,
          ex);
      throw new IllegalStateException("A prova arquivada da jornada não pôde ser lida.", ex);
    }
  }

  /** Valida o contrato privado, a versão homologada e a sequência antes de compor sua prova. */
  private ObjectNode proof(
      BusinessProcessDefinition process,
      BusinessProcessActivityDefinition activity,
      Product product,
      String reference) {
    require(
        PROCESS.equals(process.getProcessCode())
            && Set.of("destination", "integration").contains(activity.getActivityId()),
        "A atividade não pertence à preparação da jornada privada.");
    var input =
        json.valueToTree(
            (IrisPrivateProductContext.supports(reference)
                    ? privateProducts == null
                        ? Optional.<Map<String, Object>>empty()
                        : privateProducts.resolve(reference)
                    : cycles.resolve(reference))
                .orElseThrow(
                    () -> new IllegalStateException("O contexto privado não foi encontrado.")));
    require(
        "AVAILABLE".equals(input.path("availability").asText())
            && "READY".equals(input.path("inputReadiness").asText()),
        input.path("reason").asText("O gate da versão privada não está aprovado."));
    require(
        reference.equals(input.path("sourceReference").asText())
            && product.getId().equals(input.path("product").path("id").asLong()),
        "Produto ou referência divergente na jornada privada.");
    var destination = input.path("approvedDestination");
    String url = input.path("product").path("publicUrl").asText();
    String version = input.path("prototypeVersion").asText();
    require(
        "PRIVATE_PDE_DESTINATION_V1".equals(destination.path("contractVersion").asText())
            && "APPROVED_PRIVATE_PDE".equals(destination.path("type").asText())
            && !destination.path("requiresLandingGeneration").asBoolean(true)
            && !version.isBlank()
            && version.equals(destination.path("prototypeVersion").asText())
            && url.startsWith("https://")
            && url.equals(destination.path("url").asText())
            && url.equals(input.path("validationGate").path("publicUrl").asText())
            && !input.path("publicationAuthorized").asBoolean(true)
            && !input.path("paymentEnabled").asBoolean(true)
            && !input.path("externalMediaSpendAuthorized").asBoolean(true),
        "O destino não preserva o contrato e os limites privados do contexto.");
    var preceding = predecessors.readiness(process, activity, reference);
    require(preceding.ready(), preceding.reason());
    var communication = communication(input, process.getId(), reference);
    var technical = technical(input, reference, version, url);
    var evidence = json.createObjectNode();
    evidence.put(
        "evidenceType",
        "integration".equals(activity.getActivityId())
            ? "PDE_COMMUNICATION_PRIVATE_INTEGRATION_V1"
            : "PDE_COMMUNICATION_PRIVATE_DESTINATION_V1");
    evidence.set("mode", input.path("mode"));
    evidence.put("productId", product.getId());
    evidence.put("sourceReference", reference);
    evidence.set("cycleId", input.path("cycleId"));
    evidence.set("chainDefinitionId", input.path("chainDefinitionId"));
    evidence.put("prototypeVersion", version);
    evidence.put("destinationUrl", url);
    evidence.put("requiresLandingGeneration", false);
    evidence.set("gateInstanceId", input.path("gateInstanceId"));
    evidence.put("gateSha256", hash(input.path("validationGate").toString()));
    evidence.set("communicationTaskId", communication.path("taskId"));
    evidence.set("communicationSha256", communication.path("resultSha256"));
    evidence.set("technicalTaskId", technical.path("taskId"));
    evidence.set("technicalResultSha256", technical.path("resultSha256"));
    evidence.set("validatedChecks", technical.path("result").path("checks"));
    evidence.put("trafficClass", "AGENT_VALIDATION");
    evidence.put("internalMarker", "mh_internal_test");
    evidence.put("checkoutMode", "SIMULATED");
    evidence.put("paymentEnabled", false);
    evidence.put("publicationAuthorized", false);
    evidence.put("campaignAuthorized", false);
    evidence.put("mediaSpendAuthorized", false);
    evidence.put("humanEvidenceClaimed", false);
    evidence.put("commercialEvidenceClaimed", false);
    var predecessorsProof = evidence.putArray("predecessors");
    var prior =
        instances
            .findAllByActivityDefinitionProcessDefinitionIdAndSourceReferenceOrderByActivityDefinitionIdAscOccurrenceNumberAsc(
                process.getId(), reference);
    for (String code :
        "integration".equals(activity.getActivityId())
            ? List.of("communicationContract", "creatives", "destination")
            : List.of("communicationContract", "creatives")) {
      var instance =
          prior.stream()
              .filter(i -> code.equals(i.getActivityDefinition().getActivityId()))
              .max(
                  java.util.Comparator.comparing(
                      BusinessProcessActivityInstance::getOccurrenceNumber))
              .orElseThrow(
                  () -> new IllegalStateException("Conclua neste ciclo a atividade " + code + "."));
      require(
          "COMPLETED".equals(instance.getStatus()) && instance.isObjectiveAchieved(),
          "A atividade " + code + " não possui objetivo comprovado neste contexto.");
      if ("creatives".equals(code))
        evidence.set("creativeApproval", creativeProof.resolve(instance, reference, version));
      if ("destination".equals(code))
        require(
            proof(process, instance.getActivityDefinition(), product, reference)
                .toString()
                .equals(instance.getObjectiveEvidenceJson()),
            "O destino deve ser confirmado novamente: suas provas foram substituídas.");
      predecessorsProof
          .addObject()
          .put("activityId", code)
          .put("instanceId", instance.getId())
          .put(
              "evidenceSha256",
              hash(Objects.requireNonNullElse(instance.getObjectiveEvidenceJson(), "")));
    }
    require(
        tasks.findFunctionalSnapshots(reference, Set.of("landing-page-generation"), null).stream()
            .noneMatch(t -> Set.of("PENDING", "IN_PROGRESS").contains(t.status())),
        "Aguarde a tarefa de landing em curso; a decisão preserva seu resultado antes de mudar o destino.");
    return evidence;
  }

  /** Exige a comunicação da mesma definição e o hash estratégico ainda vigente. */
  private JsonNode communication(JsonNode input, Long processId, String reference) {
    for (var artifact : input.path("communicationArtifacts")) {
      var result = artifact.path("result");
      if (!"communicationContract".equals(artifact.path("activityId").asText())) continue;
      require(
          processId.equals(artifact.path("processDefinitionId").asLong())
              && "IRIS_COMMUNICATION_V1".equals(result.path("contractVersion").asText())
              && "COMMUNICATION_PACKAGE".equals(result.path("outputType").asText())
              && "COMPLETED".equals(result.path("executionStatus").asText())
              && reference.equals(result.path("sourceReference").asText())
              && input
                  .path("marketStrategicContract")
                  .path("contentHash")
                  .asText()
                  .equals(result.path("strategicContractReference").path("contentHash").asText())
              && !result.path("functionalOutput").path("messageStrategy").asText().isBlank()
              && !result.path("functionalOutput").path("channelBriefings").isEmpty(),
          "O contrato de comunicação não corresponde à estratégia e à definição deste ciclo.");
      return artifact;
    }
    throw new IllegalStateException(
        "Conclua o contrato de comunicação do próprio contexto com Íris.");
  }

  /** Exige os controles de integração comprovados pelo executor técnico no gate vigente. */
  private JsonNode technical(JsonNode input, String reference, String version, String url) {
    for (var artifact : input.path("approvedUpstreamArtifacts")) {
      if (!"technicalHomologation".equals(artifact.path("activityId").asText())) continue;
      var result = artifact.path("result");
      require(
          "PDE_AGENT_TECHNICAL_HOMOLOGATION_V1".equals(result.path("contractVersion").asText())
              && "APPROVED".equals(result.path("decision").asText())
              && reference.equals(result.path("sourceReference").asText())
              && version.equals(result.path("prototypeVersion").asText())
              && url.equals(result.path("publicUrl").asText())
              && TECHNICAL_CHECKS.stream()
                  .allMatch(key -> result.path("checks").path(key).asBoolean()),
          "A homologação não comprova acesso, retomada, eventos e checkout simulado desta versão.");
      return artifact;
    }
    throw new IllegalStateException("A prova técnica da jornada privada está ausente.");
  }

  /** Consulta a última ocorrência da referência sem exigir escrita para exibir seu histórico. */
  private Optional<BusinessProcessActivityInstance> latest(
      BusinessProcessActivityDefinition activity, String reference) {
    return instances.findFirstByActivityDefinitionIdAndSourceReferenceOrderByOccurrenceNumberDesc(
        activity.getId(), reference);
  }

  /** Mantém cada ausência como bloqueio funcional explícito. */
  private static void require(boolean condition, String reason) {
    if (!condition) throw new IllegalStateException(reason);
  }

  /** Vincula a decisão aos bytes persistidos da prova, sem copiar prompts ou dados privados. */
  private static String hash(String value) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (java.security.NoSuchAlgorithmException ex) {
      log.error("SHA-256 indisponível ao vincular a jornada privada.", ex);
      throw new IllegalStateException("Não foi possível vincular a prova da jornada.", ex);
    }
  }
}
