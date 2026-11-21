package com.marketinghub.product.valuedelivery.v1.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.agenttask.*;
import com.marketinghub.businessprocess.*;
import com.marketinghub.product.Product;
import com.marketinghub.product.service.agentvalidation.PdeOperationalControlEvidence;
import com.marketinghub.producttype.ProductTypeDefinition;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.product.ProductRepository;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Exercita a passagem real do contrato e a prevenção de redução silenciosa em identidades
 * distintas.
 */
class ProductValueDeliveryServiceTest {
  private final ObjectMapper json = new ObjectMapper();
  private final ProductRepository products = mock(ProductRepository.class);
  private final AgentTaskRepository tasks = mock(AgentTaskRepository.class);
  private final PdeOperationalControlEvidence evidence = mock(PdeOperationalControlEvidence.class);
  private final ProductValueDeliveryService service =
      new ProductValueDeliveryService(products, tasks, evidence, json);
  private final List<AgentTask> history = new ArrayList<>();

  /** Simula a passagem completa e comprova que editar catálogo não muda a base da execução. */
  @ParameterizedTest
  @ValueSource(longs = {4, 903})
  void preservesProposalThroughConstructionAndIndependentReviews(long id) throws Exception {
    var journey = setup(id);
    JsonNode baseline =
        json.readTree(journey.getProductBigIdeaJson()).path("valueDeliveryBaseline");
    ObjectNode spec = specification(baseline);
    var response = json.createObjectNode().set("valueSpecification", spec);
    service.apply(journey, request(response));
    journey.setResultJson(response.toString());
    journey.setStatus("COMPLETED");
    history.add(journey);
    var second = task(journey.getProcessDefinition(), id, "deliverables");
    var newIdea = json.createObjectNode();
    service.pin(second, newIdea, target(id));
    assertThat(newIdea.path("valueDeliveryBaseline")).isEqualTo(baseline);
    second.setProductBigIdeaJson(newIdea.toString());
    service.apply(
        second,
        request(
            json.createObjectNode()
                .putArray("acceptanceCriteria")
                .add("VALUE_SPECIFICATION:" + ProductValueContract.hash(spec))));
    var technical = task(journey.getProcessDefinition(), id, "technicalHomologation");
    technical.setStatus("COMPLETED");
    technical.setResultJson(
        json.createObjectNode()
            .put("productId", id)
            .put("sourceReference", "experiment:" + id)
            .put("productSlug", "fixture-" + id)
            .put("prototypeVersion", "v1")
            .toString());
    history.add(technical);
    var report = report(baseline, spec);
    when(evidence.resolve("fixture-" + id, "v1")).thenReturn(Optional.of(report));
    service.verify(journey.getProcessDefinition(), journey.getSourceReference(), false);
    assertThatThrownBy(
            () ->
                service.verify(journey.getProcessDefinition(), journey.getSourceReference(), true))
        .hasMessageContaining("revisão independente");
    for (String activity : ProductValueDeliveryService.REVIEWS) {
      var review = task(journey.getProcessDefinition(), id, activity);
      review.setProductBigIdeaJson(newIdea.toString());
      var result = json.createObjectNode();
      result
          .putArray("evidence")
          .add(
              "VALUE_DELIVERY:"
                  + ProductValueContract.hash(spec)
                  + ":"
                  + report.path("reportSha256").asText());
      service.apply(review, request(result));
      review.setResultJson(result.toString());
      review.setStatus("COMPLETED");
      history.add(review);
    }
    service.verify(journey.getProcessDefinition(), journey.getSourceReference(), true);
    report.put("reportSha256", "b".repeat(64));
    assertThatThrownBy(
            () ->
                service.verify(journey.getProcessDefinition(), journey.getSourceReference(), true))
        .hasMessageContaining("revisão independente");
    verify(products, times(1)).findById(id);
  }

