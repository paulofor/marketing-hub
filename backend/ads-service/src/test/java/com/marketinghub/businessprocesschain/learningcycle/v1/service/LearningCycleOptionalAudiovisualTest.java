package com.marketinghub.businessprocesschain.learningcycle.v1.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.agenttask.*;
import com.marketinghub.businessprocess.*;
import com.marketinghub.businessprocesschain.*;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.repository.jpa.agenttask.*;
import com.marketinghub.repository.jpa.businessprocesschain.BusinessProcessChainDefinitionRepository;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

/**
 * Responsabilidade: reproduzir o bloqueio de formato e impedir dispensas sem prova da mesma versão.
 */
class LearningCycleOptionalAudiovisualTest {
  final BusinessProcessActivityInstanceRepository instances =
      mock(BusinessProcessActivityInstanceRepository.class);
  final AgentTaskRepository tasks = mock(AgentTaskRepository.class);
  final BusinessProcessChainDefinitionRepository chains =
      mock(BusinessProcessChainDefinitionRepository.class);
  final ObjectMapper mapper = new ObjectMapper();
  final LearningCycleOptionalAudiovisual resolver =
      new LearningCycleOptionalAudiovisual(instances, tasks, chains, new LearningCycleJson(mapper));
  final LearningSalesCycle cycle = new LearningSalesCycle();
  final List<BusinessProcessActivityInstance> history = new ArrayList<>();
  final Instant now = Instant.parse("2026-10-21T10:00:00Z");
  String source;
  ObjectNode decision;

  /** Monta o caso real com identificadores substituíveis, sem chamada de modelo ou publicação. */
  void arrange(long product, long experiment) {
    cycle.setId(13L);
    cycle.setProductId(product);
    cycle.setExperimentId(experiment);
    cycle.setChainDefinitionId(26L);
    cycle.setProductVersion("candidate-v14");
    cycle.setVersionChangedAt(now.minusSeconds(60));
    source = "experiment:" + experiment;
    var parent = new BusinessProcessDefinition();
    parent.setId(113L);
    parent.setProcessCode("pde-communication-sales-journey");
    var item = new BusinessProcessChainItem();
    item.setProcessDefinition(parent);
    var chain = new BusinessProcessChainDefinition();
    chain.setItems(List.of(item));
    when(chains.findById(26L)).thenReturn(Optional.of(chain));
    task("COMPLETED", "{\"functionalOutput\":{\"audiovisualBrief\":null}}");
    decision =
        mapper
            .createObjectNode()
            .put("evidenceType", "COMMUNICATION_FORMATS_V1")
            .put("communicationTaskId", 786)
            .put("sourceReference", source)
            .put("audiovisualRequired", false)
            .put("audiovisualPlanned", false)
            .put("audiovisualProductionDeferred", false);
    decision
        .putObject("privateCreativePreparation")
        .put("prototypeVersion", cycle.getProductVersion());
    history.clear();
    history.add(instance(719L, "audiovisual", "NOT_APPLICABLE", false));
    history.add(instance(718L, "route", "COMPLETED", true));
    persist();
    when(instances
            .findAllByActivityDefinitionProcessDefinitionProcessCodeAndSourceReferenceOrderByCreatedAtDescIdDesc(
                "creative-production-approval", source))
        .thenReturn(history);
  }

  /** Substitui a última tarefa para simular rejeição ou novo contrato de comunicação. */
  void task(String status, String result) {
    when(tasks.findFunctionalSnapshots(
            source, List.of("pde-communication-sales-journey"), cycle.getVersionChangedAt()))
        .thenReturn(
            List.of(
                new AgentTaskFunctionalSnapshot(
                    786L,
                    113L,
                    "pde-communication-sales-journey",
                    "communicationContract",
                    "communication-director",
                    status,
                    now,
                    now,
                    result)));
  }

  /** Cria a ocorrência do subprocesso responsável com autoria de Apolo preservada. */
  BusinessProcessActivityInstance instance(
      long id, String activity, String status, boolean achieved) {
    var process = new BusinessProcessDefinition();
    process.setId(121L);
    process.setProcessCode("creative-production-approval");
    var definition = new BusinessProcessActivityDefinition();
    definition.setActivityId(activity);
    definition.setProcessDefinition(process);
    definition.setOwnerName("Apolo");
    var value = new BusinessProcessActivityInstance();
    value.setId(id);
    value.setActivityDefinition(definition);
    value.setSourceReference(source);
    value.setStatus(status);
    value.setObjectiveAchieved(achieved);
    value.setExitedAt(now.plusSeconds(1));
    return value;
  }

