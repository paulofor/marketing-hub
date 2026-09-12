package com.marketinghub.businessprocesschain.learningcycle.v1.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.agenttask.BusinessProcessActivityInstance;
import com.marketinghub.businessprocess.BusinessProcessActivityDefinition;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.agenttask.BusinessProcessActivityInstanceRepository;
import com.marketinghub.repository.jpa.product.ProductRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: manter a mesma identidade entre produção e consumo das provas do ciclo. */
class LearningCycleEvidenceTest {
  private final ObjectMapper mapper = new ObjectMapper();
  private final BusinessProcessActivityInstanceRepository instances =
      mock(BusinessProcessActivityInstanceRepository.class);
  private final ProductRepository products = mock(ProductRepository.class);
  private final LearningCycleEvidence evidence =
      new LearningCycleEvidence(instances, null, new LearningCycleJson(mapper), null, products);
  private final Product product = Product.builder().id(4L).build();
  private final LearningSalesCycle cycle = new LearningSalesCycle();
  private BusinessProcessActivityInstance gate;
  private ObjectNode proof;

  /** Reproduz a identidade do gate 258 e a data posterior à correção v12 do ciclo 2. */
  @BeforeEach
  void setup() {
    cycle.setId(2L);
    cycle.setProductId(4L);
    cycle.setExperimentId(92L);
    cycle.setProductVersion("musa-pde-entry-v12-primeiro-ajuste-aplicavel");
    cycle.setVersionChangedAt(Instant.parse("2026-09-11T20:20:44Z"));
    when(products.findById(4L)).thenReturn(Optional.of(product));
    var process = new BusinessProcessDefinition();
    process.setId(70L);
    process.setProcessCode("pde-construction-approval");
    var activity = new BusinessProcessActivityDefinition();
    activity.setProcessDefinition(process);
    activity.setActivityId("agentValidationGate");
    gate = new BusinessProcessActivityInstance();
    gate.setId(258L);
    gate.setActivityDefinition(activity);
    gate.setSourceReference("experiment:92");
    gate.setStatus("COMPLETED");
    gate.setObjectiveAchieved(true);
    gate.setExitedAt(Instant.parse("2026-09-11T21:59:37Z"));
    proof =
        mapper
            .createObjectNode()
            .put("evidenceType", "PDE_AGENT_VALIDATION_GATE_V1")
            .put("productId", 4)
            .put("prototypeVersion", cycle.getProductVersion())
            .put("sourceReference", "experiment:92");
    when(instances.findById(258L)).thenReturn(Optional.of(gate));
    history("experiment:92", gate);
  }

  /**
   * Simula a ordenação já filtrada pelo repositório, sem unir fontes ou escolher apenas aprovações.
   */
  private void history(String source, BusinessProcessActivityInstance... values) {
    when(instances
            .findAllByActivityDefinitionProcessDefinitionProcessCodeAndSourceReferenceOrderByCreatedAtDescIdDesc(
                "pde-construction-approval", source))
        .thenReturn(List.of(values));
    gate.setObjectiveEvidenceJson(proof.toString());
  }

  /** Fornece somente registro técnico, sem observação humana ou autorização comercial. */
  private ObjectNode request() {
    gate.setObjectiveEvidenceJson(proof.toString());
    return mapper
        .createObjectNode()
        .put("approvalInstanceId", 258)
        .put("journeyEvidence", "internal://fixture/private-integration/270")
        .put("instrumentationVerified", true);
  }

  /** A prova exata do experimento é oferecida e consumida sem reexecutar os especialistas. */
  @Test
  void acceptsCurrentExperimentProofInSelectionAndCommand() {
    assertThat(evidence.approvals(cycle)).extracting(option -> option.id()).containsExactly(258L);
    evidence.validation(cycle, request());
    verify(instances, never())
        .findAllByActivityDefinitionProcessDefinitionProcessCodeAndSourceReferenceOrderByCreatedAtDescIdDesc(
            "pde-construction-approval", "product:4@agent-validation-v1");
    verify(instances, never()).save(any());
  }