  /** Recusa omissão, fonte inventada, recorte promovido e critérios genéricos sem execução. */
  @Test
  void rejectsSilentReductionAndUnrelatedProof() throws Exception {
    var task = setup(4);
    var baseline = json.readTree(task.getProductBigIdeaJson()).path("valueDeliveryBaseline");
    var spec = specification(baseline);
    var story = (ObjectNode) spec.path("stories").get(0);
    var sources = story.path("sourceRefs").deepCopy();
    story.putArray("sourceRefs").add("bigIdea.promise");
    assertThatThrownBy(() -> ProductValueContract.specification(baseline, spec))
        .hasMessageContaining("sem história");
    story.putArray("sourceRefs").add("inventada");
    assertThatThrownBy(() -> ProductValueContract.specification(baseline, spec))
        .hasMessageContaining("desconhecida");
    story.set("sourceRefs", sources);
    story
        .put("action", "DEFER")
        .put("deferReason", "Consultor e repertório visual ainda não construídos");
    assertThatThrownBy(() -> ProductValueContract.specification(baseline, spec))
        .hasMessageContaining("parcial");
    spec.put("scope", "PROTOTYPE_SLICE");
    ProductValueContract.specification(baseline, spec);
    assertThatThrownBy(
            () -> ProductValueContract.evidence(baseline, spec, report(baseline, spec), "v1"))
        .hasMessageContaining("recorte");
    story.put("action", "BUILD");
    spec.put("scope", "PRODUCT");
    var report = report(baseline, spec);
    ((ObjectNode) report.path("criteria").get(0)).put("testMethod", "genericHealthcheck");
    assertThatThrownBy(() -> ProductValueContract.evidence(baseline, spec, report, "v1"))
        .hasMessageContaining("execução comprovada");
  }

  /** Não aceita produto, ciclo, versão, história ou referência de especificação de outra origem. */
  @Test
  void isolatesContextAndBlocksStaleEvidence() throws Exception {
    var task = setup(903);
    var baseline = json.readTree(task.getProductBigIdeaJson()).path("valueDeliveryBaseline");
    var spec = specification(baseline);
    for (String field : List.of("productId", "sourceReference", "specificationSha256")) {
      var report = report(baseline, spec);
      ((ObjectNode) report.path("valueDelivery")).put(field, "foreign");
      assertThatThrownBy(() -> ProductValueContract.evidence(baseline, spec, report, "v1"))
          .isInstanceOf(IllegalArgumentException.class);
    }
    assertThatThrownBy(
            () -> ProductValueContract.evidence(baseline, spec, report(baseline, spec), "v2"))
        .hasMessageContaining("versão");
    var missing = report(baseline, spec);
    ((ObjectNode) missing.path("valueDelivery")).putArray("criteria");
    assertThatThrownBy(() -> ProductValueContract.evidence(baseline, spec, missing, "v1"))
        .hasMessageContaining("critérios");
    spec.put("baselineSha256", "a".repeat(64));
    assertThatThrownBy(() -> ProductValueContract.specification(baseline, spec))
        .hasMessageContaining("outra proposta");
  }

  /** Preserva processos antigos e outros tipos sem inventar novos requisitos retroativos. */
  @Test
  void preservesLegacyAndOtherTypes() {
    var task = setup(702);
    task.getProcessDefinition().setDiagramJson("{}");
    assertThat(service.supports(task)).isFalse();
    service.verify(task.getProcessDefinition(), task.getSourceReference(), true);
    task.getProcessDefinition()
        .setDiagramJson("{\"productValueDeliveryContract\":\"TURMALINA_VALUE_DELIVERY_V1\"}");
    products
        .findById(702L)
        .orElseThrow()
        .getProductTypeDefinition()
        .setCode("LOW_TICKET_DIGITAL_PRODUCT");
    var fixed = json.createObjectNode();
    service.pin(task, fixed, target(702));
    task.setProductBigIdeaJson(fixed.toString());
    history.add(task);
    assertThat(service.supports(task)).isFalse();
    service.verify(task.getProcessDefinition(), task.getSourceReference(), true);
  }

  /** Mantém a primeira lacuna visível sem consumir revisão e sem ressuscitar tentativa anterior. */
  @Test
  void explainsMissingImplementationBeforePaidReview() throws Exception {
    var task = setup(4);
    history.add(task);
    var activity = new BusinessProcessActivityDefinition();
    activity.setActivityId("psiqueAdherent");
    var readiness = new ProductValueReviewReadiness(service);
    assertThat(readiness.supports(task.getProcessDefinition(), activity)).isTrue();
    assertThat(
            readiness
                .readiness(task.getProcessDefinition(), activity, null, task.getSourceReference())
                .ready())
        .isFalse();
    task.setStatus("COMPLETED");
    task.setResultJson(
        json.createObjectNode()
            .set(
                "valueSpecification",
                specification(
                    json.readTree(task.getProductBigIdeaJson()).path("valueDeliveryBaseline")))
            .toString());
    assertThat(
            readiness
                .readiness(task.getProcessDefinition(), activity, null, task.getSourceReference())
                .reason())
        .contains("homologação técnica");
    var blocked = task(task.getProcessDefinition(), 4, "journey");
    blocked.setStatus("BLOCKED");
    blocked.setProductBigIdeaJson(task.getProductBigIdeaJson());
    history.add(blocked);
    assertThat(
            readiness
                .readiness(task.getProcessDefinition(), activity, null, task.getSourceReference())
                .reason())
        .contains("especificação atual");
    verifyNoInteractions(evidence);
  }

