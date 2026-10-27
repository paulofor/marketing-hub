package com.marketinghub.pde.kit.privateprototype.v1.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agenttask.AgentTaskFunctionalSnapshot;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycleEvent;
import com.marketinghub.pde.kit.privateprototype.v1.service.bindImplementation.Bind;
import com.marketinghub.pde.kit.privateprototype.v1.service.implementationPlan.Plan;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleEventRepository;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Responsabilidade: vincular uma capacidade instalada ao desenho aprovado sem fabricar homologação.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class KitImplementationBinding {
  private static final String ACTION = "BIND_KIT_IMPLEMENTATION";
  private final LearningSalesCycleRepository cycles;
  private final LearningSalesCycleEventRepository events;
  private final AgentTaskRepository tasks;
  private final ObjectMapper json;

  /** Apresenta o desenho vigente e as capacidades disponíveis sem criar tarefa ou autorização. */
  @Transactional(readOnly = true)
  public Plan plan(Long productId, Long cycleId) {
    var cycle = cycles.findById(cycleId).orElseThrow();
    require(productId.equals(cycle.getProductId()), "O ciclo não pertence ao produto.");
    var architecture = architecture(cycle);
    return new Plan(
        cycle.getRevision(),
        architecture.id(),
        hash(architecture),
        resolved(cycle, architecture).orElse(null),
        List.of(KitSpecialties.BEAUTY, KitSpecialties.BEAUTY_REFERENCE));
  }

  /** Registra seleção explícita e idempotente; mudança do desenho invalida o vínculo antigo. */
  @Transactional
  public Plan bind(Bind input) {
    var cycle = cycles.findLocked(input.productId(), input.cycleId()).orElseThrow();
    var architecture = architecture(cycle);
    require(KitSpecialties.isFamily(input.profileCode()), "Capacidade não suportada.");
    require(
        Objects.equals(architecture.id(), input.architectureTaskId())
            && hash(architecture).equals(input.architectureSha256()),
        "O desenho mudou. Confira a versão atual.");
    var request = json.valueToTree(input);
    var previous = events.findByCycleIdAndRequestKey(cycle.getId(), input.requestKey().toString());
    if (previous.isPresent()) {
      require(
          ACTION.equals(previous.get().getAction())
              && previous.get().getRequestJson().equals(request.toString()),
          "A chave já foi usada com outra seleção.");
      return plan(input.productId(), input.cycleId());
    }
    var definition = read(architecture.resultJson()).path("productArchitecture");
    require(
        definition.path("kitProfileCode").asText("").isBlank()
            && KitSpecialties.declaredProfile(definition).isEmpty(),
        "O desenho já declara uma capacidade. Corrija o próprio contrato antes de trocar o"
            + " perfil.");
    require(cycle.getRevision() == input.expectedRevision(), "A revisão mudou. Atualize o ciclo.");
    require(resolved(cycle, architecture).isEmpty(), "Esta versão já possui capacidade vinculada.");
    var proof =
        json.createObjectNode()
            .put("profileCode", input.profileCode())
            .put("productId", cycle.getProductId())
            .put("experimentId", cycle.getExperimentId())
            .put("productVersion", cycle.getProductVersion())
            .put("architectureTaskId", architecture.id())
            .put("architectureSha256", hash(architecture))
            .put("implementationValidated", false)
            .put("commercialSpendAuthorized", false);
    cycle.setRevision(cycle.getRevision() + 1);
    cycle.setUpdatedAt(Instant.now());
    cycles.saveAndFlush(cycle);
    var event = new LearningSalesCycleEvent();
    event.setCycleId(cycle.getId());
    event.setRequestKey(input.requestKey().toString());
    event.setRequestJson(request.toString());
    event.setRevision(cycle.getRevision());
    event.setFromStage(cycle.getStage());
    event.setToStage(cycle.getStage());
    event.setAction(ACTION);
    event.setOperatorName(input.operatorName());
    event.setSummary(
        "Capacidade de composição por especialidade vinculada. Pacotes e homologação ainda"
            + " pendentes.");
    event.setEvidenceReference("internal://tasks/" + architecture.id());
    event.setEvidenceJson(proof.toString());
    event.setCreatedAt(cycle.getUpdatedAt());
    events.saveAndFlush(event);
    log.info(
        "Kit: capacidade vinculada productId={} cycleId={} architectureTaskId={} profileCode={}",
        cycle.getProductId(),
        cycle.getId(),
        architecture.id(),
        input.profileCode());
    return plan(input.productId(), input.cycleId());
  }

  /**
   * Usa somente o recibo da versão e do desenho exatos; nunca extrai especialidade de texto livre.
   */
  public Optional<String> resolved(
      LearningSalesCycle cycle, AgentTaskFunctionalSnapshot architecture) {
    if (!"COMPLETED".equals(architecture.status())
        || !"APPROVE".equals(read(architecture.resultJson()).path("decision").asText()))
      return Optional.empty();
    return events.findByCycleIdAndActionOrderByRevisionDesc(cycle.getId(), ACTION).stream()
        .findFirst()
        .map(e -> read(e.getEvidenceJson()))
        .filter(
            p ->
                p.path("productId").asLong() == cycle.getProductId()
                    && p.path("experimentId").asLong() == cycle.getExperimentId()
                    && cycle.getProductVersion().equals(p.path("productVersion").asText())
                    && p.path("architectureTaskId").asLong() == architecture.id()
                    && hash(architecture).equals(p.path("architectureSha256").asText())
                    && KitSpecialties.isFamily(p.path("profileCode").asText()))
        .map(p -> p.path("profileCode").asText());
  }

  /** Identifica o desenho associado à prova sem promover o vínculo a teste aprovado. */
  public com.fasterxml.jackson.databind.node.ObjectNode registrationEvidence(
      LearningSalesCycle cycle) {
    var task = latestArchitecture(cycle);
    require(compatible(cycle, task), "A capacidade precisa estar vinculada ao desenho atual.");
    return json.createObjectNode()
        .put("implementationArchitectureTaskId", task.id())
        .put("implementationArchitectureSha256", hash(task));
  }

  /** Impede que a aceitação antiga sobreviva a uma arquitetura posterior da mesma versão. */
  public boolean currentProof(
      LearningSalesCycle cycle, com.fasterxml.jackson.databind.JsonNode proof) {
    var task = latestArchitecture(cycle);
    return compatible(cycle, task)
        && proof.path("implementationArchitectureTaskId").asLong() == task.id()
        && hash(task).equals(proof.path("implementationArchitectureSha256").asText());
  }

  /**
   * Reutiliza declaração explícita do desenho; recibo separado só resolve a ausência desse código.
   */
  private boolean compatible(LearningSalesCycle cycle, AgentTaskFunctionalSnapshot task) {
    return "COMPLETED".equals(task.status())
        && "APPROVE".equals(read(task.resultJson()).path("decision").asText())
        && (resolved(cycle, task).isPresent()
            || KitSpecialties.declaredProfile(read(task.resultJson()).path("productArchitecture"))
                .filter(KitSpecialties::isFamily)
                .isPresent());
  }

  /** Lê a última tentativa do mesmo ciclo, incluindo bloqueios posteriores. */
  private AgentTaskFunctionalSnapshot latestArchitecture(LearningSalesCycle cycle) {
    return tasks
        .findFunctionalSnapshots(
            "experiment:" + cycle.getExperimentId(),
            Set.of("pde-commercial-plan-offer"),
            cycle.getCreatedAt())
        .stream()
        .filter(t -> "productArchitecture".equals(t.processActivityId()))
        .max(Comparator.comparing(AgentTaskFunctionalSnapshot::id))
        .orElseThrow();
  }

  /**
   * Exige a última arquitetura aprovada deste ciclo; especificação não se torna prova de execução.
   */
  private AgentTaskFunctionalSnapshot architecture(LearningSalesCycle cycle) {
    require(
        "OPEN".equals(cycle.getStatus())
            && Set.of("ADJUSTMENT", "VALIDATION").contains(cycle.getStage()),
        "A vinculação exige ciclo aberto em construção ou validação.");
    var task = latestArchitecture(cycle);
    require(
        "COMPLETED".equals(task.status())
            && "APPROVE".equals(read(task.resultJson()).path("decision").asText()),
        "Conclua o desenho aprovado antes de selecionar sua implementação.");
    require(
        KitSpecialties.privateAdapterSupported(read(task.resultJson()).path("productArchitecture")),
        "O desenho não autoriza o adaptador determinístico privado sem efeitos externos.");
    return task;
  }

  /** Calcula a identidade do contrato integral preservado, sem reescrever a saída de Dédalo. */
  private String hash(AgentTaskFunctionalSnapshot task) {
    return KitArtifactContract.hash(task.resultJson().getBytes(StandardCharsets.UTF_8));
  }

  /** Preserva diagnóstico completo sem retornar payload sensível na interface. */
  private com.fasterxml.jackson.databind.JsonNode read(String value) {
    try {
      return json.readTree(value);
    } catch (Exception ex) {
      log.error("Kit: recibo de implementação inválido", ex);
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "O recibo de implementação não pôde ser conferido.", ex);
    }
  }

  /** Recusa alteração incompatível sem concluir atividade ou iniciar inferência. */
  private void require(boolean condition, String reason) {
    if (!condition) throw new ResponseStatusException(HttpStatus.CONFLICT, reason);
  }
}