  /** Referência global antiga não substitui um gate ausente do experimento comercial. */
  @Test
  void doesNotFallbackToLegacyApprovalForAnotherExecution() {
    history("experiment:92");
    history("product:4@agent-validation-v1", gate);
    assertThat(evidence.approvals(cycle)).isEmpty();
    assertThatThrownBy(() -> evidence.validation(cycle, request()))
        .isInstanceOf(ResponseStatusException.class);
  }

  /**
   * O contrato legado explícito continua aceitando sua fonte, inclusive sem campo novo na prova.
   */
  @Test
  void preservesLegacySourceOnlyForItsCanonicalProductContract() {
    product.setValidationDefinitionVersion("PDE_AGENT_VALIDATED_V1");
    gate.setSourceReference("product:4@agent-validation-v1");
    proof.remove("sourceReference");
    history(gate.getSourceReference(), gate);
    assertThat(evidence.approvals(cycle)).hasSize(1);
    evidence.validation(cycle, request());
  }

  /** A existência de uma ocorrência posterior bloqueada invalida a aprovação anterior. */
  @Test
  void laterRejectionCannotBeSkipped() {
    var rejected = new BusinessProcessActivityInstance();
    rejected.setId(259L);
    rejected.setActivityDefinition(gate.getActivityDefinition());
    rejected.setSourceReference(gate.getSourceReference());
    rejected.setStatus("BLOCKED");
    history("experiment:92", rejected, gate);
    assertThat(evidence.approvals(cycle)).isEmpty();
    assertThatThrownBy(() -> evidence.validation(cycle, request()))
        .hasMessageContaining("revisão mais recente");
  }

  /** Uma prova com fonte divergente não aparece na seleção nem passa pelo comando. */
  @Test
  void rejectsMismatchedEvidenceSource() {
    proof.put("sourceReference", "experiment:91");
    var input = request();
    assertThat(evidence.approvals(cycle)).isEmpty();
    assertThatThrownBy(() -> evidence.validation(cycle, input))
        .hasMessageContaining("produto e à versão exatos");
  }

  /** O sucessor não recebe a aprovação do experimento anterior. */
  @Test
  void rejectsAnotherExperiment() {
    cycle.setExperimentId(93L);
    assertThat(evidence.approvals(cycle)).isEmpty();
    assertThatThrownBy(() -> evidence.validation(cycle, request()))
        .isInstanceOf(ResponseStatusException.class);
  }

  /** Prova de outro produto não é homologação deste ciclo. */
  @Test
  void rejectsAnotherProduct() {
    proof.put("productId", 5);
    var input = request();
    assertThat(evidence.approvals(cycle)).isEmpty();
    assertThatThrownBy(() -> evidence.validation(cycle, input))
        .isInstanceOf(ResponseStatusException.class);
  }

  /** A versão aprovada deve coincidir integralmente com a versão do ciclo. */
  @Test
  void rejectsAnotherVersion() {
    proof.put("prototypeVersion", "v11");
    var input = request();
    assertThat(evidence.approvals(cycle)).isEmpty();
    assertThatThrownBy(() -> evidence.validation(cycle, input))
        .isInstanceOf(ResponseStatusException.class);
  }

  /** Uma alteração posterior exige nova homologação, mesmo quando o identificador coincide. */
  @Test
  void rejectsApprovalBeforeLastCorrection() {
    cycle.setVersionChangedAt(gate.getExitedAt().plusSeconds(1));
    assertThat(evidence.approvals(cycle)).isEmpty();
    assertThatThrownBy(() -> evidence.validation(cycle, request()))
        .hasMessageContaining("anterior ao ciclo");
  }

  /** A aprovação dos agentes não dispensa a segregação das métricas e a prova de instrumentação. */
  @Test
  void stillRequiresInstrumentationEvidence() {
    assertThatThrownBy(
            () -> evidence.validation(cycle, request().put("instrumentationVerified", false)))
        .hasMessageContaining("segregação dos testes");
  }
}