  /** Preserva o único efeito de aceitação de runtime, sem disputar seu callback de acesso. */
  @Test
  void preservesRuntimeAcceptanceOwner() {
    var task = setup(903);
    task.setProcessActivityId("access");
    task.setSourceReference("product:903@agent-validation-v1");
    task.setAssignedAgent(
        com.marketinghub.agent.Agent.builder().agentKey("landing-generator").build());
    var runtime =
        new com.marketinghub.product.service.agentvalidation
            .PdeAgentValidationRuntimeAcceptanceHook(
            products,
            json,
            mock(
                com.marketinghub.product.service.agentvalidation.PdeAgentValidationRuntimeProbe
                    .class));
    assertThat(runtime.supports(task)).isTrue();
    assertThat(service.supports(task)).isFalse();
    task.setProcessActivityId("journey");
    assertThat(runtime.supports(task)).isFalse();
    assertThat(service.supports(task)).isTrue();
  }

  /**
   * Reutiliza o relatório real de MUSA como prova parcial, nunca como cobertura do novo contrato.
   */
  @Test
  void preservesActualMusaEvidenceWithoutPromotingItToFullProduct() throws Exception {
    var catalog =
        new PdeOperationalControlEvidence(
            json, new org.springframework.core.io.DefaultResourceLoader());
    JsonNode historical =
        catalog.resolve("metodo-musa-7-dias", "musa-consultor-pwa-v1").orElseThrow();
    assertThat(historical.path("status").asText()).isEqualTo("PASS");
    assertThat(historical.path("providerCalls").asInt()).isZero();
    var task = setup(4);
    var baseline = json.readTree(task.getProductBigIdeaJson()).path("valueDeliveryBaseline");
    assertThatThrownBy(
            () ->
                ProductValueContract.evidence(
                    baseline, specification(baseline), historical, "musa-consultor-pwa-v1"))
        .hasMessageContaining("Falta prova da implementação desta especificação");
    assertThat(catalog.resolve("metodo-musa-7-dias", "musa-consultor-pwa-v1")).contains(historical);
  }

  /**
   * Confere serialização equivalente ao emissor Python, imune à ordem de campos e sem esconder
   * adulteração.
   */
  @Test
  void hashesCanonicalJsonAndRejectsTamperedExecution() throws Exception {
    var input =
        json.readTree(
            """
        {"z":[{"id":4,"ação":"retomar"}],"status":"PASS"}
        """);
    assertThat(ProductValueContract.hash(input))
        .isEqualTo("8a34d3885cf8ba8fa8b508df3f894fe746b1af7be142ceda221b18e8616464ec");
    var task = setup(903);
    var baseline = json.readTree(task.getProductBigIdeaJson()).path("valueDeliveryBaseline");
    var spec = specification(baseline);
    var report = report(baseline, spec);
    ((ObjectNode) report.path("criteria").get(0).path("result")).put("restoredChoice", false);
    assertThatThrownBy(() -> ProductValueContract.evidence(baseline, spec, report, "v1"))
        .hasMessageContaining("execução comprovada");
  }

  /** Prepara fontes sintéticas sem ler produção nem chamar agentes. */
  private AgentTask setup(long id) {
    var process = new BusinessProcessDefinition();
    process.setId(100L + id);
    process.setProcessCode(ProductValueDeliveryService.PROCESS);
    process.setVersionNumber(12);
    process.setDiagramJson("{\"productValueDeliveryContract\":\"TURMALINA_VALUE_DELIVERY_V1\"}");
    var product = new Product();
    product.setId(id);
    product.setProductTypeDefinition(
        ProductTypeDefinition.builder()
            .code("AI_PWA_CONSULTANT_PRODUCT")
            .blueprintVersion("consultant-pwa-v2")
            .customerJob("Reutilizar escolhas próprias")
            .valueMechanism("Consultor contextual")
            .experienceFlow("Valor, salvar, retomar")
            .requiredInputs("Contexto autorizado")
            .expectedOutputs("Orientação visual")
            .memoryStrategy("Memória segregada")
            .integrationRequirements("SDK consultor")
            .safetyGuardrails("Controle e exclusão")
            .successMetrics("Uso e retorno")
            .build());
    when(products.findById(id)).thenReturn(Optional.of(product));
    when(tasks.findByProcessDefinitionIdAndSourceReferenceOrderByCreatedAtAscIdAsc(
            process.getId(), "experiment:" + id))
        .thenAnswer(call -> List.copyOf(history));
    var task = task(process, id, "journey");
    var idea = json.createObjectNode();
    var revision = idea.putObject("revision");
    revision.put("id", id);
    revision
        .putObject("content")
        .put("promise", "Escolher, salvar e adaptar")
        .put("mechanism", "Consultor visual contínuo")
        .put("limits", "Sem compra ou aprovação social garantida");
    service.pin(task, idea, target(id));
    task.setProductBigIdeaJson(idea.toString());
    return task;
  }

