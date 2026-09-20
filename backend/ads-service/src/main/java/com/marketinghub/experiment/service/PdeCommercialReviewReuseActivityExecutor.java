package com.marketinghub.experiment.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agenttask.AgentTaskReviewSnapshot;
import com.marketinghub.agenttask.BusinessProcessActivityInstance;
import com.marketinghub.businessprocess.BusinessProcessActivityDefinition;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocess.execution.service.backendactivity.BackendProductProcessActivityExecutionResult;
import com.marketinghub.businessprocess.execution.service.backendactivity.BackendProductProcessActivityExecutor;
import com.marketinghub.businessprocess.execution.service.backendactivity.BackendProductProcessActivityReadiness;
import com.marketinghub.businessprocess.execution.service.predecessor.ProductProcessActivityPredecessorService;
import com.marketinghub.businessprocess.execution.service.productProcessExecutions.ProductProcessActivityRequirementResponse;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.opala.commercial.v1.service.OpalaCommercialRouting;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.agenttask.BusinessProcessActivityInstanceRepository;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessActivityDefinitionRepository;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Responsabilidade: reaproveitar no Processo 5 uma revisão vigente do subprocesso comercial sem
 * repetir custo de agente.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PdeCommercialReviewReuseActivityExecutor
    implements BackendProductProcessActivityExecutor {
  private static final String PROCESS_CODE = "pde-commercial-homologation-activation";
  private static final String REUSE_VERSION = "COMMERCIAL_REVIEW_REUSE_V1";
  private static final Set<String> ACTIVITIES =
      Set.of("humanExperienceReview", "commercialIntegrityReview");

  private final ObjectMapper json;
  private final ProductProcessActivityPredecessorService predecessors;
  private final LearningSalesCycleRepository cycles;
  private final OpalaCommercialRouting routing;
  private final BusinessProcessActivityDefinitionRepository definitions;
  private final AgentTaskRepository tasks;
  private final BusinessProcessActivityInstanceRepository instances;

  @org.springframework.beans.factory.annotation.Autowired(required = false)
  private com.marketinghub.quartzo.commercial.v1.service.QuartzoCommercialService
      quartzoPreparation;

  /** Reconhece somente os dois gates que declaram reutilização auditável na versão do processo. */
  @Override
  public boolean supports(
      BusinessProcessDefinition process, BusinessProcessActivityDefinition activity) {
    return process != null
        && activity != null
        && PROCESS_CODE.equals(process.getProcessCode())
        && ACTIVITIES.contains(activity.getActivityId())
        && REUSE_VERSION.equals(metadata(activity).path("evidenceReuseVersion").asText());
  }

  /** Exige preparação concluída e prova ainda vigente no mesmo produto, ciclo e experimento. */
  @Override
  @Transactional(readOnly = true)
  public BackendProductProcessActivityReadiness readiness(
      BusinessProcessDefinition process,
      BusinessProcessActivityDefinition activity,
      Product product,
      String sourceReference) {
    var predecessor = predecessors.readiness(process, activity, sourceReference);
    try {
      ReusedReview proof = proof(activity, product, sourceReference);
      boolean ready = predecessor.ready();
      return new BackendProductProcessActivityReadiness(
          ready,
          ready
              ? "Parecer vigente localizado no subprocesso; a reutilização não gera nova chamada paga."
              : predecessor.reason(),
          "Reutilizar parecer vigente",
          "O backend referencia a tarefa original e registra custo incremental zero; qualquer mudança material invalida a prova no gate Opala.",
          null,
          null,
          List.of(
              requirement(
                  "COMMERCIAL_PREPARATION_COMPLETED",
                  "Preparação comercial concluída",
                  predecessor.ready(),
                  predecessor.reason()),
              requirement(
                  "REVIEW_SCOPE_CURRENT",
                  "Parecer do mesmo escopo",
                  true,
                  "Tarefa #"
                      + proof.task().taskId()
                      + " e consolidação #"
                      + proof.readyInstance().getId()
                      + " correspondem à configuração vigente.")));
    } catch (RuntimeException ex) {
      log.warn(
          "Reutilização de parecer comercial bloqueada. processDefinitionId={} activityId={} productId={} source={}",
          process == null ? null : process.getId(),
          activity == null ? null : activity.getActivityId(),
          product == null ? null : product.getId(),
          sourceReference,
          ex);
      return new BackendProductProcessActivityReadiness(
          false,
          ex.getMessage(),
          "Revalidar parecer na preparação",
          "Abra o subprocesso comercial e renove somente a revisão afetada.",
          null,
          null,
          List.of(
              requirement(
                  "COMMERCIAL_PREPARATION_COMPLETED",
                  "Preparação comercial concluída",
                  predecessor.ready(),
                  predecessor.reason()),
              requirement(
                  "REVIEW_SCOPE_CURRENT", "Parecer do mesmo escopo", false, ex.getMessage())));
    }
  }

  /** Materializa uma referência idempotente à evidência original, preservando o custo no filho. */
  @Override
  @Transactional
  public BackendProductProcessActivityExecutionResult execute(
      BusinessProcessDefinition process,
      BusinessProcessActivityDefinition activity,
      Product product,
      String sourceReference) {
    var check = readiness(process, activity, product, sourceReference);
    if (!check.ready()) throw new IllegalStateException(check.reason());
    ReusedReview proof = proof(activity, product, sourceReference);
    String fingerprint = fingerprint(proof);
    var previous =
        instances.findTopByActivityDefinitionIdAndSourceReferenceOrderByOccurrenceNumberDesc(
            activity.getId(), sourceReference);
    if (previous
        .filter(
            instance ->
                "COMPLETED".equals(instance.getStatus())
                    && instance.isObjectiveAchieved()
                    && fingerprint.equals(
                        read(instance.getObjectiveEvidenceJson()).path("fingerprint").asText()))
        .isPresent()) return completed(sourceReference);
    Instant now = Instant.now();
    var evidence = json.createObjectNode();
    evidence.put("evidenceType", "COMMERCIAL_REVIEW_REUSED_V1");
    evidence.put("sourceProcessDefinitionId", proof.process().getId());
    evidence.put("sourceProcessCode", proof.process().getProcessCode());
    evidence.put("sourceActivityId", proof.sourceActivity());
    evidence.put("sourceTaskId", proof.task().taskId());
    evidence.put("sourceReadyInstanceId", proof.readyInstance().getId());
    evidence.put("sourceReference", sourceReference);
    evidence.put("fingerprint", fingerprint);
    evidence.put("incrementalCostUsd", BigDecimal.ZERO);
    evidence.put("costRecordedInSubprocess", true);
    var instance = new BusinessProcessActivityInstance();
    instance.setActivityDefinition(activity);
    instance.setSourceReference(sourceReference);
    instance.setOccurrenceNumber(previous.map(value -> value.getOccurrenceNumber() + 1).orElse(1));
    instance.setStatus("COMPLETED");
    instance.setEnteredAt(now);
    instance.setExitedAt(now);
    instance.setObjectiveAchieved(true);
    instance.setObjectiveEvidenceJson(evidence.toString());
    instance.setBlockedReason(null);
    instance.setKnownCostUsd(BigDecimal.ZERO);
    instance.setCostCoverage("COMPLETE");
    instance.setEvidenceQuality("REUSED_DIRECT");
    instance.setCreatedAt(now);
    instance.setUpdatedAt(now);
    instances.saveAndFlush(instance);
    return completed(sourceReference);
  }

  /** Localiza a revisão após o gate do tipo revalidar produto, experimento, versão e ativos. */
  private ReusedReview proof(
      BusinessProcessActivityDefinition activity, Product product, String sourceReference) {
    BusinessProcessDefinition target;
    if (quartzoPreparation != null
        && product.getProductTypeDefinition() != null
        && com.marketinghub.quartzo.commercial.v1.service.QuartzoCommercialContext.TYPE.equals(
            product.getProductTypeDefinition().getCode())) {
      if (!quartzoPreparation.completed(product, sourceReference))
        throw new IllegalStateException(
            "Conclua ou revalide a preparação Quartzo antes de reutilizar pareceres.");
      target = quartzoPreparation.target();
    } else {
      LearningSalesCycle cycle = cycle(product, sourceReference);
      if (!routing.isOpala(product.getId()))
        throw new IllegalStateException(
            "O tipo do produto ainda não possui verificador de reutilização de parecer.");
      target = routing.target(cycle);
      if (target == null)
        throw new IllegalStateException(
            "A preparação comercial do tipo não pertence a esta cadeia.");
      if (!routing.completed(cycle))
        throw new IllegalStateException(
            "Conclua ou revalide a preparação Opala da configuração atual antes de reutilizar pareceres.");
    }
    String sourceActivity = metadata(activity).path("reuseSubprocessActivityId").asText();
    var task =
        tasks
            .findLatestReviewSnapshots(
                target.getId(),
                sourceReference,
                sourceActivity,
                org.springframework.data.domain.PageRequest.of(0, 1))
            .stream()
            .findFirst()
            .filter(candidate -> "COMPLETED".equals(candidate.status()))
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "O parecer " + sourceActivity + " não está concluído no subprocesso."));
    BusinessProcessActivityDefinition ready =
        definitions
            .findByProcessDefinitionIdAndActivityId(target.getId(), "ready")
            .orElseThrow(
                () -> new IllegalStateException("O subprocesso não possui consolidação final."));
    BusinessProcessActivityInstance readyInstance =
        instances
            .findFirstByActivityDefinitionIdAndSourceReferenceOrderByOccurrenceNumberDesc(
                ready.getId(), sourceReference)
            .filter(value -> value.isObjectiveAchieved() && "COMPLETED".equals(value.getStatus()))
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "A consolidação vigente da preparação não foi encontrada."));
    return new ReusedReview(target, task, sourceActivity, readyInstance);
  }

  /** Confere a referência exata sem selecionar silenciosamente outro experimento ou produto. */
  private LearningSalesCycle cycle(Product product, String sourceReference) {
    if (sourceReference == null || !sourceReference.matches("experiment:[1-9][0-9]{0,17}"))
      throw new IllegalStateException("A revisão exige a referência exata do experimento.");
    long experimentId = Long.parseLong(sourceReference.substring("experiment:".length()));
    var cycle =
        cycles
            .findByExperimentId(experimentId)
            .orElseThrow(
                () -> new IllegalStateException("O experimento não possui ciclo comercial."));
    if (!Objects.equals(product.getId(), cycle.getProductId()))
      throw new IllegalStateException("O ciclo comercial pertence a outro produto.");
    return cycle;
  }

  /** Lê metadados versionados sem converter corrupção do contrato em ausência de requisito. */
  private JsonNode metadata(BusinessProcessActivityDefinition activity) {
    return read(activity == null ? null : activity.getDefinitionJson());
  }

  /** Lê uma evidência JSON e registra a causa quando o documento persistido é inválido. */
  private JsonNode read(String value) {
    try {
      return json.readTree(value == null ? "{}" : value);
    } catch (Exception ex) {
      log.error("Falha ao ler evidência reutilizável do Processo 5.", ex);
      throw new IllegalStateException("A evidência comercial reutilizável está inválida.", ex);
    }
  }

  /**
   * Calcula uma impressão estável da decisão e da consolidação que sustentam o reaproveitamento.
   */
  private String fingerprint(ReusedReview proof) {
    try {
      String source =
          proof.task().taskId()
              + "|"
              + Objects.toString(proof.task().evidenceJson(), "")
              + "|"
              + proof.readyInstance().getId()
              + "|"
              + Objects.toString(proof.readyInstance().getObjectiveEvidenceJson(), "");
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(source.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception ex) {
      log.error(
          "Falha ao calcular impressão da revisão comercial. taskId={}", proof.task().taskId(), ex);
      throw new IllegalStateException("Não foi possível identificar a evidência reutilizada.", ex);
    }
  }

  /** Monta um requisito operacional exibido pela tela sem transferir a regra ao frontend. */
  private ProductProcessActivityRequirementResponse requirement(
      String code, String title, boolean satisfied, String detail) {
    return new ProductProcessActivityRequirementResponse(
        code, title, satisfied, detail, satisfied ? "Preserve a configuração comprovada." : detail);
  }

  /** Devolve a conclusão funcional sem afirmar nova avaliação ou novo custo. */
  private BackendProductProcessActivityExecutionResult completed(String sourceReference) {
    return new BackendProductProcessActivityExecutionResult(
        sourceReference,
        "COMPLETED",
        true,
        "Parecer vigente referenciado no Processo 5 com custo incremental zero.");
  }

  /** Agrupa as três identidades imutáveis usadas para comprovar a reutilização. */
  private record ReusedReview(
      BusinessProcessDefinition process,
      AgentTaskReviewSnapshot task,
      String sourceActivity,
      BusinessProcessActivityInstance readyInstance) {}
}