  /** Serializa as provas dos dois registros sem alterar a tarefa autora. */
  void persist() {
    for (var value : history) {
      var data = decision.deepCopy();
      if ("audiovisual".equals(value.getActivityDefinition().getActivityId()))
        data.put("evidenceType", "OPTIONAL_ACTIVITY_NOT_REQUIRED_V1")
            .put("activityId", "audiovisual");
      value.setObjectiveEvidenceJson(data.toString());
    }
  }

  /** Mesmo comportamento para Vega e outra identidade, sem exceção por nome ou identificador. */
  @ParameterizedTest
  @CsvSource({"4,106", "51004,51106"})
  void recognizesCurrentExplicitStaticRoute(long product, long experiment) {
    arrange(product, experiment);
    var proof = resolver.proof(cycle).orElseThrow();
    assertThat(proof.path("productId").asLong()).isEqualTo(product);
    assertThat(proof.path("experimentId").asLong()).isEqualTo(experiment);
    assertThat(proof.path("omissionInstanceId").asLong()).isEqualTo(719);
    assertThat(proof.path("publicationAuthorized").asBoolean()).isFalse();
    assertThat(proof.path("spendAuthorized").asBoolean()).isFalse();
  }

  /** Vídeos planejados ou adiados continuam exigindo seu percurso e orçamento próprios. */
  @ParameterizedTest
  @ValueSource(
      strings = {"audiovisualRequired", "audiovisualPlanned", "audiovisualProductionDeferred"})
  void neverSkipsPlannedOrDeferredVideo(String flag) {
    arrange(4, 106);
    decision.put(flag, true);
    persist();
    assertThat(resolver.proof(cycle)).isEmpty();
  }

  /** Dados ausentes e tipos incorretos não se transformam em uma decisão de dispensa. */
  @ParameterizedTest
  @ValueSource(
      strings = {"audiovisualRequired", "audiovisualPlanned", "audiovisualProductionDeferred"})
  void missingOrCoercedFlagsAreNotOmission(String flag) {
    arrange(4, 106);
    decision.remove(flag);
    persist();
    assertThat(resolver.proof(cycle)).isEmpty();
    decision.put(flag, "false");
    persist();
    assertThat(resolver.proof(cycle)).isEmpty();
  }

  /** Não aceita fonte, versão ou tarefa de outro contexto na evidência. */
  @ParameterizedTest
  @ValueSource(strings = {"source", "version", "task"})
  void rejectsCrossContextProof(String kind) {
    arrange(4, 106);
    if (kind.equals("source")) decision.put("sourceReference", "experiment:999");
    if (kind.equals("version"))
      ((ObjectNode) decision.path("privateCreativePreparation")).put("prototypeVersion", "older");
    if (kind.equals("task")) decision.put("communicationTaskId", 999);
    persist();
    assertThat(resolver.proof(cycle)).isEmpty();
  }

  /** Nova decisão de Íris com vídeo ou falha invalida a dispensa anterior. */
  @Test
  void newerContractInvalidatesPreviousOmission() {
    arrange(4, 106);
    task("COMPLETED", "{\"functionalOutput\":{\"audiovisualBrief\":\"Vídeo necessário\"}}");
    assertThat(resolver.proof(cycle)).isEmpty();
    task("BLOCKED", "{\"functionalOutput\":{\"audiovisualBrief\":null}}");
    assertThat(resolver.proof(cycle)).isEmpty();
  }

  /** Ausência do campo no resultado de Íris não comprova que o formato foi avaliado. */
  @Test
  void absentBriefIsNotAnExplicitDecision() {
    arrange(4, 106);
    task("COMPLETED", "{\"functionalOutput\":{}}");
    assertThat(resolver.proof(cycle)).isEmpty();
  }

  /** Rejeição posterior prevalece sobre uma dispensa antes válida. */
  @Test
  void latestOccurrenceRevokesOmission() {
    arrange(4, 106);
    history.addFirst(instance(800L, "audiovisual", "BLOCKED", false));
    assertThat(resolver.proof(cycle)).isEmpty();
  }

  /** Prova anterior à tarefa e processo estranho à cadeia não podem justificar a dispensa. */
  @Test
  void rejectsOldProofAndUnrelatedChain() {
    arrange(4, 106);
    history.getFirst().setExitedAt(now.minusSeconds(1));
    assertThat(resolver.proof(cycle)).isEmpty();
    arrange(4, 106);
    var chain = new BusinessProcessChainDefinition();
    chain.setItems(List.of());
    when(chains.findById(26L)).thenReturn(Optional.of(chain));
    assertThat(resolver.proof(cycle)).isEmpty();
  }
}