  /** Cria uma tarefa com identidade explícita e estado não concluído. */
  private AgentTask task(BusinessProcessDefinition process, long id, String activity) {
    var task = new AgentTask();
    task.setId(id + history.size());
    task.setProcessDefinition(process);
    task.setSourceReference("experiment:" + id);
    task.setProcessActivityId(activity);
    task.setStatus("PENDING");
    return task;
  }

  /** Produz alvo próprio, sem supor que número ou nome define comportamento. */
  private AgentTaskTargetResponse target(long id) {
    return new AgentTaskTargetResponse(
        "experiment:" + id,
        id,
        id,
        "fixture-" + id,
        "Teste",
        "Teste",
        "v1",
        null,
        null,
        null,
        null,
        null);
  }

  /** Monta histórias concretas com cobertura das fontes e critério verificável. */
  private ObjectNode specification(JsonNode baseline) {
    var spec =
        json.createObjectNode()
            .put("contractVersion", ProductValueContract.VERSION)
            .put("baselineSha256", ProductValueContract.hash(baseline))
            .put("scope", "PRODUCT");
    var story =
        spec.putArray("stories")
            .addObject()
            .put("id", "resume")
            .put("customerOutcome", "Retomar escolha e adaptar à nova ocasião")
            .put("action", "BUILD")
            .put("implementationNotes", "Usar backend persistente e consultor SDK");
    var refs = story.putArray("sourceRefs");
    baseline.path("requirements").fieldNames().forEachRemaining(refs::add);
    var states = story.putObject("screenStates");
    for (String state : List.of("entry", "result", "resume", "empty", "error"))
      states.put(state, "Tela " + state + " com ação de continuar ou corrigir");
    story
        .putArray("criteria")
        .addObject()
        .put("id", "restore")
        .put("given", "Escolha salva e sessão fechada")
        .put("when", "Retomar em outra ocasião")
        .put("then", "Preservar original e criar adaptação no mesmo proprietário")
        .put("testMethod", "resumeAcrossOccasions");
    return spec;
  }

  /** Simula recibo do catálogo operacional, separado de aprovação e prova comercial. */
  private ObjectNode report(JsonNode baseline, JsonNode spec) {
    var report =
        json.createObjectNode()
            .put("prototypeVersion", "v1")
            .put("reportSha256", "a".repeat(64))
            .put("frontendSourceFingerprint", "c".repeat(64));
    var trace =
        report
            .putObject("valueDelivery")
            .put("specificationSha256", ProductValueContract.hash(spec));
    trace.set("productId", baseline.path("productId"));
    trace.set("sourceReference", baseline.path("sourceReference"));
    trace
        .putArray("criteria")
        .addObject()
        .put("storyId", "resume")
        .put("criterionId", "restore")
        .put("proofCode", "RESUME");
    report
        .putArray("criteria")
        .addObject()
        .put("code", "RESUME")
        .put("status", "PASS")
        .put("verificationType", "EXECUTION")
        .put("testMethod", "resumeAcrossOccasions")
        .put("resultSha256", "d".repeat(64));
    var proof = (ObjectNode) report.path("criteria").get(0);
    var observed =
        proof
            .putObject("result")
            .put("status", "PASS")
            .put("restoredChoice", true)
            .put("originalPreserved", true);
    proof.put("resultSha256", ProductValueContract.hash(observed));
    return report;
  }

  /** Monta o mesmo DTO do callback preservando resultado estruturado. */
  private CompleteAgentTaskRequest request(JsonNode result) {
    return json.convertValue(
        Map.of("resultJson", result.toString()), CompleteAgentTaskRequest.class);
  }
}
